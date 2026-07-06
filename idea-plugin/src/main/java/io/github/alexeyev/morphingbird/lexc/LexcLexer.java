package io.github.alexeyev.morphingbird.lexc;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.tree.IElementType;
import io.github.alexeyev.morphingbird.core.LexcScanner;
import io.github.alexeyev.morphingbird.core.LexcToken;
import org.jetbrains.annotations.NotNull;

/**
 * An IntelliJ {@link LexerBase} that delegates to the tested, IntelliJ-free
 * {@link LexcScanner}. The scanner does the hard {@code %}-escape work; this
 * adapter just walks its token stream and exposes IntelliJ element types.
 *
 * <p>The lexer tokenises the buffer once on {@link #start} (the scanner is
 * fast — ~114 ms for the 626 KB kir lexc, and IntelliJ lexes incrementally by
 * region in practice) and then iterates. Offsets from the scanner are absolute
 * within the {@code [startOffset, endOffset)} window.</p>
 */
public final class LexcLexer extends LexerBase {

    private CharSequence buffer;
    private int startOffset;
    private int endOffset;

    private java.util.List<LexcToken> tokens;
    private int index;       // index into tokens
    private int tokenStart;  // current token start (absolute)
    private int tokenEnd;    // current token end (absolute)

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset,
                      int initialState) {
        this.buffer = buffer;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        String text = buffer.subSequence(startOffset, endOffset).toString();
        this.tokens = LexcScanner.tokenize(text);
        this.index = 0;
        syncToken();
    }

    private void syncToken() {
        if (tokens != null && index < tokens.size()) {
            LexcToken t = tokens.get(index);
            // Scanner offsets are relative to the sliced text; shift to absolute.
            tokenStart = startOffset + t.start;
            tokenEnd = startOffset + t.end;
        } else {
            tokenStart = endOffset;
            tokenEnd = endOffset;
        }
    }

    @Override
    public int getState() {
        return 0;  // the scanner is effectively stateless across tokens
    }

    @Override
    public IElementType getTokenType() {
        if (tokens == null || index >= tokens.size()) return null;
        LexcToken t = tokens.get(index);
        if (t.kind == LexcToken.Kind.EOF) return null;
        return LexcTokenTypes.of(t.kind);
    }

    @Override
    public int getTokenStart() {
        return tokenStart;
    }

    @Override
    public int getTokenEnd() {
        return tokenEnd;
    }

    @Override
    public void advance() {
        if (tokens != null && index < tokens.size()) {
            index++;
            syncToken();
        }
    }

    @Override
    public @NotNull CharSequence getBufferSequence() {
        return buffer;
    }

    @Override
    public int getBufferEnd() {
        return endOffset;
    }
}
