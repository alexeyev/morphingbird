package io.github.alexeyev.morphingbird.refs;

import com.intellij.lang.cacheBuilder.DefaultWordsScanner;
import com.intellij.lang.cacheBuilder.WordsScanner;
import com.intellij.lang.findUsages.FindUsagesProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.TokenSet;
import io.github.alexeyev.morphingbird.lexc.LexcLexer;
import io.github.alexeyev.morphingbird.lexc.LexcTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Enables Find Usages for lexc symbols. The {@link WordsScanner} feeds IntelliJ's
 * word index using the tested {@link LexcLexer}: identifiers (LEXICON names /
 * continuation classes), tags and archiphonemes are treated as "code" words, and
 * comments as comment words. With the references from
 * {@link MorphingbirdReferenceContributor} in place, the platform then resolves and
 * lists usages.
 */
public final class MorphingbirdFindUsagesProvider implements FindUsagesProvider {

    @Override
    public @Nullable WordsScanner getWordsScanner() {
        return new DefaultWordsScanner(
                new LexcLexer(),
                /* identifiers */ TokenSet.create(
                        LexcTokenTypes.IDENTIFIER,
                        LexcTokenTypes.TAG,
                        LexcTokenTypes.ARCHIPHONEME),
                /* comments    */ TokenSet.create(LexcTokenTypes.COMMENT),
                /* literals    */ TokenSet.create(LexcTokenTypes.STRING));
    }

    @Override
    public boolean canFindUsagesFor(@NotNull PsiElement element) {
        // We attach references to leaf tokens; allow find-usages on the symbol
        // tokens themselves.
        if (element.getNode() == null) return false;
        var t = element.getNode().getElementType();
        return t == LexcTokenTypes.IDENTIFIER
                || t == LexcTokenTypes.TAG
                || t == LexcTokenTypes.ARCHIPHONEME;
    }

    @Override
    public @Nullable String getHelpId(@NotNull PsiElement element) {
        return null;
    }

    @Override
    public @NotNull String getType(@NotNull PsiElement element) {
        if (element.getNode() == null) return "symbol";
        var t = element.getNode().getElementType();
        if (t == LexcTokenTypes.TAG) return "tag";
        if (t == LexcTokenTypes.ARCHIPHONEME) return "archiphoneme";
        return "lexicon / continuation class";
    }

    @Override
    public @NotNull String getDescriptiveName(@NotNull PsiElement element) {
        String t = element.getText();
        return t == null ? "" : t;
    }

    @Override
    public @NotNull String getNodeText(@NotNull PsiElement element, boolean useFullName) {
        String t = element.getText();
        return t == null ? "" : t;
    }
}
