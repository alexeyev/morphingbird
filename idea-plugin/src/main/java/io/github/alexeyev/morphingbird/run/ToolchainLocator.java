package io.github.alexeyev.morphingbird.run;

import java.io.File;
import java.util.List;

/**
 * Locates the apertium toolchain binary. Mirrors Refalcon's auto-detect pattern:
 * an explicit path wins; otherwise search {@code PATH} and a few standard
 * install locations. Note (also Refalcon's hard-won lesson): a running IDE keeps
 * the {@code PATH} it started with, so a freshly installed toolchain may not be
 * visible until restart — callers should surface that hint on failure.
 */
public final class ToolchainLocator {

    private static final String[] BINARIES = {"apertium"};
    private static final String[] EXTRA_DIRS = {
            "/usr/local/bin", "/usr/bin", "/opt/local/bin",
            "/opt/homebrew/bin",                  // Apple silicon Homebrew
            System.getProperty("user.home") + "/.local/bin"
    };

    private ToolchainLocator() {}

    /**
     * Resolves the apertium binary. If {@code explicit} is non-empty and exists,
     * it is returned; otherwise PATH and standard dirs are searched. Returns
     * {@code null} if nothing is found.
     */
    public static String resolveApertium(String explicit) {
        if (explicit != null && !explicit.isBlank()) {
            File f = new File(explicit);
            if (f.canExecute()) return f.getAbsolutePath();
        }
        // PATH
        String path = System.getenv("PATH");
        if (path != null) {
            for (String dir : path.split(File.pathSeparator)) {
                String hit = firstExecutable(dir);
                if (hit != null) return hit;
            }
        }
        // Standard locations
        for (String dir : EXTRA_DIRS) {
            String hit = firstExecutable(dir);
            if (hit != null) return hit;
        }
        return null;
    }

    private static String firstExecutable(String dir) {
        if (dir == null || dir.isEmpty()) return null;
        for (String bin : BINARIES) {
            File f = new File(dir, bin);
            if (f.canExecute()) return f.getAbsolutePath();
        }
        return null;
    }

    /** Human-readable hint shown when the toolchain can't be found. */
    public static String notFoundHint() {
        return "apertium not found on PATH or in standard locations. Install the "
                + "apertium toolchain, set the binary path in the run "
                + "configuration, and note that a running IDE keeps its startup "
                + "PATH — restart the IDE after installing.";
    }
}
