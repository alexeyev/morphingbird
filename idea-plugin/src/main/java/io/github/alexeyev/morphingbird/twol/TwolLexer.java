package io.github.alexeyev.morphingbird.twol;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

/**
 * A small streaming lexer for twol, sufficient for syntax highlighting. It
 * recognises section keywords, {@code %{X%}} archiphonemes, {@code "..."} rule
 * names, the two-level operators ({@code <=> => <= /<=}), comments ({@code !}),
 * and falls back to identifiers/other. (Deep rule structure is intentionally not
 * modelled — see the plan: twol semantics are out of scope for v1.)
 */
public final class TwolLexer extends LexerBase {

    private CharSequence buf;
    private int start;
    private int end;
    private int pos;
    private int tokStart;
    private int tokEnd;
    private IElementType tokType;

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset,
                      int initialState) {
        this.buf = buffer;
        this.start = startOffset;
        this.end = endOffset;
        this.pos = startOffset;
        nextToken();
    }

    private char at(int i) {
        return i < end ? buf.charAt(i) : '\0';
    }

    private boolean lineStart(int i) {
        // i is at line start if previous non-buffer or previous char is newline
        return i == start || at(i - 1) == '\n' || at(i - 1) == '\r';
    }

    private void nextToken() {
        tokStart = pos;
        if (pos >= end) { tokType = null; tokEnd = pos; return; }
        char c = at(pos);

        // Comment
        if (c == '!') {
            int p = pos + 1;
            while (p < end && at(p) != '\n') p++;
            tokType = TwolTokenTypes.COMMENT; tokEnd = p; pos = p; return;
        }
        // Whitespace
        if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f') {
            int p = pos + 1;
            while (p < end) {
                char d = at(p);
                if (d == ' ' || d == '\t' || d == '\n' || d == '\r' || d == '\f') p++;
                else break;
            }
            tokType = TwolTokenTypes.WHITESPACE; tokEnd = p; pos = p; return;
        }
        // Rule name "..."
        if (c == '"') {
            int p = pos + 1;
            while (p < end && at(p) != '"' && at(p) != '\n') p++;
            if (p < end && at(p) == '"') p++;
            tokType = TwolTokenTypes.RULENAME; tokEnd = p; pos = p; return;
        }
        // Archiphoneme %{X%}
        if (c == '%' && at(pos + 1) == '{') {
            int p = pos + 2;
            while (p < end) {
                char d = at(p);
                if (d == '%' && at(p + 1) == '}') { p += 2; break; }
                if (d == '}') { p++; break; }
                if (d == '\n') break;
                if (d == '%') p += 2; else p++;
            }
            tokType = TwolTokenTypes.ARCHIPHONEME; tokEnd = p; pos = p; return;
        }
        // Other %-escape (e.g. %0, %  )
        if (c == '%' && pos + 1 < end) {
            tokType = TwolTokenTypes.OTHER; tokEnd = pos + 2; pos += 2; return;
        }
        // Two-level operators
        if (c == '<' || c == '=' || c == '/') {
            int p = pos;
            // greedily take a run of operator chars
            while (p < end) {
                char d = at(p);
                if (d == '<' || d == '=' || d == '>' || d == '/') p++;
                else break;
            }
            if (p > pos) {
                tokType = TwolTokenTypes.OPERATOR; tokEnd = p; pos = p; return;
            }
        }
        // Section keyword at line start
        if (lineStart(pos) && Character.isLetter(c)) {
            int p = pos;
            while (p < end && (Character.isLetterOrDigit(at(p)) || at(p) == '_')) p++;
            String w = buf.subSequence(pos, p).toString();
            if (w.equals("Alphabet") || w.equals("Sets") || w.equals("Definitions")
                    || w.equals("Rules")) {
                tokType = TwolTokenTypes.SECTION; tokEnd = p; pos = p; return;
            }
            tokType = TwolTokenTypes.IDENT; tokEnd = p; pos = p; return;
        }
        // Identifier
        if (Character.isLetterOrDigit(c) || c == '_') {
            int p = pos;
            while (p < end && (Character.isLetterOrDigit(at(p)) || at(p) == '_')) p++;
            tokType = TwolTokenTypes.IDENT; tokEnd = p; pos = p; return;
        }
        // Single other char
        tokType = TwolTokenTypes.OTHER; tokEnd = pos + 1; pos += 1;
    }

    @Override public int getState() { return 0; }
    @Override public IElementType getTokenType() { return tokType; }
    @Override public int getTokenStart() { return tokStart; }
    @Override public int getTokenEnd() { return tokEnd; }
    @Override public void advance() { nextToken(); }
    @Override public @NotNull CharSequence getBufferSequence() { return buf; }
    @Override public int getBufferEnd() { return end; }
}
