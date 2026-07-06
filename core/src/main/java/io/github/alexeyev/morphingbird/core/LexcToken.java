package io.github.alexeyev.morphingbird.core;

/**
 * A single lexical token produced by {@link LexcScanner}.
 *
 * <p>The scanner is deliberately IntelliJ-free so it can be unit-tested as a
 * plain library and later adapted to an IntelliJ {@code LexerBase}. Tokens
 * carry absolute source offsets ({@code start} inclusive, {@code end}
 * exclusive) so callers can map them back into a document.</p>
 */
public final class LexcToken {

    /** Token categories for lexc (Xerox/HFST lexc as used by apertium). */
    public enum Kind {
        /** Whitespace (spaces, tabs, newlines) outside other tokens. */
        WHITESPACE,
        /** A {@code !}-to-end-of-line comment. */
        COMMENT,
        /** The {@code Multichar_Symbols} section keyword. */
        KW_MULTICHAR,
        /** The {@code LEXICON} section keyword. */
        KW_LEXICON,
        /** The {@code Definitions} section keyword (rarely used, accepted). */
        KW_DEFINITIONS,
        /**
         * A multichar symbol token of the {@code %<...%>} form, e.g.
         * {@code %<nom%>}. The canonical value (see {@link #canonical}) strips
         * the {@code %} escapes, yielding {@code <nom>}.
         */
        TAG,
        /**
         * A multichar symbol of the {@code %{...%}} form, e.g. {@code %{A%}},
         * an archiphoneme. Canonical value is {@code {A}}.
         */
        ARCHIPHONEME,
        /**
         * A bare identifier: a LEXICON name in a header, a continuation-class
         * reference at the end of an entry, or a stem token. Disambiguation of
         * these roles is the parser's job, not the scanner's.
         */
        IDENTIFIER,
        /** The {@code :} separating upper:lower sides of an entry. */
        COLON,
        /** The {@code ;} terminating an entry. */
        SEMICOLON,
        /** A run of word characters/symbols that is part of a form (stem). */
        WORD,
        /** A double-quoted string, e.g. a {@code "weight: 1.0"} annotation. */
        STRING,
        /** Any single character that did not fit another category. */
        OTHER,
        /** End of input. */
        EOF
    }

    public final Kind kind;
    public final int start;
    public final int end;
    /** The raw source text of the token. */
    public final String text;
    /**
     * For TAG / ARCHIPHONEME, the escape-stripped canonical symbol
     * (e.g. {@code <nom>}, {@code {A}}); otherwise equal to {@link #text}.
     */
    public final String canonical;

    public LexcToken(Kind kind, int start, int end, String text, String canonical) {
        this.kind = kind;
        this.start = start;
        this.end = end;
        this.text = text;
        this.canonical = canonical;
    }

    public int length() {
        return end - start;
    }

    @Override
    public String toString() {
        return kind + "[" + start + "," + end + ")"
                + (canonical.equals(text) ? " '" + text + "'"
                                          : " '" + text + "'=>'" + canonical + "'");
    }
}
