// :core — the IntelliJ-free symbol-graph indexer and file-format models.
//
// Plain Java, no runtime dependencies. The test suite wraps the model's
// assertion checks as JUnit 5 tests; tests that need real Apertium repositories
// as fixtures skip cleanly (via Assumptions) when those fixtures are absent, so
// the suite is green on a bare checkout and richer when fixtures are provided.

plugins {
    java
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
    // Let fixture-backed tests find optional sample repositories if present.
    systemProperty("morphingbird.fixtures", System.getProperty("morphingbird.fixtures", ""))
}
