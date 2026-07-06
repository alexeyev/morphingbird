package io.github.alexeyev.morphingbird.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A plain-{@code main} smoke test (no JUnit needed in this environment) that
 * scans the real apertium-kir lexc and reports what the scanner found, so we
 * can eyeball correctness and catch escape-handling bugs against real input.
 */
public final class ScannerSmokeTest {

    public static void main(String[] args) throws Exception {
        String path = args.length > 0 ? args[0]
                : "/home/claude/kir-buildtest/apertium-kir.kir.lexc";
        String src = new String(Files.readAllBytes(Path.of(path)));

        long t0 = System.nanoTime();
        List<LexcToken> toks = LexcScanner.tokenize(src);
        long ms = (System.nanoTime() - t0) / 1_000_000;

        Map<LexcToken.Kind, Integer> counts = new TreeMap<>();
        for (LexcToken t : toks) {
            counts.merge(t.kind, 1, Integer::sum);
        }

        System.out.println("file: " + path);
        System.out.println("size: " + src.length() + " chars, "
                + src.split("\n", -1).length + " lines");
        System.out.println("tokens: " + toks.size() + " in " + ms + " ms");
        System.out.println("by kind: " + counts);

        // Show the first few TAG and ARCHIPHONEME tokens with canonical values.
        System.out.println("\n-- sample TAG tokens (raw => canonical) --");
        printSample(toks, LexcToken.Kind.TAG, 8);
        System.out.println("\n-- sample ARCHIPHONEME tokens --");
        printSample(toks, LexcToken.Kind.ARCHIPHONEME, 8);

        // Round-trip sanity: re-assembling token texts must equal the source.
        StringBuilder sb = new StringBuilder();
        for (LexcToken t : toks) {
            if (t.kind != LexcToken.Kind.EOF) sb.append(t.text);
        }
        boolean roundTrip = sb.toString().equals(src);
        System.out.println("\nround-trip (concat tokens == source): "
                + (roundTrip ? "OK" : "MISMATCH"));
        if (!roundTrip) {
            int i = firstDiff(sb.toString(), src);
            System.out.println("  first diff at offset " + i);
            System.out.println("  scanner: ..." + safe(sb.toString(), i));
            System.out.println("  source : ..." + safe(src, i));
        }
    }

    private static void printSample(List<LexcToken> toks, LexcToken.Kind k, int n) {
        int shown = 0;
        for (LexcToken t : toks) {
            if (t.kind == k) {
                System.out.println("  " + t.text + "  =>  " + t.canonical);
                if (++shown >= n) break;
            }
        }
    }

    private static int firstDiff(String a, String b) {
        int n = Math.min(a.length(), b.length());
        for (int i = 0; i < n; i++) if (a.charAt(i) != b.charAt(i)) return i;
        return n;
    }

    private static String safe(String s, int i) {
        int e = Math.min(s.length(), i + 30);
        return s.substring(Math.max(0, i), e).replace("\n", "\\n");
    }
}
