import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    id("java")
    kotlin("jvm") version "2.3.0"
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "dev.varlock"
version = "0.1.0"

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform {
        // Sandbox builds use a pre-extracted IDE (DMG mounting is blocked there);
        // host builds resolve the platform distribution normally.
        val localIde = System.getenv("VARLOCK_IDE_HOME")
        if (localIde != null) {
            local(localIde)
        } else {
            intellijIdeaUltimate("2026.2.3")
        }
        bundledPlugin("com.intellij.java")
        bundledPlugin("JavaScript")
        bundledPlugin("NodeJS")
        // Python run-config env injection (optional integration); installed as a
        // custom plugin in IDEA Ultimate, resolved from the Marketplace here.
        plugin("PythonCore", "262.10968.63")
    }
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // local() exposes the IDE root directory only; sandboxed compilation needs the
    // split platform module jars explicitly on the classpath.
    if (ideHome != null) {
        compileOnly(fileTree("$ideHome/lib/modules") { include("*.jar") })
    }
}

// The IDE's bundled JBR is unavailable in sandboxed builds (VARLOCK_IDE_HOME);
// run tests on a standard toolchain JDK instead.
val ideHome = System.getenv("VARLOCK_IDE_HOME")

intellijPlatform {
    pluginConfiguration {
        ideaVersion { sinceBuild = "262" }
    }
    // Headless-IDE indexing needs the bundled JBR, which is unavailable in
    // sandboxed builds; skip it (plugin.xml declares no searchable options anyway).
    buildSearchableOptions = false
    pluginVerification {
        // The Python integration knowingly uses unstable/internal APIs (the only
        // extension mechanism the Python plugin offers).
        failureLevel = listOf(
            com.jetbrains.intellij.platform.gradle.VerifierFailureLevel.COMPATIBILITY_WARNINGS,
            com.jetbrains.intellij.platform.gradle.VerifierFailureLevel.DEPRECATED_API_USAGES,
            com.jetbrains.intellij.platform.gradle.VerifierFailureLevel.PLUGIN_DEPENDENCIES,
        )
        if (ideHome != null) {
            // DMG mounting is blocked in sandboxed builds, so verify against the
            // already-extracted local IDE instead of downloading recommended IDEs.
            ides {
                current()
            }
        }
    }
}

tasks {
    test {
        useJUnitPlatform()
        if (ideHome != null) {
            javaLauncher.set(project.javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })
        }
    }
    named("verifyPlugin") {
        if (ideHome != null) {
            // Sandbox cannot write to ~/.pluginVerifier; keep verifier state in a writable dir.
            (this as JavaExec).environment("PLUGIN_VERIFIER_HOME", "/tmp/opencode/plugin-verifier-home")
        }
    }
}
