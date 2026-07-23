import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.changelog")
    id("org.jetbrains.qodana")
    id("org.jetbrains.intellij.platform")
}

kotlin {
    jvmToolchain(21)
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
    testImplementation(libs.testng)
    testImplementation(libs.easymock)
    testImplementation(libs.hamcrest)
    testImplementation(libs.xmlunit.matchers)

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        intellijIdea("2025.3.5")
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)

        // Add plugin dependencies for compilation here:
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")
        bundledPlugin("com.intellij.properties")
        bundledPlugin("com.intellij.javaee")
        bundledPlugin("com.intellij.css")
        bundledModule("intellij.relaxng")
    }
}

// The `test` task runs the JUnit-based platform integration tests. The mock-based
// unit tests under `.../tapestry/core/**` use TestNG, which Gradle can't mix into a
// single task, so they get their own task mirroring the platform-configured `test`.
val testSourceSet = sourceSets["test"]
val platformRuntime = configurations["intellijPlatformClasspath"]
tasks.register<Test>("testng") {
    useTestNG()
    testClassesDirs = testSourceSet.output.classesDirs
    classpath = testSourceSet.runtimeClasspath + platformRuntime

    include("com/intellij/tapestry/core/**")
}
