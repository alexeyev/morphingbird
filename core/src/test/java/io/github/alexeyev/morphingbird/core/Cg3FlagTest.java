package io.github.alexeyev.morphingbird.core;

/**
 * Regression test for CG3 case/regex flags after string literals, in both the
 * attached ("foo"i) and space-separated ("foo" i) forms. The space-separated
 * form was found in apertium-kaz ("&lt;астында&gt;" i) and previously leaked a
 * bogus &lt;i&gt; tag into the index.
 */
public final class Cg3FlagTest {
    private static int fail = 0;

    public static void main(String[] args) {
        // Attached flag
        noTag("(0 (\"foo\"i))", "<i>");
        // Space-separated flag (the kaz case)
        noTag("(0 (\"<bar>\" i))", "<i>");
        // Multiple space-flags in one line
        noTag("(0 (\"<x>\" i) OR (\"<y>\" i))", "<i>");
        // A real 'inline' word must NOT be eaten as a flag (it's a longer ident)
        hasTagOrKeyword("(0 (\"x\" inline))");

        if (fail == 0) System.out.println("ALL CG3 FLAG ASSERTIONS PASSED");
        else { System.out.println(fail + " CG3 FLAG ASSERTION(S) FAILED"); System.exit(1); }
    }

    private static void noTag(String src, String forbidden) {
        Cg3Model m = Cg3Model.parse(src);
        boolean found = m.tagRefs.stream().anyMatch(r -> r.canonical.equals(forbidden));
        if (found) {
            System.out.println("FAIL: '" + src + "' produced forbidden tag " + forbidden);
            fail++;
        }
    }

    private static void hasTagOrKeyword(String src) {
        // Just ensure it parses without throwing and 'inline' isn't dropped as a
        // flag char silently turning into <i>. We assert no <i> specifically.
        Cg3Model m = Cg3Model.parse(src);
        if (m.tagRefs.stream().anyMatch(r -> r.canonical.equals("<i>"))) {
            System.out.println("FAIL: 'inline' was mis-parsed into <i> in '" + src + "'");
            fail++;
        }
    }
}
