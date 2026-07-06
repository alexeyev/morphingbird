package io.github.alexeyev.morphingbird.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The CI-facing test suite for {@code :core}. These tests are self-contained —
 * they either build their inputs inline or use the small committed fixture under
 * {@code src/test/resources/fixtures} — so the suite is green on a bare checkout.
 *
 * <p>The deeper, repository-backed assertion programs (e.g. {@code CoreRegressionTest},
 * {@code HaaFullTest}) remain runnable via their {@code main} methods against
 * real Apertium checkouts during development; they are intentionally not part of
 * this gate so CI does not depend on large external corpora.</p>
 */
public class CoreTest {

    // ---- pure parser/model tests (no external files) ----

    @Test
    void lexcScannerRoundTripsAndDecodesEscapes() {
        String src = "Multichar_Symbols\n%<n%>\n%{A%}\n\nLEXICON Root\ncat:cat N ;\n";
        LexcModel m = LexcParser.parse(src);
        // byte-exact reconstruction from tokens
        StringBuilder sb = new StringBuilder();
        for (LexcToken t : LexcScanner.tokenize(src)) {
            if (t.kind != LexcToken.Kind.EOF) sb.append(src, t.start, t.end);
        }
        assertEquals(src, sb.toString(), "scanner must cover the input gaplessly");
        assertTrue(m.declaredSymbols.stream().anyMatch(d -> d.canonical.equals("<n>")));
        assertTrue(m.declaredSymbols.stream().anyMatch(d -> d.canonical.equals("{A}")));
    }

    @Test
    void cg3DistinguishesRegexFlagsFromTags() {
        // "foo"i  and  "foo" i  are regex flags, NOT a tag <i>.
        assertFalse(tags("(0 (\"foo\"i))").contains("<i>"),
                "attached regex flag must not be read as a tag");
        assertFalse(tags("(0 (\"foo\" i))").contains("<i>"),
                "space-separated regex flag must not be read as a tag");
        // a bare tag IS picked up
        assertTrue(tags("SELECT (nom) ;").contains("<nom>"));
    }

    @Test
    void undeclaredTagQuickFixResolvesTheWarning() {
        String lexc = "Multichar_Symbols\n%<n%>\n%<nom%>\n\nLEXICON Root\n"
                + "cat:cat N ;\n\nLEXICON N\n%<n%>%<nom%>: # ;\n";
        String cg3 = "LIST X = acc ;\nSELECT (acc) ;\n";   // acc undeclared

        SymbolIndex idx = new SymbolIndex();
        idx.addLexc("t.lexc", LexcParser.parse(lexc));
        idx.addCg3("t.rlx", Cg3Model.parse(cg3));

        boolean hasUndeclared = idx.validate().stream().anyMatch(d ->
                d.kind == SymbolIndex.Diagnostic.Kind.UNDECLARED_TAG
                        && "<acc>".equals(d.symbol));
        assertTrue(hasUndeclared, "should flag <acc> as undeclared");

        // Apply the fix the IDE would: insert %<acc%> at the computed offset.
        int offset = idx.multicharInsertOffset("t.lexc");
        assertTrue(offset > 0);
        String fixed = lexc.substring(0, offset) + "\n%<acc%>" + lexc.substring(offset);

        SymbolIndex idx2 = new SymbolIndex();
        idx2.addLexc("t.lexc", LexcParser.parse(fixed));
        idx2.addCg3("t.rlx", Cg3Model.parse(cg3));
        assertFalse(idx2.tagDeclarations("<acc>").isEmpty(), "<acc> now declared");
        assertTrue(idx2.validate().stream().noneMatch(d ->
                        d.kind == SymbolIndex.Diagnostic.Kind.UNDECLARED_TAG
                                && "<acc>".equals(d.symbol)),
                "the warning the fix targets must be gone");
    }

    @Test
    void lexdParsesTagsArchiphonemesAndFilters() {
        String lexd = "PATTERNS\nNOUN\n\nPATTERN NOUN\n"
                + "NounRoot[I,m] [<n><m>:] NounInfl\n\n"
                + "LEXICON NounInfl\n<n><m><sg>:{Z}\n<n><m><pl>:lar\n";
        LexdModel m = LexdModel.parse(lexd);
        assertTrue(m.tagRefs.stream().anyMatch(r -> r.canonical.equals("<n>")));
        assertTrue(m.archiphonemeRefs.stream().anyMatch(r -> r.canonical.equals("{Z}")));
        assertTrue(m.filters.stream().anyMatch(f -> f.body.equals("I,m")),
                "tag-filter [I,m] must be captured");
        assertTrue(m.definitions.stream().anyMatch(d -> d.name.equals("NounInfl")));
    }

    @Test
    void testSuiteJsonParses() {
        String json = "{\n  \"a\": { \"input\": \"a-input.txt\", \"mode\": \"x-morph\" },\n"
                + "  \"b\": { \"input\": \"b-input.txt\", \"mode\": \"x-gener\" }\n}";
        TestSuiteModel m = TestSuiteModel.parse(json);
        assertEquals(2, m.tests.size());
        assertTrue(m.byMode().containsKey("x-morph"));
    }

    @Test
    void probModelDetectsUntrainedDefaultByEmptiness() {
        // An all-zero blob has no decodable tagset → reported as untrained.
        ProbModel m = ProbModel.parse(new byte[64]);
        assertTrue(m.looksUntrained);
        assertTrue(m.tags.isEmpty());
    }

    @Test
    void probModelDecodesStringsFromSyntheticBlob() {
        // Encode "adj" the way apertium stores wide-char strings: 01 len (01 ch)*.
        byte[] blob = encodeString("adj");
        ProbModel m = ProbModel.parse(blob);
        assertTrue(m.tags.contains("adj"), "should decode the embedded tag");
    }

    @Test
    void buildGraphReadsModesAndTools() {
        String modes = "<modes><mode name=\"x-morph\"><pipeline>"
                + "<program name=\"hfst-proc\"><file name=\".deps/x.automorf.hfst\"/>"
                + "</program></pipeline></mode></modes>";
        String makefile = "LANG1=x\nBASENAME=apertium-$(LANG1)\n"
                + ".deps/x.automorf.hfst: $(BASENAME).$(LANG1).lexc\n\thfst-lexc $< -o $@\n";
        BuildGraphModel g = BuildGraphModel.build(modes, makefile,
                java.util.List.of("apertium-x.x.lexc"));
        assertTrue(g.modePipelines.containsKey("x-morph"));
        assertTrue(g.tools().contains("hfst-lexc"));
    }

    // ---- fixture-backed test (committed mini sample) ----

    @Test
    void miniFixtureValidatesCleanly() throws Exception {
        Path dir = TestFixtures.repo("mini-kir");
        Assumptions.assumeTrue(dir != null, "mini-kir fixture not present; skipping");

        SymbolIndex idx = new SymbolIndex();
        idx.addLexc("lexc", LexcParser.parse(Files.readString(dir.resolve("apertium-mini.mini.lexc"))));
        idx.addTwol("twol", TwolModel.parse(Files.readString(dir.resolve("apertium-mini.mini.twol"))));
        idx.addCg3("rlx", Cg3Model.parse(Files.readString(dir.resolve("apertium-mini.mini.rlx"))));

        long errors = idx.validate().stream()
                .filter(d -> d.severity == SymbolIndex.Diagnostic.Severity.ERROR)
                .count();
        assertEquals(0, errors, "mini fixture must have no error-level diagnostics");
        assertFalse(idx.archiphonemeResolutions("{A}").isEmpty(),
                "{A} should resolve via the twol rule");
        assertFalse(idx.tagDeclarations("<nom>").isEmpty());
    }

    // ---- helpers ----

    private static java.util.Set<String> tags(String cg3) {
        Cg3Model m = Cg3Model.parse(cg3);
        java.util.Set<String> out = new java.util.HashSet<>();
        for (LexcModel.SymbolRef r : m.tagRefs) out.add(r.canonical);
        return out;
    }

    private static byte[] encodeString(String s) {
        // 01 <len> then for each char 01 <char>
        byte[] b = new byte[2 + s.length() * 2];
        int i = 0;
        b[i++] = 0x01; b[i++] = (byte) s.length();
        for (int k = 0; k < s.length(); k++) { b[i++] = 0x01; b[i++] = (byte) s.charAt(k); }
        return b;
    }
}
