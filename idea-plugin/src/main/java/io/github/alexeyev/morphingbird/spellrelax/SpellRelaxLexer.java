package io.github.alexeyev.morphingbird.spellrelax;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

/**
 * A streaming highlighter lexer for {@code .spellrelax}. The format is an HFST
 * regex cascade — rules of the shape {@code [ ?* [ x (->) y ] ?* ] .o.} — so the
 * lexer recognises {@code !} line comments, the replacement/composition
 * operators ({@code (->)}, {@code ->}, {@code <-}, {@code .o.}), grouping
 * brackets, {@code %}-escapes, and otherwise emits bare symbols.
 */
public final class SpellRelaxLexer extends LexerBase {

    private CharSequence buf;
    private int start, end, pos, tokStart, tokEnd;
    private IElementType tokType;

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset,
                      int initialState) {
        this.buf = buffer; this.start = startOffset; this.end = endOffset;
        this.pos = startOffset;
        advance0();
    }

    private char at(int i) { return i < end ? buf.charAt(i) : '\0'; }

    private boolean matches(int at, String s) {
        if (at + s.length() > end) return false;
        for (int i = 0; i < s.length(); i++) {
            if (buf.charAt(at + i) != s.charAt(i)) return false;
        }
        return true;
    }

    private void advance0() {
        tokStart = pos;
        if (pos >= end) { tokType = null; tokEnd = pos; return; }
        char c = at(pos);

        // Whitespace.
        if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
            int p = pos + 1;
            while (p < end) {
                char d = at(p);
                if (d == ' ' || d == '\t' || d == '\n' || d == '\r') p++; else break;
            }
            tokType = SpellRelaxTokenTypes.WHITESPACE; tokEnd = p; pos = p; return;
        }
        // Comment: ! to end of line.
        if (c == '!') {
            int p = pos;
            while (p < end && at(p) != '\n') p++;
            tokType = SpellRelaxTokenTypes.COMMENT; tokEnd = p; pos = p; return;
        }
        // Escape: %x (the next char is literal).
        if (c == '%' && pos + 1 < end) {
            tokType = SpellRelaxTokenTypes.ESCAPE; tokEnd = pos + 2; pos += 2; return;
        }
        // Multi-char operators, longest first.
        for (String op : new String[]{"(->)", "(<-)", ".o.", "->", "<-", "=>"}) {
            if (matches(pos, op)) {
                tokType = SpellRelaxTokenTypes.OPERATOR; tokEnd = pos + op.length();
                pos += op.length(); return;
            }
        }
        // Grouping brackets and parens.
        if (c == '[' || c == ']' || c == '(' || c == ')' || c == '{' || c == '}') {
            tokType = SpellRelaxTokenTypes.BRACKET; tokEnd = pos + 1; pos += 1; return;
        }
        // A run of "plain" symbol characters (anything not special / whitespace).
        int p = pos;
        while (p < end) {
            char d = at(p);
            if (d == ' ' || d == '\t' || d == '\n' || d == '\r'
                    || d == '!' || d == '%' || d == '[' || d == ']'
                    || d == '(' || d == ')' || d == '{' || d == '}') break;
            // stop if an operator starts here
            if (d == '.' && matches(p, ".o.")) break;
            if (d == '-' && (matches(p, "->"))) break;
            if (d == '<' && (matches(p, "<-"))) break;
            p++;
        }
        if (p == pos) p = pos + 1;   // ensure progress
        tokType = SpellRelaxTokenTypes.SYMBOL; tokEnd = p; pos = p;
    }

    @Override public int getState() { return 0; }
    @Override public IElementType getTokenType() { return tokType; }
    @Override public int getTokenStart() { return tokStart; }
    @Override public int getTokenEnd() { return tokEnd; }
    @Override public void advance() { advance0(); }
    @Override public @NotNull CharSequence getBufferSequence() { return buf; }
    @Override public int getBufferEnd() { return end; }
}
