package io.github.alexeyev.morphingbird.cg3;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * A small streaming lexer for CG3 highlighting: keywords (LIST/SET/SELECT/…),
 * set names (Capitalised identifiers), quoted strings (with trailing case
 * flag), bare tag words, {@code #} comments, and other punctuation.
 */
public final class Cg3Lexer extends LexerBase {

    private static final Set<String> KEYWORDS = Set.of(
            "LIST", "SET", "SECTION", "SELECT", "REMOVE", "IFF", "ADD", "MAP",
            "SUBSTITUTE", "APPEND", "DELIMITERS", "SOFT-DELIMITERS", "TEMPLATE",
            "BARRIER", "TARGET", "IF", "TO", "FROM", "OR", "AND", "NOT", "LINK",
            "NEGATE", "BEFORE", "AFTER", "WITHIN", "STATIC", "CONSTRAINTS",
            "MAPPINGS", "CORRECTIONS");

    private CharSequence buf;
    private int start, end, pos, tokStart, tokEnd;
    private IElementType tokType;

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset,
                      int initialState) {
        this.buf = buffer; this.start = startOffset; this.end = endOffset;
        this.pos = startOffset; nextToken();
    }

    private char at(int i) { return i < end ? buf.charAt(i) : '\0'; }

    private void nextToken() {
        tokStart = pos;
        if (pos >= end) { tokType = null; tokEnd = pos; return; }
        char c = at(pos);

        if (c == '#') {
            int p = pos + 1;
            while (p < end && at(p) != '\n') p++;
            tokType = Cg3TokenTypes.COMMENT; tokEnd = p; pos = p; return;
        }
        if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f') {
            int p = pos + 1;
            while (p < end) {
                char d = at(p);
                if (d == ' ' || d == '\t' || d == '\n' || d == '\r' || d == '\f') p++;
                else break;
            }
            tokType = Cg3TokenTypes.WHITESPACE; tokEnd = p; pos = p; return;
        }
        if (c == '"') {
            int p = pos + 1;
            while (p < end && at(p) != '"') {
                if (at(p) == '\\' && p + 1 < end) p++;
                p++;
            }
            if (p < end) p++;                       // closing quote
            // trailing case/regex flag
            while (p < end && (at(p) == 'i' || at(p) == 'r' || at(p) == 'v' || at(p) == 'l')) p++;
            tokType = Cg3TokenTypes.STRING; tokEnd = p; pos = p; return;
        }
        if (c == '<') {
            int p = pos + 1;
            while (p < end && at(p) != '>' && at(p) != '\n') p++;
            if (p < end && at(p) == '>') p++;
            tokType = Cg3TokenTypes.TAG; tokEnd = p; pos = p; return;
        }
        if (Character.isLetter(c) || c == '_' || c == '@') {
            int p = pos;
            while (p < end && (Character.isLetterOrDigit(at(p)) || at(p) == '_'
                    || at(p) == '-' || at(p) == '@' || at(p) == '.')) p++;
            String w = buf.subSequence(pos, p).toString();
            if (KEYWORDS.contains(w)) tokType = Cg3TokenTypes.KEYWORD;
            else if (!w.isEmpty() && Character.isUpperCase(stripPrefix(w)))
                tokType = Cg3TokenTypes.SETNAME;     // Capitalised => set name
            else tokType = Cg3TokenTypes.TAG;        // lowercase => tag
            tokEnd = p; pos = p; return;
        }
        tokType = Cg3TokenTypes.OTHER; tokEnd = pos + 1; pos += 1;
    }

    /** First letter, skipping a leading '@'. */
    private static char stripPrefix(String w) {
        int i = 0;
        while (i < w.length() && (w.charAt(i) == '@')) i++;
        return i < w.length() ? w.charAt(i) : '_';
    }

    @Override public int getState() { return 0; }
    @Override public IElementType getTokenType() { return tokType; }
    @Override public int getTokenStart() { return tokStart; }
    @Override public int getTokenEnd() { return tokEnd; }
    @Override public void advance() { nextToken(); }
    @Override public @NotNull CharSequence getBufferSequence() { return buf; }
    @Override public int getBufferEnd() { return end; }
}
