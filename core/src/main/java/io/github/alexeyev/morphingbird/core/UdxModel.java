package io.github.alexeyev.morphingbird.core;

import java.util.ArrayList;
import java.util.List;

/**
 * A parser for Apertium {@code .udx} files — tab-separated rules mapping
 * Apertium tags to Universal Dependencies POS and features (see Apertium's
 * ud-scripts; the "8-column rule file" form). A representative row:
 *
 * <pre>
 *   _    prn    dem|qnt    _    _    PRON    PronType=Dem    _
 * </pre>
 *
 * <p>The columns are a match pattern → UD output. Columns 1–4 are the
 * <em>match</em> side (lemma, POS tag, feature tags, …) using {@code _} as a
 * wildcard; column 6 is the output UD POS ({@code PRON}); column 7 the output UD
 * features ({@code PronType=Dem}). The Apertium tags being matched live in
 * <b>columns 2 and 3</b>, and within column 3 a {@code |} separates a tag
 * combination ({@code dem|qnt}).</p>
 *
 * <p>For the symbol graph we extract those Apertium tag references (with exact
 * source offsets) so a {@code .udx} tag links into the same vocabulary as lexc
 * {@code %<dem%>} / CG3 {@code dem} / lttoolbox {@code <s n="dem"/>}. That makes
 * {@code .udx} navigable and lets the validator flag a UD mapping for a tag the
 * morphology never emits.</p>
 *
 * <p>This is IntelliJ-free and offset-accurate, so it is unit-testable against
 * real {@code .udx} files.</p>
 */
public final class UdxModel {

    /** A reference to an Apertium tag at a source location (canonical {@code <tag>}). */
    public final List<LexcModel.SymbolRef> tagRefs = new ArrayList<>();

    /** A parsed mapping row, for the structure view / hovers. */
    public static final class Mapping {
        public final List<String> apertiumTags;  // canonical, e.g. ["<prn>","<dem>"]
        public final String udPos;               // e.g. "PRON" or "" if none
        public final String udFeats;             // e.g. "PronType=Dem" or ""
        public final int lineStart;
        public final int lineEnd;
        public Mapping(List<String> tags, String udPos, String udFeats,
                       int lineStart, int lineEnd) {
            this.apertiumTags = tags; this.udPos = udPos; this.udFeats = udFeats;
            this.lineStart = lineStart; this.lineEnd = lineEnd;
        }
    }

    public final List<Mapping> mappings = new ArrayList<>();

    public static UdxModel parse(String src) {
        UdxModel m = new UdxModel();
        int len = src.length();
        int lineStart = 0;
        while (lineStart <= len) {
            int nl = src.indexOf('\n', lineStart);
            int lineEnd = nl < 0 ? len : nl;
            parseRow(src, lineStart, lineEnd, m);
            if (nl < 0) break;
            lineStart = nl + 1;
        }
        return m;
    }

    /** Parses a single row [lineStart, lineEnd). */
    private static void parseRow(String src, int lineStart, int lineEnd, UdxModel m) {
        // Skip blank lines and comments (some udx use '#').
        int firstNonWs = lineStart;
        while (firstNonWs < lineEnd && Character.isWhitespace(src.charAt(firstNonWs))) {
            firstNonWs++;
        }
        if (firstNonWs >= lineEnd) return;
        if (src.charAt(firstNonWs) == '#') return;

        // Split into tab-delimited columns, tracking each column's start offset.
        List<int[]> cols = new ArrayList<>();   // {start, end}
        int colStart = lineStart;
        for (int i = lineStart; i <= lineEnd; i++) {
            if (i == lineEnd || src.charAt(i) == '\t') {
                cols.add(new int[]{colStart, i});
                colStart = i + 1;
            }
        }
        if (cols.size() < 3) return;   // not a real mapping row

        List<String> tags = new ArrayList<>();
        // Apertium tags live in columns 2 and 3 (1-based) => indices 1 and 2.
        extractTags(src, cols.get(1), tags, m);   // POS-tag column
        extractTags(src, cols.get(2), tags, m);   // feature-tag column

        String udPos = cols.size() > 5 ? value(src, cols.get(5)) : "";
        String udFeats = cols.size() > 6 ? value(src, cols.get(6)) : "";
        m.mappings.add(new Mapping(tags,
                isWild(udPos) ? "" : udPos,
                isWild(udFeats) ? "" : udFeats,
                lineStart, lineEnd));
    }

    /**
     * Extracts Apertium tags from a column whose value may be {@code _} (wild,
     * skip), a single tag ({@code nom}), or a {@code |}-separated combination
     * ({@code dem|qnt}). Each tag yields a canonical {@code <tag>} ref with the
     * exact source offsets of that token.
     */
    private static void extractTags(String src, int[] col, List<String> tags,
                                    UdxModel m) {
        int start = col[0], end = col[1];
        if (start >= end) return;
        String text = src.substring(start, end);
        if (isWild(text)) return;

        int seg = start;
        for (int i = start; i <= end; i++) {
            if (i == end || src.charAt(i) == '|') {
                if (i > seg) {
                    String tok = src.substring(seg, i).trim();
                    if (!tok.isEmpty() && !isWild(tok) && looksLikeTag(tok)) {
                        // offset of the trimmed token
                        int ts = seg, te = i;
                        // tighten to the trimmed token range
                        while (ts < te && Character.isWhitespace(src.charAt(ts))) ts++;
                        while (te > ts && Character.isWhitespace(src.charAt(te - 1))) te--;
                        String canon = "<" + tok + ">";
                        tags.add(canon);
                        m.tagRefs.add(new LexcModel.SymbolRef(canon, ts, te));
                    }
                }
                seg = i + 1;
            }
        }
    }

    private static String value(String src, int[] col) {
        return src.substring(col[0], col[1]).trim();
    }

    private static boolean isWild(String s) {
        return s == null || s.isEmpty() || s.equals("_");
    }

    /** Apertium tags are lowercase letters / digits / hyphen (e.g. {@code px3sp}). */
    private static boolean looksLikeTag(String w) {
        if (w.isEmpty()) return false;
        boolean hasLetter = false;
        for (int i = 0; i < w.length(); i++) {
            char c = w.charAt(i);
            if (Character.isUpperCase(c)) return false;   // UD POS/feats are not bare tags
            if (Character.isLetter(c)) hasLetter = true;
            else if (!(Character.isDigit(c) || c == '-' || c == '_')) {
                // '=' or other punctuation => this is a UD feature, not a tag
                if (c == '=') return false;
                return false;
            }
        }
        return hasLetter;
    }
}
