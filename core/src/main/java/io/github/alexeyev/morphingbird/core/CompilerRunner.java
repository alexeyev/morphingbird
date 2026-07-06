package io.github.alexeyev.morphingbird.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tier-2 validation: actually compile a single apertium source file with the
 * real toolchain and parse the diagnostics. This is IntelliJ-free (plain
 * {@link ProcessBuilder}) so it is unit-testable against an installed toolchain;
 * the IntelliJ layer wraps it in an {@code ExternalAnnotator}.
 *
 * <p>Isolation (Refalcon's lesson): the file is copied to a fresh temp directory
 * and compiled there, so a half-written buffer never corrupts the real build and
 * concurrent runs don't collide.</p>
 */
public final class CompilerRunner {

    /** A compiler diagnostic with a source position (1-based line/col). */
    public static final class Diag {
        public enum Severity { ERROR, WARNING }
        public final Severity severity;
        public final int line;     // 1-based; 0 if unknown
        public final int column;   // 1-based; 0 if unknown
        public final String message;
        public Diag(Severity sev, int line, int column, String message) {
            this.severity = sev; this.line = line; this.column = column;
            this.message = message;
        }
        @Override public String toString() {
            return severity + " " + line + ":" + column + " " + message;
        }
    }

    /** The kind of source, which determines the compiler invoked. */
    public enum Kind { LEXC, TWOL, CG3 }

    /** Result: the diagnostics plus whether the tool was found/ran. */
    public static final class Result {
        public final List<Diag> diagnostics;
        public final boolean toolAvailable;
        public final String toolName;
        public Result(List<Diag> d, boolean avail, String tool) {
            this.diagnostics = d; this.toolAvailable = avail; this.toolName = tool;
        }
    }

    private static final Kind[] KINDS = Kind.values();

    /** Picks the compiler kind from a filename, or null if unsupported. */
    public static Kind kindForFile(String name) {
        if (name.endsWith(".lexc")) return Kind.LEXC;
        if (name.endsWith(".twol") || name.endsWith(".twoc")) return Kind.TWOL;
        if (name.endsWith(".rlx")) return Kind.CG3;
        return null;
    }

    /**
     * Compiles {@code content} (the in-editor text) of the given kind in an
     * isolated temp dir and returns diagnostics. {@code toolOverrideDir} may hold
     * an explicit directory to find binaries in (else PATH is used).
     */
    public static Result compile(Kind kind, String fileName, String content,
                                 String toolOverrideDir) {
        List<Diag> diags = new ArrayList<>();
        String tool = toolFor(kind);
        String bin = resolve(tool, toolOverrideDir);
        if (bin == null) {
            return new Result(diags, false, tool);
        }

        Path tmp = null;
        try {
            tmp = Files.createTempDirectory("apertium-tier2-");
            Path src = tmp.resolve(safeName(fileName, kind));
            Files.write(src, content.getBytes(StandardCharsets.UTF_8));
            Path out = tmp.resolve("out.hfst");

            List<String> cmd = command(kind, bin, src, out);
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(tmp.toFile());
            pb.redirectErrorStream(true);
            Process p = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    output.append(line).append('\n');
                }
            }
            p.waitFor();
            parseDiagnostics(kind, output.toString(), src.getFileName().toString(), diags);
        } catch (Exception e) {
            // Treat infrastructure failure as "no diagnostics" rather than noise.
        } finally {
            if (tmp != null) deleteRecursively(tmp.toFile());
        }
        return new Result(diags, true, tool);
    }

    private static String toolFor(Kind kind) {
        switch (kind) {
            case LEXC: return "hfst-lexc";
            case TWOL: return "hfst-twolc";
            case CG3:  return "cg-comp";
            default:   return null;
        }
    }

    private static List<String> command(Kind kind, String bin, Path src, Path out) {
        List<String> cmd = new ArrayList<>();
        cmd.add(bin);
        switch (kind) {
            case LEXC:
                // hfst-lexc <file> -o <out>
                cmd.add(src.toString());
                cmd.add("-o");
                cmd.add(out.toString());
                break;
            case TWOL:
                // hfst-twolc <file> -o <out>
                cmd.add(src.toString());
                cmd.add("-o");
                cmd.add(out.toString());
                break;
            case CG3:
                // cg-comp <file> <out>
                cmd.add(src.toString());
                cmd.add(out.toString());
                break;
        }
        return cmd;
    }

    /** File name in temp dir; keep extension so the tool recognises it. */
    private static String safeName(String fileName, Kind kind) {
        String base = new File(fileName).getName();
        if (base.isBlank()) {
            switch (kind) {
                case LEXC: return "buffer.lexc";
                case TWOL: return "buffer.twol";
                case CG3:  return "buffer.rlx";
            }
        }
        return base;
    }

    // --- diagnostic parsing ------------------------------------------------

    // hfst-twolc / hfst-lexc style:  file:line:col: message   OR  file:line.col-...
    private static final Pattern LINE_COL = Pattern.compile(
            "(?:^|\\s)([^\\s:]+):(\\d+)(?:[:.](\\d+))?(?:-\\S+)?:?\\s*(.*)$");
    // cg-comp style: "Error: ... on line N" or "line N:"
    private static final Pattern CG_LINE = Pattern.compile(
            "(?i)(error|warning)[^\\n]*?line\\s+(\\d+)[:\\s]*(.*)$");

    static void parseDiagnostics(Kind kind, String output, String srcName,
                                 List<Diag> out) {
        for (String raw : output.split("\n")) {
            String line = raw.strip();
            if (line.isEmpty()) continue;

            if (kind == Kind.CG3) {
                Matcher cg = CG_LINE.matcher(line);
                if (cg.find()) {
                    Diag.Severity sev = cg.group(1).equalsIgnoreCase("warning")
                            ? Diag.Severity.WARNING : Diag.Severity.ERROR;
                    out.add(new Diag(sev, parseInt(cg.group(2)), 0,
                            trimMsg(cg.group(3), line)));
                    continue;
                }
            }
            Matcher m = LINE_COL.matcher(line);
            if (m.find()) {
                // Only accept matches whose file part looks like our source (or a
                // bare line number form), to avoid matching ratios/timestamps.
                String file = m.group(1);
                if (file.equals(srcName) || file.endsWith("." + extOf(kind))
                        || file.contains("apertium-tier2-")) {
                    Diag.Severity sev = line.toLowerCase().contains("warning")
                            ? Diag.Severity.WARNING : Diag.Severity.ERROR;
                    out.add(new Diag(sev, parseInt(m.group(2)),
                            parseInt(m.group(3)), trimMsg(m.group(4), line)));
                }
            }
        }
    }

    private static String extOf(Kind kind) {
        switch (kind) {
            case LEXC: return "lexc";
            case TWOL: return "twol";
            case CG3:  return "rlx";
            default:   return "";
        }
    }

    private static String trimMsg(String msg, String fallback) {
        if (msg == null || msg.isBlank()) return fallback;
        return msg.strip();
    }

    private static int parseInt(String s) {
        if (s == null) return 0;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    // --- binary resolution -------------------------------------------------

    private static String resolve(String tool, String overrideDir) {
        if (tool == null) return null;
        if (overrideDir != null && !overrideDir.isBlank()) {
            File f = new File(overrideDir, tool);
            if (f.canExecute()) return f.getAbsolutePath();
        }
        String path = System.getenv("PATH");
        if (path != null) {
            for (String dir : path.split(File.pathSeparator)) {
                File f = new File(dir, tool);
                if (f.canExecute()) return f.getAbsolutePath();
            }
        }
        for (String dir : new String[]{"/usr/local/bin", "/usr/bin",
                "/opt/homebrew/bin", "/opt/local/bin"}) {
            File f = new File(dir, tool);
            if (f.canExecute()) return f.getAbsolutePath();
        }
        return null;
    }

    private static void deleteRecursively(File f) {
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) deleteRecursively(k);
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }
}
