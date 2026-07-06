package io.github.alexeyev.morphingbird.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Regression test that pins the verified behaviour of the whole core against the
 * real apertium-kir sources. If a future change to a scanner or the index
 * silently breaks parsing, these assertions fail. Run via {@code main}.
 *
 * <p>The numbers here were established by inspecting the real files and
 * confirming each is correct (e.g. 0 unresolved continuations, the cross-DSL
 * bridge resolving, and only genuine — not false-positive — diagnostics).</p>
 */
public final class CoreRegressionTest {

    private static int failures = 0;
    static final String DIR = "/home/claude/kir-buildtest/";

    public static void main(String[] args) throws Exception {
        String lexc = read(DIR + "apertium-kir.kir.lexc");
        String twol = read(DIR + "apertium-kir.kir.twol");
        String cg3  = read(DIR + "apertium-kir.kir.rlx");

        LexcModel lm = LexcParser.parse(lexc);
        TwolModel tm = TwolModel.parse(twol);
        Cg3Model  cm = Cg3Model.parse(cg3);

        // --- lexc structure ---
        ge("lexc lexicons", 185, lm.lexicons.size());
        ge("lexc entries", 15000, totalEntries(lm));
        ge("lexc declared tags", 130,
                (int) lm.declaredSymbols.stream().filter(s -> s.isTag).count());
        eq("lexc declared archiphonemes", 28,
                (int) lm.declaredSymbols.stream().filter(s -> !s.isTag).count());

        // CRITICAL: every continuation class resolves (no false unresolved).
        long unresolved = lm.continuationRefs.stream()
                .filter(r -> !lm.isResolvedContinuation(r.canonical)).count();
        eq("lexc unresolved continuations", 0L, unresolved);

        // --- twol structure: the lexc<->twol bridge ---
        ge("twol resolved archiphonemes", 28, tm.resolvedArchiphonemes.size());
        ge("twol named sets", 30, tm.sets.size());
        ge("twol named rules", 55, tm.rules.size());

        // --- CG3 structure ---
        ge("cg3 definitions", 100, cm.definitions.size());
        ge("cg3 distinct tags", 50,
                (int) cm.tagRefs.stream().map(r -> r.canonical).distinct().count());

        // --- cross-file index ---
        SymbolIndex idx = new SymbolIndex();
        idx.addLexc(DIR + "l", lm);
        idx.addTwol(DIR + "t", tm);
        idx.addCg3(DIR + "c", cm);

        // Navigation: N-INFL is heavily used.
        neNull("N-INFL has a definition", idx.lexiconDefinition("N-INFL"));
        ge("N-INFL usages", 4000, idx.lexiconUsages("N-INFL").size());

        // Cross-DSL bridge: <gen> declared in lexc, used in both lexc and CG3.
        eq("<gen> declared once", 1, idx.tagDeclarations("<gen>").size());
        ge("<gen> used across DSLs", 10, idx.tagUsages("<gen>").size());

        // Archiphoneme bridge: {G} resolved by a twol rule.
        ge("{G} resolved", 1, idx.archiphonemeResolutions("{G}").size());

        // Validation: zero ERRORS (only WARNINGs for real latent issues), and a
        // small, bounded number of warnings (all verified genuine).
        List<SymbolIndex.Diagnostic> diags = idx.validate();
        long errors = diags.stream()
                .filter(d -> d.severity == SymbolIndex.Diagnostic.Severity.ERROR)
                .count();
        eq("validation errors (must be zero on valid project)", 0L, errors);
        le("validation warnings bounded", diags.size(), 20);

        if (failures == 0) {
            System.out.println("ALL CORE REGRESSION ASSERTIONS PASSED ("
                    + "lexicons=" + lm.lexicons.size()
                    + ", entries=" + totalEntries(lm)
                    + ", warnings=" + diags.size() + ")");
        } else {
            System.out.println(failures + " REGRESSION ASSERTION(S) FAILED");
            System.exit(1);
        }
    }

    private static int totalEntries(LexcModel m) {
        return m.lexicons.stream().mapToInt(l -> l.entries.size()).sum();
    }

    private static <T> void eq(String what, T exp, T act) {
        if (!exp.equals(act)) { fail(what, "expected " + exp + " got " + act); }
    }
    private static void ge(String what, int min, int act) {
        if (act < min) { fail(what, "expected >= " + min + " got " + act); }
    }
    private static void le(String what, int act, int max) {
        if (act > max) { fail(what, "expected <= " + max + " got " + act); }
    }
    private static void neNull(String what, Object o) {
        if (o == null) { fail(what, "was null"); }
    }
    private static void fail(String what, String detail) {
        System.out.println("FAIL " + what + ": " + detail);
        failures++;
    }
    private static String read(String p) throws Exception {
        return new String(Files.readAllBytes(Path.of(p)));
    }
}
