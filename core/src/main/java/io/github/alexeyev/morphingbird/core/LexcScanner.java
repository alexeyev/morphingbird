package io.github.alexeyev.morphingbird.core;

import java.util.ArrayList;
import java.util.List;

/**
 * A hand-written, IntelliJ-free scanner for apertium lexc source.
 *
 * <p>The single hardest detail in lexc tokenisation is the {@code %} escape
 * character. In lexc, {@code %} makes the <em>next</em> character literal:</p>
 * <ul>
 *   <li>{@code %<n%>} is the multichar symbol (tag) {@code <n>}.</li>
 *   <li>{@code %{A%}} is the archiphoneme {@code {A}}.</li>
 *   <li>{@code %&nbsp;} (percent-space) is a literal space inside a form.</li>
 *   <li>{@code %!} is a literal {@code !}, NOT the start of a comment.</li>
 *   <li>{@code %>} is a literal {@code >} (a morpheme boundary marker).</li>
 * </ul>
 *
 * <p>Getting this wrong mis-tokenises essentially every interesting lexc line,
 * and — more importantly — every cross-file symbol comparison depends on the
 * <em>canonical</em> (escape-stripped) symbol value. So the scanner computes
 * both the raw text and the canonical value for TAG/ARCHIPHONEME tokens.</p>
 *
 * <p>The scanner is intentionally lenient: it never throws on malformed input.
 * An unterminated {@code %<} simply yields whatever was read; the parser and
 * annotators decide whether that is an error.</p>
 */
public final class LexcScanner {

    private final String src;
    private final int len;
    private int pos;

    public LexcScanner(String source) {
        this.src = source;
        this.len = source.length();
        this.pos = 0;
    }

    /** Tokenises the entire input, ending with a single EOF token. */
    public static List<LexcToken> tokenize(String source) {
        LexcScanner s = new LexcScanner(source);
        List<LexcToken> out = new ArrayList<>();
        LexcToken t;
        do {
            t = s.next();
            out.add(t);
        } while (t.kind != LexcToken.Kind.EOF);
        return out;
    }

    private char peek() {
        return pos < len ? src.charAt(pos) : '\0';
    }

    private char peek(int ahead) {
        int i = pos + ahead;
        return i < len ? src.charAt(i) : '\0';
    }

    /** Produces the next token. Returns an EOF token at end of input. */
    public LexcToken next() {
        if (pos >= len) {
            return tok(LexcToken.Kind.EOF, pos, pos, "");
        }
        char c = peek();

        if (isInlineWs(c) || c == '\n' || c == '\r') {
            return whitespace();
        }
        if (c == '!') {
            return comment();
        }
        if (c == ':') {
            int s = pos++;
            return tok(LexcToken.Kind.COLON, s, pos, ":");
        }
        if (c == ';') {
            int s = pos++;
            return tok(LexcToken.Kind.SEMICOLON, s, pos, ";");
        }
        if (c == '"') {
            return string();
        }
        // A multichar symbol written with escapes: %< ... %> or %{ ... %}
        if (c == '%' && (peek(1) == '<' || peek(1) == '{')) {
            return multichar();
        }
        // Any other %-escape (e.g. %> morpheme boundary, %  literal space,
        // %! literal bang) begins/continues a form WORD.
        if (c == '%' && peek(1) != '\0') {
            return word();
        }
        // Identifier / keyword / continuation class / stem word.
        if (isWordStart(c)) {
            return word();
        }
        // Anything else: single OTHER char (brackets, stray punctuation...).
        int s = pos++;
        return tok(LexcToken.Kind.OTHER, s, pos, String.valueOf(c));
    }

    private LexcToken whitespace() {
        int s = pos;
        while (pos < len) {
            char c = peek();
            if (isInlineWs(c) || c == '\n' || c == '\r') {
                pos++;
            } else {
                break;
            }
        }
        return tok(LexcToken.Kind.WHITESPACE, s, pos, src.substring(s, pos));
    }

    /** A {@code "..."} quoted string (weight annotations etc.); lenient on EOL. */
    private LexcToken string() {
        int s = pos;
        pos++;                         // opening quote
        while (pos < len) {
            char c = peek();
            if (c == '\\' && pos + 1 < len) {  // escaped char inside string
                pos += 2;
                continue;
            }
            if (c == '"') {
                pos++;
                break;
            }
            if (c == '\n' || c == '\r') break;  // never span a line
            pos++;
        }
        return tok(LexcToken.Kind.STRING, s, pos, src.substring(s, pos));
    }

    /** A {@code !} comment runs to end of line (the {@code !} is unescaped here). */
    private LexcToken comment() {
        int s = pos;
        while (pos < len && peek() != '\n' && peek() != '\r') {
            pos++;
        }
        return tok(LexcToken.Kind.COMMENT, s, pos, src.substring(s, pos));
    }

    /**
     * Scans a {@code %<...%>} or {@code %{...%}} multichar symbol, producing the
     * canonical escape-stripped value. We are positioned on the leading
     * {@code %}; {@code peek(1)} is {@code <} or <code>{</code>.
     */
    private LexcToken multichar() {
        int s = pos;
        char open = peek(1);           // '<' or '{'
        char close = open == '<' ? '>' : '}';
        StringBuilder canon = new StringBuilder();

        pos += 2;                      // consume "%<" or "%{"
        canon.append(open);

        boolean closed = false;
        while (pos < len) {
            char c = peek();
            if (c == '%') {
                char n = peek(1);
                if (n == '\0') {       // dangling '%' at EOF
                    pos++;
                    break;
                }
                // Escaped char: the next char is literal.
                if (n == close) {
                    pos += 2;
                    canon.append(close);
                    closed = true;
                    break;             // %> / %} closes the symbol
                }
                canon.append(n);
                pos += 2;
            } else if (c == '\n' || c == '\r') {
                break;                 // never let a symbol span a line
            } else {
                // An *unescaped* close also terminates (defensive; lexc usually
                // escapes it, but real files vary).
                if (c == close) {
                    pos++;
                    canon.append(close);
                    closed = true;
                    break;
                }
                canon.append(c);
                pos++;
            }
        }

        String raw = src.substring(s, pos);
        LexcToken.Kind kind = open == '<'
                ? LexcToken.Kind.TAG
                : LexcToken.Kind.ARCHIPHONEME;
        // If we never closed, still return a best-effort token (lenient).
        String canonical = closed ? canon.toString() : raw;
        return new LexcToken(kind, s, pos, raw, canonical);
    }

    /**
     * Scans a bare word: a LEXICON name, a keyword, a continuation-class
     * reference, or a stem. A word may contain {@code %}-escapes (e.g. a stem
     * containing {@code %&nbsp;} for a literal space, or {@code %>} for a
     * boundary), embedded {@code %<tag%>} fragments, and ordinary letters. We
     * keep the whole run as one WORD/IDENTIFIER token and let the parser split
     * roles; embedded tags inside a form are surfaced separately by the parser
     * via {@link #scanInlineSymbols(String)} when needed.
     */
    private LexcToken word() {
        int s = pos;
        while (pos < len) {
            char c = peek();
            if (c == '%') {
                char n = peek(1);
                if (n == '\0') {
                    pos++;
                    break;
                }
                // Stop before a multichar symbol start: %< or %{ must become
                // its own TAG / ARCHIPHONEME token, even mid-form.
                if (n == '<' || n == '{') {
                    break;
                }
                pos += 2;              // any other escaped char is part of word
                continue;
            }
            if (isWordChar(c)) {
                pos++;
            } else {
                break;
            }
        }
        // A word might be empty only if we immediately hit a %<; guard against
        // a zero-length token (caller guarantees at least one consumable char).
        if (pos == s) {
            // Defensive: emit the single char as OTHER to guarantee progress.
            char c = peek();
            pos++;
            return tok(LexcToken.Kind.OTHER, s, pos, String.valueOf(c));
        }
        String raw = src.substring(s, pos);
        // Keyword recognition (exact, case-sensitive — lexc keywords are fixed).
        LexcToken.Kind kind;
        switch (raw) {
            case "Multichar_Symbols": kind = LexcToken.Kind.KW_MULTICHAR; break;
            case "LEXICON":           kind = LexcToken.Kind.KW_LEXICON; break;
            case "Definitions":       kind = LexcToken.Kind.KW_DEFINITIONS; break;
            default:                  kind = LexcToken.Kind.IDENTIFIER;
        }
        return tok(kind, s, pos, raw);
    }

    // --- character classes -------------------------------------------------

    private static boolean isInlineWs(char c) {
        return c == ' ' || c == '\t' || c == '\f' || c == 0x0B;
    }

    /** A word may start with any letter/digit/underscore or escaped char. */
    private static boolean isWordStart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '@' || c == '#'
                || c == '-' || c == '+' || c == '<' || c == '{';
        // Note: a bare '<' or '{' (unescaped) can begin an inline symbol inside
        // a form; we still absorb it into the WORD and let the parser inspect.
    }

    /**
     * Characters that continue a word/form. Stops at whitespace, ':' ';' '!'
     * and the EOF. Everything else (letters, digits, punctuation used in stems)
     * is absorbed; '%' escaping is handled in {@link #word()}.
     */
    private static boolean isWordChar(char c) {
        if (isInlineWs(c) || c == '\n' || c == '\r') return false;
        if (c == ':' || c == ';' || c == '!') return false;
        return true;
    }

    private LexcToken tok(LexcToken.Kind k, int s, int e, String text) {
        return new LexcToken(k, s, e, text, text);
    }
}
