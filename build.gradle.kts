// Morphingbird — root build.
//
// Two modules:
//   :core        — plain Java, zero dependencies. The tested symbol-graph indexer
//                  and all file-format models. Has its own JUnit test suite.
//   :idea-plugin — the IntelliJ Platform plugin that wraps :core.
//
// The IntelliJ Platform plugin is applied only in :idea-plugin so that :core
// stays buildable and testable without the (network-fetched) IDE distribution.

plugins {
    java
}

allprojects {
    group = "io.github.alexeyev.morphingbird"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    extensions.configure<JavaPluginExtension> {
        // Java 21: required by the IntelliJ Platform 2025.3+ target in
        // :idea-plugin. :core is plain Java and compiles cleanly under 21 too,
        // so a single shared baseline keeps the two modules consistent.
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }
}
