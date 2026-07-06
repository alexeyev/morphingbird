package io.github.alexeyev.morphingbird.core;

import java.util.ArrayList;
import java.util.List;

/**
 * A lenient scanner + model extractor for CG3 constraint grammars
 * ({@code .rlx} disambiguation). For the cross-file index we need:
 * <ul>
 *   <li><b>LIST / SET definitions</b> ({@code LIST Nom = nom ;}) and their
 *       references, for intra-file navigation;</li>
 *   <li><b>morphological tag references</b> — the bare tag words like
 *       {@code nom}, which correspond to the lexc/HFST tags {@code <nom>}.
 *       This is the lexc↔CG3 bridge in the shared symbol graph.</li>
 * </ul>
 *
 * <p>CG3 comments are {@code #} to end of line. Tags inside lists are bare
 * identifiers; angle-bracketed tags can also appear and are normalised to the
 * {@code <tag>} canonical form so they unify with the lexc declarations.</p>
 */
public final class Cg3Model {

    /** A LIST or SET definition. */
    public static final class Def {
        public final String name;
        public final boolean isList;   // true LIST, false SET
        public final int start;
        public final int end;
        public Def(String name, boolean isList, int start, int end) {
            this.name = name; this.isList = isList; this.start = start; this.end = end;
        }
    }

    public final List<Def> definitions = new ArrayList<>();
    /**
     * Canonical tag references (e.g. {@code <nom>}) found in list bodies and
     * rules. Bare CG3 tags like {@code nom} are normalised to {@code <nom>}.
     */
    public final List<LexcModel.SymbolRef> tagRefs = new ArrayList<>();
    /** Names of sections, for the structure view. */
    public final List<LexcModel.SymbolRef> sections = new ArrayList<>();

    public Def definition(String name) {
        for (Def d : definitions) if (d.name.equals(name)) return d;
        return null;
    }

    // --- parsing -----------------------------------------------------------

    public static Cg3Model parse(String src) {
        Cg3Model m = new Cg3Model();
        int len = src.length();
        int pos = 0;
        boolean atLineStart = true;

        while (pos < len) {
            char c = src.charAt(pos);

            // Comment: '#' to end of line.
            if (c == '#') {
                while (pos < len && src.charAt(pos) != '\n') pos++;
                continue;
            }
            if (c == '\n' || c == '\r') { pos++; atLineStart = true; continue; }
            if (c == ' ' || c == '\t') { pos++; continue; }

            // Quoted regex/literal "..." — skip (may contain tag-like chars).
            // A case/regex flag letter (i, r, v, l) may follow the closing
            // quote, either immediately ("foo"i) or after whitespace ("foo" i).
            if (c == '"') {
                pos++;
                while (pos < len && src.charAt(pos) != '"') {
                    if (src.charAt(pos) == '\\' && pos + 1 < len) pos++;
                    pos++;
                }
                if (pos < len) pos++;                 // closing quote
                // Immediately-attached flags: "foo"ir
                while (pos < len && isRegexFlag(src.charAt(pos))) pos++;
                // Space-separated flag: "foo" i  — only consume a lone flag
                // letter that is NOT the start of a longer identifier (so we
                // don't swallow a real following token).
                int look = pos;
                while (look < len && (src.charAt(look) == ' ' || src.charAt(look) == '\t')) look++;
                if (look < len && isRegexFlag(src.charAt(look))
                        && (look + 1 >= len || !isCgIdentChar(src.charAt(look + 1)))) {
                    pos = look + 1;                   // consume the lone flag
                }
                atLineStart = false;
                continue;
            }

            // Angle-bracketed tag: <nom>, <<<, >>> (the latter are BOS/EOS marks).
            if (c == '<') {
                int s = pos; pos++;
                StringBuilder sb = new StringBuilder("<");
                while (pos < len && src.charAt(pos) != '>'
                        && src.charAt(pos) != '\n') {
                    sb.append(src.charAt(pos)); pos++;
                }
                if (pos < len && src.charAt(pos) == '>') { sb.append('>'); pos++; }
                String t = sb.toString();
                // Only treat <...> with letters as a tag (skip <<< / >>>).
                if (t.length() > 2 && Character.isLetter(t.charAt(1))) {
                    m.tagRefs.add(new LexcModel.SymbolRef(t, s, pos));
                }
                atLineStart = false;
                continue;
            }

            // A keyword or identifier.
            if (Character.isLetter(c) || c == '_' || c == '@') {
                int s = pos;
                while (pos < len && isCgIdentChar(src.charAt(pos))) pos++;
                String word = src.substring(s, pos);

                if (atLineStart && (word.equals("LIST") || word.equals("SET"))) {
                    // Definition: next identifier is the name.
                    boolean isList = word.equals("LIST");
                    int q = pos;
                    while (q < len && (src.charAt(q) == ' ' || src.charAt(q) == '\t')) q++;
                    int ns = q;
                    while (q < len && isCgIdentChar(src.charAt(q))) q++;
                    if (q > ns) {
                        m.definitions.add(new Def(src.substring(ns, q), isList, ns, q));
                    }
                    pos = q;
                    atLineStart = false;
                    continue;
                }
                if (atLineStart && word.equals("SECTION")) {
                    m.sections.add(new LexcModel.SymbolRef(word, s, pos));
                    atLineStart = false;
                    continue;
                }
                // A bare lowercase identifier in a list body is a tag reference.
                // We normalise to <word> so it unifies with lexc tags. We only
                // do this for words that look like tags (all lowercase / digits
                // / hyphen), not for CG3 keywords or set names (Capitalised).
                if (!isCgKeyword(word) && looksLikeTag(word)) {
                    m.tagRefs.add(new LexcModel.SymbolRef("<" + word + ">", s, pos));
                }
                atLineStart = false;
                continue;
            }

            atLineStart = false;
            pos++;
        }
        return m;
    }

    private static boolean isCgIdentChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '@'
                || c == '.';
    }

    private static boolean isCgKeyword(String w) {
        switch (w) {
            case "LIST": case "SET": case "SECTION": case "SELECT": case "REMOVE":
            case "IFF": case "ADD": case "MAP": case "SUBSTITUTE": case "APPEND":
            case "DELIMITERS": case "SOFT-DELIMITERS": case "TEMPLATE":
            case "BARRIER": case "TARGET": case "IF": case "TO": case "FROM":
            case "OR": case "AND": case "NOT": case "LINK": case "NEGATE":
            case "BEFORE": case "AFTER": case "WITHIN": case "STATIC":
            // CG3 accepts lowercase boolean/relational operators inline too.
            case "or": case "and": case "not": case "link": case "to": case "from":
            case "if": case "barrier": case "target": case "before": case "after":
                return true;
            default:
                return false;
        }
    }

    /** A single trailing flag after a CG3 string literal (case/regex flags). */
    private static boolean isRegexFlag(char c) {
        return c == 'i' || c == 'r' || c == 'v' || c == 'l';
    }

    /** A tag word is all lowercase letters / digits / hyphen (e.g. {@code px3sp}). */
    private static boolean looksLikeTag(String w) {
        if (w.isEmpty()) return false;
        for (int i = 0; i < w.length(); i++) {
            char c = w.charAt(i);
            if (Character.isUpperCase(c)) return false;       // set names are Capitalised
            if (!(Character.isLowerCase(c) || Character.isDigit(c)
                    || c == '-')) return false;
        }
        // Must contain at least one letter (avoid pure numbers).
        for (int i = 0; i < w.length(); i++) {
            if (Character.isLetter(w.charAt(i))) return true;
        }
        return false;
    }
}
