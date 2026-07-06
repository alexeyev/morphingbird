package io.github.alexeyev.morphingbird.lexd;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

/**
 * A streaming highlighter lexer for lexd. It recognises comments ({@code #}…),
 * the section keywords ({@code PATTERNS}/{@code PATTERN}/{@code LEXICON}), tags
 * ({@code <np>}), archiphonemes ({@code {Z}}), morphophonological sieves
 * ({@code [l,impf,0cm]}), and bare identifiers (lexicon/pattern names).
 */
public final class LexdLexer extends LexerBase {

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
            tokType = LexdTokenTypes.WHITESPACE; tokEnd = p; pos = p; return;
        }
        // Comment to end of line.
        if (c == '#') {
            int p = pos;
            while (p < end && at(p) != '\n') p++;
            tokType = LexdTokenTypes.COMMENT; tokEnd = p; pos = p; return;
        }
        // Tag <...>.
        if (c == '<') {
            int close = indexOf('>', pos + 1);
            if (close >= 0) {
                tokType = LexdTokenTypes.TAG; tokEnd = close + 1; pos = close + 1; return;
            }
        }
        // Archiphoneme {...}.
        if (c == '{') {
            int close = indexOf('}', pos + 1);
            if (close >= 0) {
                tokType = LexdTokenTypes.ARCHIPHONEME; tokEnd = close + 1; pos = close + 1; return;
            }
        }
        // Sieve [...]. But a bracket that contains a tag (e.g. [<n>:]) is an
        // inline entry, not a morphophonological sieve — only treat as a SIEVE
        // when the bracket body has no '<' (so the inner tag can be lexed).
        if (c == '[') {
            int close = indexOf(']', pos + 1);
            if (close >= 0) {
                boolean hasTag = false;
                for (int i = pos + 1; i < close; i++) {
                    if (at(i) == '<') { hasTag = true; break; }
                }
                if (!hasTag) {
                    tokType = LexdTokenTypes.SIEVE; tokEnd = close + 1; pos = close + 1; return;
                }
                // else: emit just the '[' and let the body lex normally
                tokType = LexdTokenTypes.OTHER; tokEnd = pos + 1; pos = pos + 1; return;
            }
        }
        // Identifier / keyword.
        if (Character.isLetter(c)) {
            int p = pos;
            while (p < end && (Character.isLetterOrDigit(at(p))
                    || at(p) == '-' || at(p) == '_')) p++;
            String word = buf.subSequence(pos, p).toString();
            tokType = (word.equals("PATTERNS") || word.equals("PATTERN")
                    || word.equals("LEXICON"))
                    ? LexdTokenTypes.KEYWORD : LexdTokenTypes.IDENT;
            tokEnd = p; pos = p; return;
        }
        // Anything else: single char.
        tokType = LexdTokenTypes.OTHER; tokEnd = pos + 1; pos = pos + 1;
    }

    private int indexOf(char ch, int from) {
        for (int i = from; i < end; i++) {
            if (at(i) == ch) return i;
            if (at(i) == '\n') return -1;   // don't cross lines
        }
        return -1;
    }

    @Override public int getState() { return 0; }
    @Override public IElementType getTokenType() { return tokType; }
    @Override public int getTokenStart() { return tokStart; }
    @Override public int getTokenEnd() { return tokEnd; }
    @Override public void advance() { advance0(); }
    @Override public @NotNull CharSequence getBufferSequence() { return buf; }
    @Override public int getBufferEnd() { return end; }
}
