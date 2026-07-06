package io.github.alexeyev.morphingbird.core;

import java.util.ArrayList;
import java.util.List;

/**
 * A lenient parser for the Apertium <b>lexd</b> lexicon format (used by newer
 * transducers, including many Athabaskan/Dene languages such as Hän). lexd is
 * structurally different from lexc: instead of continuation classes it composes
 * <em>lexicons</em> positionally through <em>patterns</em>, which suits the
 * prefix-template morphology of these languages.
 *
 * <p>For the cross-file symbol graph we extract the graph-relevant symbols with
 * exact offsets:</p>
 * <ul>
 *   <li><b>tags</b> {@code <np>}, {@code <v>}, {@code <s_1sg>} — the
 *       morphological vocabulary, shared with udx/rlx/dix (so a lexd
 *       {@code <np>} unifies with a udx {@code np} mapping);</li>
 *   <li><b>archiphonemes</b> {@code {Z}}, {@code {E}}, {@code {NOV}};</li>
 *   <li><b>LEXICON</b> and <b>PATTERN</b> definitions and the references to them
 *       inside pattern bodies (lexd-internal navigation).</li>
 * </ul>
 *
 * <p>Two lexd-specific hazards are handled explicitly:</p>
 * <ol>
 *   <li>The file opens with a large comment block documenting the tagset as
 *       {@code # <n> # Noun}. Those are comments, not declarations, so all
 *       {@code #}-to-end-of-line text is skipped before extraction.</li>
 *   <li>{@code <...>} tags must be told apart from the morpheme-boundary
 *       {@code >} and the {@code :} analysis/surface separator. We only treat a
 *       {@code <...>} run containing tag-name characters as a tag.</li>
 * </ol>
 *
 * <p>This intentionally models the symbol graph, not the full lexd compilation
 * semantics (slot indices, anonymous patterns, sieves). It is IntelliJ-free and
 * offset-accurate, so it is unit-testable against real {@code .lexd} files.</p>
 */
public final class LexdModel {

    /** A LEXICON or PATTERN definition (the {@code (N)} slot suffix is stripped). */
    public static final class Def {
        public final String name;
        public final boolean isPattern;   // true=PATTERN, false=LEXICON
        public final int start;
        public final int end;
        public Def(String name, boolean isPattern, int start, int end) {
            this.name = name; this.isPattern = isPattern; this.start = start; this.end = end;
        }
    }

    public final List<LexcModel.SymbolRef> tagRefs = new ArrayList<>();
    public final List<LexcModel.SymbolRef> archiphonemeRefs = new ArrayList<>();
    public final List<Def> definitions = new ArrayList<>();
    /** References to lexicon/pattern names inside PATTERNS / PATTERN bodies. */
    public final List<LexcModel.SymbolRef> nameRefs = new ArrayList<>();

    /** A lexd lexicon tag-filter such as {@code [I,m]} or {@code [m,-unmarked]}. */
    public static final class Filter {
        public final String body;     // raw inner text, e.g. "m,-unmarked,-I"
        public final int start;
        public final int end;
        public Filter(String body, int start, int end) {
            this.body = body; this.start = start; this.end = end;
        }
    }
    /** Tag-filters found on lexicon references in pattern bodies. */
    public final List<Filter> filters = new ArrayList<>();

    public static LexdModel parse(String src) {
        LexdModel m = new LexdModel();
        int len = src.length();

        // Pass over the source line by line, tracking section context so we know
        // when a bare identifier is a pattern's reference to a lexicon.
        int lineStart = 0;
        String section = "";   // "PATTERNS", "PATTERN", "LEXICON", or ""
        while (lineStart <= len) {
            int nl = src.indexOf('\n', lineStart);
            int lineEnd = nl < 0 ? len : nl;

            // Strip an inline comment: everything from an unescaped '#'.
            int contentEnd = lineEnd;
            for (int i = lineStart; i < lineEnd; i++) {
                if (src.charAt(i) == '#') { contentEnd = i; break; }
            }

            // Identify a section header.
            String trimmed = src.substring(lineStart, contentEnd).trim();
            if (trimmed.startsWith("PATTERNS")) {
                section = "PATTERNS";
            } else if (trimmed.startsWith("PATTERN ") || trimmed.equals("PATTERN")) {
                section = "PATTERN";
                recordDef(src, lineStart, contentEnd, true, m);
            } else if (trimmed.startsWith("LEXICON ") || trimmed.equals("LEXICON")) {
                section = "LEXICON";
                recordDef(src, lineStart, contentEnd, false, m);
            } else {
                // Body line: extract tags, archiphonemes, and (in pattern
                // sections) lexicon-name references.
                extractInline(src, lineStart, contentEnd, m);
                if (section.equals("PATTERNS") || section.equals("PATTERN")) {
                    extractNameRefs(src, lineStart, contentEnd, m);
                }
            }

            if (nl < 0) break;
            lineStart = nl + 1;
        }
        return m;
    }

    /** Records a {@code LEXICON Name(N)} / {@code PATTERN Name} definition. */
    private static void recordDef(String src, int from, int to, boolean isPattern,
                                  LexdModel m) {
        String line = src.substring(from, to);
        int kwLen = isPattern ? "PATTERN".length() : "LEXICON".length();
        int i = kwLen;
        while (i < line.length() && Character.isWhitespace(line.charAt(i))) i++;
        int nameStart = i;
        while (i < line.length()
                && (Character.isLetterOrDigit(line.charAt(i))
                || line.charAt(i) == '-' || line.charAt(i) == '_')) i++;
        if (i > nameStart) {
            String name = line.substring(nameStart, i);
            m.definitions.add(new Def(name, isPattern,
                    from + nameStart, from + i));
        }
    }

    /** Extracts {@code <tag>} and {@code {archiphoneme}} runs from a body span. */
    private static void extractInline(String src, int from, int to, LexdModel m) {
        int i = from;
        while (i < to) {
            char c = src.charAt(i);
            if (c == '<') {
                int close = src.indexOf('>', i + 1);
                if (close >= 0 && close < to) {
                    String inner = src.substring(i + 1, close);
                    if (isTagName(inner)) {
                        // a lexd tag may be compound in source as <v><tv>; each
                        // <...> is captured separately, canonical "<v>".
                        m.tagRefs.add(new LexcModel.SymbolRef("<" + inner + ">",
                                i, close + 1));
                        i = close + 1;
                        continue;
                    }
                }
                i++;
            } else if (c == '{') {
                int close = src.indexOf('}', i + 1);
                if (close >= 0 && close < to) {
                    String inner = src.substring(i + 1, close);
                    if (!inner.isEmpty()) {
                        m.archiphonemeRefs.add(new LexcModel.SymbolRef(
                                "{" + inner + "}", i, close + 1));
                    }
                    i = close + 1;
                    continue;
                }
                i++;
            } else {
                i++;
            }
        }
    }

    /**
     * In PATTERNS / PATTERN bodies, extracts references to lexicon/pattern names.
     * A reference is a bare identifier optionally followed by a {@code (N)} slot
     * index, a {@code [tag-filter]}, or {@code ?} (e.g. {@code ProperNouns(1)},
     * {@code NounRoot[I,m]}, {@code Prepositions}).
     *
     * <p>Crucially this skips the contents of {@code [...]} tag-filters and
     * {@code <...>} tag runs, which previously leaked tag names ({@code m},
     * {@code f}) and filter flags ({@code I}, {@code unmarked}) into the
     * reference list and made Find Usages on a lexd lexicon noisy.</p>
     */
    private static void extractNameRefs(String src, int from, int to, LexdModel m) {
        int i = from;
        boolean atTokenStart = true;   // are we at the start of a whitespace-delimited token?
        while (i < to) {
            char c = src.charAt(i);
            // Skip a [tag-filter] entirely; record it as a Filter on the model.
            if (c == '[') {
                int close = src.indexOf(']', i + 1);
                if (close < 0 || close >= to) { i++; continue; }
                String inner = src.substring(i + 1, close);
                // A filter contains comma-separated tag flags (possibly negated).
                if (looksLikeFilter(inner)) {
                    m.filters.add(new Filter(inner, i, close + 1));
                }
                i = close + 1;
                atTokenStart = false;
                continue;
            }
            // Skip a <tag> run.
            if (c == '<') {
                int close = src.indexOf('>', i + 1);
                if (close >= 0 && close < to) { i = close + 1; atTokenStart = false; continue; }
                i++; continue;
            }
            // Skip a {archiphoneme}.
            if (c == '{') {
                int close = src.indexOf('}', i + 1);
                if (close >= 0 && close < to) { i = close + 1; atTokenStart = false; continue; }
                i++; continue;
            }
            // Skip a (N) slot index.
            if (c == '(') {
                int close = src.indexOf(')', i + 1);
                if (close >= 0 && close < to) { i = close + 1; atTokenStart = false; continue; }
                i++; continue;
            }
            if (Character.isWhitespace(c)) { atTokenStart = true; i++; continue; }
            // An identifier at a token start is a lexicon/pattern reference.
            if (Character.isLetter(c)) {
                int s = i;
                while (i < to && (Character.isLetterOrDigit(src.charAt(i))
                        || src.charAt(i) == '-' || src.charAt(i) == '_')) i++;
                String name = src.substring(s, i);
                if (atTokenStart
                        && !name.equals("PATTERN") && !name.equals("PATTERNS")
                        && !name.equals("LEXICON")) {
                    m.nameRefs.add(new LexcModel.SymbolRef(name, s, i));
                }
                atTokenStart = false;
            } else {
                atTokenStart = false;
                i++;
            }
        }
    }

    /**
     * A lexd tag-filter body is comma-separated flags, each an optionally-negated
     * tag name (e.g. {@code I,m} or {@code m,-unmarked,-I}). Distinguishes a real
     * filter from, say, a bracketed inline entry {@code [<n>:]}.
     */
    private static boolean looksLikeFilter(String inner) {
        if (inner.isEmpty() || inner.contains("<") || inner.contains(":")) return false;
        for (String part : inner.split(",")) {
            String p = part.trim();
            if (p.startsWith("-")) p = p.substring(1);
            if (p.isEmpty()) return false;
            for (int i = 0; i < p.length(); i++) {
                char c = p.charAt(i);
                if (!(Character.isLetterOrDigit(c) || c == '_' || c == '-')) return false;
            }
        }
        return true;
    }

    /** A lexd tag name: lowercase letters, digits, underscore (e.g. {@code s_1sg}). */
    private static boolean isTagName(String s) {
        if (s.isEmpty()) return false;
        boolean hasLetter = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'a' && c <= 'z') hasLetter = true;
            else if (!((c >= '0' && c <= '9') || c == '_' )) return false;
        }
        return hasLetter;
    }
}
