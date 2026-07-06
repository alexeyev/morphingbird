package io.github.alexeyev.morphingbird.refs;

import com.intellij.lang.cacheBuilder.DefaultWordsScanner;
import com.intellij.lang.cacheBuilder.WordsScanner;
import com.intellij.lang.findUsages.FindUsagesProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.TokenSet;
import io.github.alexeyev.morphingbird.twol.TwolLexer;
import io.github.alexeyev.morphingbird.twol.TwolTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Find Usages for twol: archiphonemes and Set names are searchable words. */
public final class TwolFindUsagesProvider implements FindUsagesProvider {

    @Override
    public @Nullable WordsScanner getWordsScanner() {
        return new DefaultWordsScanner(
                new TwolLexer(),
                TokenSet.create(TwolTokenTypes.ARCHIPHONEME, TwolTokenTypes.IDENT),
                TokenSet.create(TwolTokenTypes.COMMENT),
                TokenSet.create(TwolTokenTypes.RULENAME));
    }

    @Override
    public boolean canFindUsagesFor(@NotNull PsiElement element) {
        if (element.getNode() == null) return false;
        var t = element.getNode().getElementType();
        return t == TwolTokenTypes.ARCHIPHONEME || t == TwolTokenTypes.IDENT;
    }

    @Override public @Nullable String getHelpId(@NotNull PsiElement element) { return null; }

    @Override public @NotNull String getType(@NotNull PsiElement element) {
        if (element.getNode() != null
                && element.getNode().getElementType() == TwolTokenTypes.ARCHIPHONEME) {
            return "archiphoneme";
        }
        return "set / definition";
    }

    @Override public @NotNull String getDescriptiveName(@NotNull PsiElement element) {
        String t = element.getText();
        return t == null ? "" : t;
    }

    @Override public @NotNull String getNodeText(@NotNull PsiElement element, boolean useFullName) {
        String t = element.getText();
        return t == null ? "" : t;
    }
}
