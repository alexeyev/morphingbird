package io.github.alexeyev.morphingbird.udx;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

/**
 * A column-aware lexer for {@code .udx}. The file is tab-separated; the lexer
 * tracks the current column so it can colour the Apertium tags (columns 2–3,
 * which link into the symbol graph) differently from the Universal Dependencies
 * output (POS in column 6, features in column 7) and the {@code _} wildcards.
 *
 * <p>Column index resets to 0 at each newline and increments on each tab.</p>
 */
public final class UdxLexer extends LexerBase {

    private CharSequence buf;
    private int start, end, pos, tokStart, tokEnd;
    private IElementType tokType;
    private int column;   // 0-based column index on the current line

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset,
                      int initialState) {
        this.buf = buffer; this.start = startOffset; this.end = endOffset;
        this.pos = startOffset; this.column = initialState;
        nextToken();
    }

    private char at(int i) { return i < end ? buf.charAt(i) : '\0'; }

    private void nextToken() {
        tokStart = pos;
        if (pos >= end) { tokType = null; tokEnd = pos; return; }
        char c = at(pos);

        // Newline(s): whitespace token, reset column.
        if (c == '\n' || c == '\r') {
            int p = pos + 1;
            while (p < end && (at(p) == '\n' || at(p) == '\r')) p++;
            tokType = UdxTokenTypes.WHITESPACE; tokEnd = p; pos = p; column = 0;
            return;
        }
        // Tab: advances the column.
        if (c == '\t') {
            int p = pos + 1;
            while (p < end && at(p) == '\t') { p++; column++; }
            column++;  // for the first tab
            tokType = UdxTokenTypes.WHITESPACE; tokEnd = p; pos = p;
            return;
        }
        if (c == ' ') {
            int p = pos + 1;
            while (p < end && at(p) == ' ') p++;
            tokType = UdxTokenTypes.WHITESPACE; tokEnd = p; pos = p;
            return;
        }
        // Comment line (# at line start, column 0, first char).
        if (c == '#' && column == 0 && atLineStart(pos)) {
            int p = pos;
            while (p < end && at(p) != '\n') p++;
            tokType = UdxTokenTypes.COMMENT; tokEnd = p; pos = p;
            return;
        }
        // Pipe separates a tag combination.
        if (c == '|') {
            tokType = UdxTokenTypes.PIPE; tokEnd = pos + 1; pos += 1;
            return;
        }
        // A field value: run up to tab / pipe / newline.
        int p = pos;
        while (p < end) {
            char d = at(p);
            if (d == '\t' || d == '\n' || d == '\r' || d == '|') break;
            p++;
        }
        String text = buf.subSequence(pos, p).toString().trim();
        tokType = classify(text);
        tokEnd = p; pos = p;
    }

    private boolean atLineStart(int i) {
        return i == start || at(i - 1) == '\n' || at(i - 1) == '\r';
    }

    /** Classifies a field value by column and shape. */
    private IElementType classify(String text) {
        if (text.equals("_") || text.isEmpty()) return UdxTokenTypes.WILDCARD;
        // Apertium tags live in columns 2 and 3 (0-based 1 and 2).
        if (column == 1 || column == 2) {
            return looksLikeTag(text) ? UdxTokenTypes.TAG : UdxTokenTypes.OTHER;
        }
        // UD POS column (0-based 5).
        if (column == 5) return UdxTokenTypes.UPOS;
        // UD features column (0-based 6).
        if (column == 6) return UdxTokenTypes.UFEAT;
        return UdxTokenTypes.OTHER;
    }

    private static boolean looksLikeTag(String w) {
        boolean hasLetter = false;
        for (int i = 0; i < w.length(); i++) {
            char ch = w.charAt(i);
            if (Character.isUpperCase(ch)) return false;
            if (Character.isLetter(ch)) hasLetter = true;
            else if (!(Character.isDigit(ch) || ch == '-' || ch == '_')) return false;
        }
        return hasLetter;
    }

    @Override public int getState() {
        // Encode the current column so IntelliJ can restart lexing mid-document
        // without losing column context (clamped to a small range).
        return Math.min(column, 15);
    }
    @Override public IElementType getTokenType() { return tokType; }
    @Override public int getTokenStart() { return tokStart; }
    @Override public int getTokenEnd() { return tokEnd; }
    @Override public void advance() { nextToken(); }
    @Override public @NotNull CharSequence getBufferSequence() { return buf; }
    @Override public int getBufferEnd() { return end; }
}
