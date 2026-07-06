package io.github.alexeyev.morphingbird.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Self-contained assertion tests (no JUnit needed in this sandbox). Run via
 * {@code main}; exits non-zero and prints failures if any assertion fails.
 * These lock in the {@code %}-escape behaviour that everything else depends on.
 */
public final class ScannerAssertTest {

    private static int failures = 0;

    public static void main(String[] args) {
        testPlainTag();
        testArchiphoneme();
        testMorphemeBoundary();
        testLiteralSpaceInForm();
        testEscapedBangNotComment();
        testEntryShape();
        testLexiconHeader();
        testInlineSymbolsSplit();
        testCommentToEol();
        testUnterminatedSymbolLenient();
        testRoundTripVariety();

        if (failures == 0) {
            System.out.println("ALL SCANNER ASSERTIONS PASSED");
        } else {
            System.out.println(failures + " ASSERTION(S) FAILED");
            System.exit(1);
        }
    }

    // --- individual cases --------------------------------------------------

    private static void testPlainTag() {
        LexcToken t = onlyMeaningful("%<nom%>").get(0);
        eq("tag kind", LexcToken.Kind.TAG, t.kind);
        eq("tag canonical", "<nom>", t.canonical);
        eq("tag raw", "%<nom%>", t.text);
    }

    private static void testArchiphoneme() {
        LexcToken t = onlyMeaningful("%{A%}").get(0);
        eq("arch kind", LexcToken.Kind.ARCHIPHONEME, t.kind);
        eq("arch canonical", "{A}", t.canonical);
    }

    private static void testMorphemeBoundary() {
        // "%>" is a literal '>' (morpheme boundary), not OTHER, not a symbol.
        List<LexcToken> ts = onlyMeaningful("%>");
        eq("boundary count", 1, ts.size());
        eq("boundary kind", LexcToken.Kind.IDENTIFIER, ts.get(0).kind);
        eq("boundary text", "%>", ts.get(0).text);
    }

    private static void testLiteralSpaceInForm() {
        // "a%<b" — '% ' is a literal space inside the form, so "foo% bar"
        // is one WORD, not two.
        List<LexcToken> ts = onlyMeaningful("foo% bar");
        eq("literal-space single word", 1, ts.size());
        eq("literal-space text", "foo% bar", ts.get(0).text);
    }

    private static void testEscapedBangNotComment() {
        // "%!" is a literal '!', so the rest of the line is NOT a comment.
        List<LexcToken> ts = onlyMeaningful("a%!b");
        eq("escaped-bang single word", 1, ts.size());
        eq("escaped-bang text", "a%!b", ts.get(0).text);
    }

    private static void testEntryShape() {
        // upper:lower CONT ;
        List<LexcToken> ts = onlyMeaningful("ибарат:ибарат N-INFL ;");
        eq("entry token count", 5, ts.size());
        eq("entry upper", LexcToken.Kind.IDENTIFIER, ts.get(0).kind);
        eq("entry colon", LexcToken.Kind.COLON, ts.get(1).kind);
        eq("entry lower", LexcToken.Kind.IDENTIFIER, ts.get(2).kind);
        eq("entry cont", "N-INFL", ts.get(3).text);
        eq("entry semi", LexcToken.Kind.SEMICOLON, ts.get(4).kind);
    }

    private static void testLexiconHeader() {
        List<LexcToken> ts = onlyMeaningful("LEXICON Nouns");
        eq("lexicon kw", LexcToken.Kind.KW_LEXICON, ts.get(0).kind);
        eq("lexicon name", "Nouns", ts.get(1).text);
    }

    private static void testInlineSymbolsSplit() {
        // The lower side "%>%{N%}%{I%}н" must split into boundary + two
        // archiphonemes + stem.
        List<LexcToken> ts = onlyMeaningful("%>%{N%}%{I%}н");
        eq("inline count", 4, ts.size());
        eq("inline[0] boundary", "%>", ts.get(0).text);
        eq("inline[1] arch", "{N}", ts.get(1).canonical);
        eq("inline[2] arch", "{I}", ts.get(2).canonical);
        eq("inline[3] stem", LexcToken.Kind.IDENTIFIER, ts.get(3).kind);
    }

    private static void testCommentToEol() {
        List<LexcToken> all = LexcScanner.tokenize("foo ! a comment\nbar");
        // find the comment
        LexcToken c = null;
        for (LexcToken t : all) if (t.kind == LexcToken.Kind.COMMENT) c = t;
        neNull("comment present", c);
        eq("comment text", "! a comment", c.text);
    }

    private static void testUnterminatedSymbolLenient() {
        // "%<oops" never closes — scanner must not throw and must make progress.
        List<LexcToken> ts = onlyMeaningful("%<oops");
        eq("unterminated yields one token", 1, ts.size());
        eq("unterminated kind", LexcToken.Kind.TAG, ts.get(0).kind);
        // canonical falls back to raw when unterminated
        eq("unterminated canonical==raw", ts.get(0).text, ts.get(0).canonical);
    }

    private static void testRoundTripVariety() {
        String[] samples = {
            "LEXICON Root\n",
            "%<n%>:%<n%> # ;\n",
            "x%>y:z%{A%}w CONT ; ! note\n",
            "  спасибо:спасибо   N-INFL ;\n",
            "%<gen%>:%>%{N%}%{I%}н CLIT ;\n"
        };
        for (String s : samples) {
            StringBuilder sb = new StringBuilder();
            for (LexcToken t : LexcScanner.tokenize(s)) {
                if (t.kind != LexcToken.Kind.EOF) sb.append(t.text);
            }
            eq("round-trip: " + s.trim(), s, sb.toString());
        }
    }

    // --- helpers -----------------------------------------------------------

    /** Tokenises and drops WHITESPACE and EOF, leaving meaningful tokens. */
    private static List<LexcToken> onlyMeaningful(String src) {
        List<LexcToken> out = new ArrayList<>();
        for (LexcToken t : LexcScanner.tokenize(src)) {
            if (t.kind == LexcToken.Kind.WHITESPACE || t.kind == LexcToken.Kind.EOF) {
                continue;
            }
            out.add(t);
        }
        return out;
    }

    private static <T> void eq(String what, T expected, T actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            System.out.println("FAIL " + what + ": expected <" + expected
                    + "> but was <" + actual + ">");
            failures++;
        }
    }

    private static void neNull(String what, Object o) {
        if (o == null) {
            System.out.println("FAIL " + what + ": was null");
            failures++;
        }
    }
}
