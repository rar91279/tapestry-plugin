import com.github.javaparser.printer.concretesyntaxmodel.CsmElement.token
import jdk.jfr.internal.JVM.exclude
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

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
    // MavenUtils (production) uses Apache maven-model (MavenXpp3Writer) to write pom.xml. In 2026.2
    // this jar no longer sits on the core platform classpath the fast `kotest` task builds, so declare
    // the library explicitly for tests. Version matches the 3.x the IDE bundles.
    testImplementation("org.apache.maven:maven-model:3.8.1")

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
    classpath = testSourceSet.runtimeClasspath + platformRuntime

    include("com/intellij/tapestry/core/**")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    include("com/intellij/tapestry/tests/**")
}
