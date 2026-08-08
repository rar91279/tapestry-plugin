import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.changelog.Changelog

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.changelog")
    id("org.jetbrains.qodana")
    id("org.jetbrains.intellij.platform")
}

kotlin {
    jvmToolchain(25)
}

sourceSets {
    main {
        java.srcDirs("src/main/gen")
    }
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
intellijPlatform {
    pluginConfiguration {
        version = project.version.toString()

        // Pinned rather than inherited from whatever platform the build happens to target, so raising the
        // compatibility floor is a deliberate decision and not a side effect of a platform bump.
        ideaVersion {
            sinceBuild = "262"
        }

        // The plugin's "What's new" comes from CHANGELOG.md: the section of the version being built,
        // falling back to [Unreleased] for snapshot builds.
        changeNotes = providers.provider {
            with(changelog) {
                renderItem(
                    (getOrNull(project.version.toString()) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML
                )
            }
        }
    }

    // `verifyPlugin` (run by CI) otherwise checks whatever IPGP defaults to. Declare the target set so the
    // verified IDEs are an intentional matrix.
    pluginVerification {
        ides {
            recommended()
        }
    }

    signing {
        certificateChain = System.getenv("CERTIFICATE_CHAIN")
        privateKey = System.getenv("PRIVATE_KEY")
        password = System.getenv("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = System.getenv("PUBLISH_TOKEN")
    }
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.hamcrest)
    testImplementation(libs.xmlunit.matchers)
    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.mockk)
    testRuntimeOnly(libs.junit.vintage.engine)

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        intellijIdea("2026.2")
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)

        // Add plugin dependencies for compilation here:
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")
        bundledPlugin("com.intellij.properties")
        bundledPlugin("com.intellij.javaee")
        bundledPlugin("com.intellij.css")
        bundledModule("intellij.relaxng")
        bundledModule("intellij.platform.ui.jcef")
    }
}

// kotest/mockk pull in the upstream kotlinx-coroutines, but the IntelliJ platform
// ships a patched fork (extra methods like runBlockingWithParallelismCompensation)
// that its test fixtures require. Drop the upstream copy so the platform's wins.
configurations.testImplementation {
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
}

// Two test tasks:
//  * `kotest`     — fast, pure/mock unit specs under `core/**`. No IDE bootstrap, so
//                   its classpath is hand-built and it starts instantly.
//  * `test`       — the plugin-configured platform task (full IDE env + test-framework
//                   classpath). Runs the `tests/**` integration specs. Switched to the
//                   JUnit Platform so it runs kotest specs; the junit-vintage engine
//                   keeps the legacy JUnit `UsefulTestCase` integration tests running.
// Byte Buddy (pulled in by mockk) only "officially" supports up to Java 24; the JDK 25 toolchain
// required by the 2026.2 platform emits class file v69, which BB refuses unless run experimentally.
tasks.withType<Test>().configureEach {
    systemProperty("net.bytebuddy.experimental", "true")
}

val testSourceSet = sourceSets["test"]
val platformRuntime = configurations["intellijPlatformClasspath"]
tasks.register<Test>("kotest") {
    useJUnitPlatform()
    testClassesDirs = testSourceSet.output.classesDirs
    // compileClasspath as well as runtimeClasspath: the bundled-plugin jars (java PSI, in particular)
    // are only on the compile side, and the specs mock those PSI interfaces.
    classpath = testSourceSet.runtimeClasspath + testSourceSet.compileClasspath + platformRuntime

    include("com/github/rar91279/plugin/tapestry/core/**")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    include("com/github/rar91279/plugin/tapestry/tests/**")
}

// Run the core unit specs as part of the standard verification lifecycle (check/build).
tasks.named("check") {
    dependsOn("kotest")
}
