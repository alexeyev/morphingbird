package io.github.alexeyev.morphingbird.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * End-to-end test of the cross-file symbol index against the real apertium-kir
 * sources: builds the graph from the lexc + twol + CG3 files and exercises
 * navigation, the cross-DSL bridge, and the Tier-1 validation checks.
 */
public final class IndexIntegrationTest {

    static final String DIR = "/home/claude/kir-buildtest/";

    public static void main(String[] args) throws Exception {
        String lexcPath = DIR + "apertium-kir.kir.lexc";
        String twolPath = DIR + "apertium-kir.kir.twol";
        String cg3Path  = DIR + "apertium-kir.kir.rlx";

        SymbolIndex idx = new SymbolIndex();

        long t0 = System.nanoTime();
        idx.addLexc(lexcPath, LexcParser.parse(read(lexcPath)));
        idx.addTwol(twolPath, TwolModel.parse(read(twolPath)));
        idx.addCg3(cg3Path,   Cg3Model.parse(read(cg3Path)));
        long ms = (System.nanoTime() - t0) / 1_000_000;

        System.out.println("=== Built whole-project index in " + ms + " ms ===");
        System.out.println("stats: " + idx.stats());

        // --- Navigation samples ---
        System.out.println("\n=== Navigation ===");
        printNav(idx, "N-INFL");      // a heavily-used inflection class
        printTag(idx, "<nom>");
        printArch(idx, "{G}");

        // --- The cross-DSL bridge: a tag used in CG3 -> declared in lexc ---
        System.out.println("\n=== Cross-DSL bridge (tag <gen>) ===");
        System.out.println("  declared at: " + idx.tagDeclarations("<gen>"));
        System.out.println("  used at " + idx.tagUsages("<gen>").size()
                + " sites (lexc entries + CG3 lists)");

        // --- Validation ---
        System.out.println("\n=== Tier-1 validation (whole project) ===");
        List<SymbolIndex.Diagnostic> diags = idx.validate();
        System.out.println("total diagnostics: " + diags.size());
        long errors = diags.stream()
                .filter(d -> d.severity == SymbolIndex.Diagnostic.Severity.ERROR).count();
        long warns = diags.size() - errors;
        System.out.println("  errors: " + errors + ", warnings: " + warns);

        // Break down warnings by category (first word of message).
        System.out.println("\n  -- sample diagnostics --");
        diags.stream().limit(12).forEach(d ->
                System.out.println("   " + d.severity + ": " + d.message));

        // Count unreachable-lexicon vs unused-tag vs unresolved-arch
        long unreachable = diags.stream()
                .filter(d -> d.message.contains("unreachable")).count();
        long unresolvedArch = diags.stream()
                .filter(d -> d.message.contains("never resolved")).count();
        long undeclaredTag = diags.stream()
                .filter(d -> d.message.contains("never declared")).count();
        System.out.println("\n  by category: unreachable LEXICONs=" + unreachable
                + ", unresolved archiphonemes=" + unresolvedArch
                + ", undeclared tags=" + undeclaredTag);
    }

    private static void printNav(SymbolIndex idx, String lexicon) {
        SymbolIndex.Loc def = idx.lexiconDefinition(lexicon);
        int uses = idx.lexiconUsages(lexicon).size();
        System.out.println("  LEXICON " + lexicon + " defined at "
                + (def == null ? "?" : def.start) + ", "
                + uses + " entries continue into it");
    }

    private static void printTag(SymbolIndex idx, String tag) {
        System.out.println("  tag " + tag + ": declared "
                + idx.tagDeclarations(tag).size() + "x, used "
                + idx.tagUsages(tag).size() + "x");
    }

    private static void printArch(SymbolIndex idx, String arch) {
        System.out.println("  archiphoneme " + arch + ": declared "
                + idx.archiphonemeDeclarations(arch).size() + "x, resolved by "
                + idx.archiphonemeResolutions(arch).size() + " twol rule(s), used "
                + "(go-to-resolution jumps cross-file from lexc to twol)");
    }

    private static String read(String p) throws Exception {
        return new String(Files.readAllBytes(Path.of(p)));
    }
}
