package io.github.alexeyev.morphingbird.core;

import java.util.ArrayList;
import java.util.List;

/**
 * A lenient scanner + lightweight model extractor for apertium twol
 * (two-level rules: {@code .twol} / {@code .twoc}).
 *
 * <p>We do not need a full twol grammar for the plugin's purposes. What the
 * cross-file index needs from twol is:</p>
 * <ul>
 *   <li>the <b>archiphonemes it resolves</b> — the {@code %{G%}:г} pairs in the
 *       {@code Alphabet} section, which are the bridge from the lexc symbol
 *       declarations to their surface realisation;</li>
 *   <li>the <b>named Sets / Definitions</b> ({@code Cns = …}) and their
 *       references, for intra-file navigation;</li>
 *   <li>the <b>named Rules</b> ({@code "rule name" …}) for the structure view.</li>
 * </ul>
 *
 * <p>The {@code %} escape rules match lexc: {@code %{X%}} is the archiphoneme
 * {@code {X}}, {@code %0} is epsilon/zero, {@code %&nbsp;} a literal space, etc.</p>
 */
public final class TwolModel {

    public enum Section { NONE, ALPHABET, SETS, DEFINITIONS, RULES }

    /** A symbol used in the Alphabet, with the canonical archiphoneme if any. */
    public static final class Resolved {
        public final String archiphoneme;  // canonical "{G}"
        public final String surface;       // realisation text, e.g. "г" or "0"
        public final int start;
        public final int end;
        public Resolved(String arch, String surface, int start, int end) {
            this.archiphoneme = arch; this.surface = surface;
            this.start = start; this.end = end;
        }
    }

    /** A named Set or Definition declaration. */
    public static final class Named {
        public final String name;
        public final int start;
        public final int end;
        public Named(String name, int start, int end) {
            this.name = name; this.start = start; this.end = end;
        }
    }

    /** A named rule ("..."). */
    public static final class Rule {
        public final String name;   // without quotes
        public final int start;
        public final int end;
        public Rule(String name, int start, int end) {
            this.name = name; this.start = start; this.end = end;
        }
    }

    public final List<Resolved> resolvedArchiphonemes = new ArrayList<>();
    public final List<Named> sets = new ArrayList<>();
    public final List<Named> definitions = new ArrayList<>();
    public final List<Rule> rules = new ArrayList<>();
    /** Canonical archiphonemes referenced anywhere (e.g. inside Set bodies). */
    public final List<LexcModel.SymbolRef> archiphonemeRefs = new ArrayList<>();

    // --- parsing -----------------------------------------------------------

    public static TwolModel parse(String src) {
        TwolModel m = new TwolModel();
        int len = src.length();
        int pos = 0;
        Section section = Section.NONE;
        boolean atLineStart = true;

        while (pos < len) {
            char c = src.charAt(pos);

            // Comments: '!' to end of line (twol uses '!' like lexc).
            if (c == '!') {
                while (pos < len && src.charAt(pos) != '\n') pos++;
                continue;
            }
            if (c == '\n' || c == '\r') { pos++; atLineStart = true; continue; }
            if (c == ' ' || c == '\t') { pos++; continue; }

            // Section keywords (only meaningful at line start).
            if (atLineStart && Character.isLetter(c)) {
                int s = pos;
                while (pos < len && (Character.isLetterOrDigit(src.charAt(pos))
                        || src.charAt(pos) == '_')) pos++;
                String word = src.substring(s, pos);
                Section sec = sectionFor(word);
                if (sec != Section.NONE) {
                    section = sec;
                    atLineStart = false;
                    continue;
                }
                // Not a section keyword: in Sets/Definitions a leading word is a
                // declaration name if followed (after ws) by '='.
                if (section == Section.SETS || section == Section.DEFINITIONS) {
                    int save = pos;
                    int q = pos;
                    while (q < len && (src.charAt(q) == ' ' || src.charAt(q) == '\t')) q++;
                    if (q < len && src.charAt(q) == '=') {
                        Named n = new Named(word, s, save);
                        if (section == Section.SETS) m.sets.add(n);
                        else m.definitions.add(n);
                    }
                }
                atLineStart = false;
                continue;
            }
            atLineStart = false;

            // Named rule: "..." (only inside Rules, but accept anywhere).
            if (c == '"') {
                int s = pos; pos++;
                StringBuilder sb = new StringBuilder();
                while (pos < len && src.charAt(pos) != '"'
                        && src.charAt(pos) != '\n') {
                    sb.append(src.charAt(pos)); pos++;
                }
                if (pos < len && src.charAt(pos) == '"') pos++;
                m.rules.add(new Rule(sb.toString(), s, pos));
                continue;
            }

            // Archiphoneme symbol %{X%}, possibly with ":surface".
            if (c == '%' && pos + 1 < len && src.charAt(pos + 1) == '{') {
                int s = pos;
                String canon = readBraceSymbol(src, pos);
                pos += rawBraceLen(src, pos);
                m.archiphonemeRefs.add(new LexcModel.SymbolRef(canon, s, pos));
                // optional ":surface"
                if (pos < len && src.charAt(pos) == ':') {
                    int colon = pos; pos++;
                    int surfStart = pos;
                    // surface is a single symbol: %X, a char, or %{...%}
                    String surf = readSurface(src, pos);
                    pos += surf.length();
                    if (section == Section.ALPHABET) {
                        m.resolvedArchiphonemes.add(new Resolved(
                                canon, src.substring(surfStart, pos), s, pos));
                    }
                }
                continue;
            }

            // Any other %-escape: skip the escaped pair.
            if (c == '%' && pos + 1 < len) { pos += 2; continue; }

            pos++;
        }
        return m;
    }

    private static Section sectionFor(String word) {
        switch (word) {
            case "Alphabet": return Section.ALPHABET;
            case "Sets": return Section.SETS;
            case "Definitions": return Section.DEFINITIONS;
            case "Rules": return Section.RULES;
            default: return Section.NONE;
        }
    }

    /** Reads the canonical {@code {X}} from a {@code %{X%}} at {@code p}. */
    private static String readBraceSymbol(String src, int p) {
        int len = src.length();
        StringBuilder sb = new StringBuilder("{");
        int i = p + 2;  // skip %{
        while (i < len) {
            char c = src.charAt(i);
            if (c == '%' && i + 1 < len) {
                char n = src.charAt(i + 1);
                if (n == '}') { sb.append('}'); break; }
                sb.append(n); i += 2;
            } else if (c == '}') { sb.append('}'); break; }
            else if (c == '\n') break;
            else { sb.append(c); i++; }
        }
        return sb.toString();
    }

    /** Raw character length of a {@code %{...%}} token at {@code p}. */
    private static int rawBraceLen(String src, int p) {
        int len = src.length();
        int i = p + 2;
        while (i < len) {
            char c = src.charAt(i);
            if (c == '%' && i + 1 < len) {
                char n = src.charAt(i + 1);
                if (n == '}') { i += 2; break; }
                i += 2;
            } else if (c == '}') { i++; break; }
            else if (c == '\n') break;
            else i++;
        }
        return i - p;
    }

    /** Reads one surface symbol after a ':' (a %-escape, a %{..%}, or one char). */
    private static String readSurface(String src, int p) {
        int len = src.length();
        if (p >= len) return "";
        char c = src.charAt(p);
        if (c == '%' && p + 1 < len) {
            if (src.charAt(p + 1) == '{') {
                return src.substring(p, p + rawBraceLen(src, p));
            }
            return src.substring(p, p + 2);  // %0, %  etc.
        }
        return String.valueOf(c);
    }
}
