package io.github.alexeyev.morphingbird.lexc;

import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Nullable;

/**
 * Brace matching for lexc. lexc's structural pairing is light; the most useful
 * pairing is highlighting the start/end of an entry, but since tags and
 * archiphonemes are single tokens (the {@code %< %>} / <code>%{ %}</code> are
 * inside one token), there are no nested bracket tokens to match. We expose an
 * empty pair set rather than inventing pairings that would misfire — matching the
 * plan's "don't invent structure" stance. This class exists so the extension
 * point is wired and can be extended later (e.g. if entries become PSI ranges).
 */
public final class LexcBraceMatcher implements PairedBraceMatcher {

    private static final BracePair[] PAIRS = new BracePair[0];

    @Override
    public BracePair[] getPairs() {
        return PAIRS;
    }

    @Override
    public boolean isPairedBracesAllowedBeforeType(IElementType lbraceType,
                                                   @Nullable IElementType type) {
        return true;
    }

    @Override
    public int getCodeConstructStart(PsiFile file, int openingBraceOffset) {
        return openingBraceOffset;
    }
}
