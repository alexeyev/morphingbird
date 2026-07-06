package io.github.alexeyev.morphingbird.refs;

import com.intellij.lang.cacheBuilder.DefaultWordsScanner;
import com.intellij.lang.cacheBuilder.WordsScanner;
import com.intellij.lang.findUsages.FindUsagesProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.TokenSet;
import io.github.alexeyev.morphingbird.cg3.Cg3Lexer;
import io.github.alexeyev.morphingbird.cg3.Cg3TokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Find Usages for CG3: tags and LIST/SET names are searchable words. */
public final class Cg3FindUsagesProvider implements FindUsagesProvider {

    @Override
    public @Nullable WordsScanner getWordsScanner() {
        return new DefaultWordsScanner(
                new Cg3Lexer(),
                TokenSet.create(Cg3TokenTypes.TAG, Cg3TokenTypes.SETNAME),
                TokenSet.create(Cg3TokenTypes.COMMENT),
                TokenSet.create(Cg3TokenTypes.STRING));
    }

    @Override
    public boolean canFindUsagesFor(@NotNull PsiElement element) {
        if (element.getNode() == null) return false;
        var t = element.getNode().getElementType();
        return t == Cg3TokenTypes.TAG || t == Cg3TokenTypes.SETNAME;
    }

    @Override public @Nullable String getHelpId(@NotNull PsiElement element) { return null; }

    @Override public @NotNull String getType(@NotNull PsiElement element) {
        if (element.getNode() != null
                && element.getNode().getElementType() == Cg3TokenTypes.SETNAME) {
            return "list / set";
        }
        return "tag";
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
