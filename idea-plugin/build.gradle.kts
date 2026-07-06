// :idea-plugin — the IntelliJ Platform plugin.
//
// Uses the IntelliJ Platform Gradle Plugin (2.x) and depends on the standalone,
// IntelliJ-free :core module (the tested symbol-graph indexer), whose scanners
// and SymbolIndex are reused verbatim.

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.10.4"
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    // Reuse the tested core indexer as a real project dependency.
    implementation(project(":core"))

    intellijPlatform {
        // Target the unified IntelliJ IDEA distribution (2025.3+). The unified
        // build includes the bundled XML support used for the lttoolbox
        // .dix/.lsx schema-driven half. A single build still runs on older and
        // newer IDEs — compatibility is governed by since/until-build below.
        intellijIdea("2025.3")
        bundledPlugin("com.intellij.modules.xml")
        // Note: pluginVerifier(), zipSigner(), and instrumentationTools() are
        // applied automatically by the Gradle plugin 2.10.4+ and are no longer
        // declared explicitly.
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            // Floor kept at 2024.3 so the plugin remains installable on the last
            // pre-unification IDEs as well as every unified release after it.
            sinceBuild = "243"
            untilBuild = provider { null }  // leave open; verifier checks against latest
        }
    }

    pluginVerification {
        ides {
            // Verify against the current recommended IDEs (the latest unified
            // releases), so compatibility is checked, not merely declared.
            recommended()
        }
    }

    // Both blocks read from environment variables and are inert when those are
    // unset: buildPlugin never touches them, and signPlugin/publishPlugin are
    // only invoked by the release workflow when the corresponding GitHub
    // secrets exist. Names follow the intellij-platform-plugin-template.
    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
