package io.github.alexeyev.morphingbird.core;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Locates optional test fixtures (small real Apertium samples). Fixture-backed
 * tests use this so they run when samples are available and skip cleanly when
 * they are not — keeping the suite green on a bare checkout.
 *
 * <p>Resolution order:</p>
 * <ol>
 *   <li>the {@code morphingbird.fixtures} system property, if set;</li>
 *   <li>{@code core/src/test/resources/fixtures} relative to the working dir;</li>
 *   <li>a few well-known absolute paths used during development.</li>
 * </ol>
 */
public final class TestFixtures {
    private TestFixtures() {}

    /** Returns the fixtures root if one exists, else null. */
    public static Path root() {
        String prop = System.getProperty("morphingbird.fixtures", "");
        if (!prop.isBlank()) {
            Path p = Path.of(prop);
            if (Files.isDirectory(p)) return p;
        }
        for (String candidate : new String[]{
                "core/src/test/resources/fixtures",
                "src/test/resources/fixtures",
                "../core/src/test/resources/fixtures"}) {
            Path p = Path.of(candidate);
            if (Files.isDirectory(p)) return p.toAbsolutePath();
        }
        return null;
    }

    /** Returns a named fixture directory if it exists, else null. */
    public static Path repo(String name) {
        Path root = root();
        if (root == null) return null;
        Path p = root.resolve(name);
        return Files.isDirectory(p) ? p : null;
    }

    /** True if a named fixture is available. */
    public static boolean has(String name) {
        return repo(name) != null;
    }
}
