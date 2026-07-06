package io.github.alexeyev.morphingbird.twol;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import io.github.alexeyev.morphingbird.twol.psi.TwolFile;
import org.jetbrains.annotations.NotNull;

public final class TwolParserDefinition implements ParserDefinition {
    public static final IFileElementType FILE = new IFileElementType(TwolLanguage.INSTANCE);
    private static final TokenSet COMMENTS = TokenSet.create(TwolTokenTypes.COMMENT);
    private static final TokenSet WS = TokenSet.create(TwolTokenTypes.WHITESPACE, TokenType.WHITE_SPACE);

    @Override public @NotNull Lexer createLexer(Project p) { return new TwolLexer(); }
    @Override public @NotNull PsiParser createParser(Project p) {
        return (root, builder) -> {
            var m = builder.mark();
            while (!builder.eof()) builder.advanceLexer();
            m.done(root);
            return builder.getTreeBuilt();
        };
    }
    @Override public @NotNull IFileElementType getFileNodeType() { return FILE; }
    @Override public @NotNull TokenSet getCommentTokens() { return COMMENTS; }
    @Override public @NotNull TokenSet getWhitespaceTokens() { return WS; }
    @Override public @NotNull TokenSet getStringLiteralElements() { return TokenSet.EMPTY; }
    @Override public @NotNull PsiElement createElement(ASTNode n) {
        return new com.intellij.extapi.psi.ASTWrapperPsiElement(n);
    }
    @Override public @NotNull PsiFile createFile(@NotNull FileViewProvider vp) { return new TwolFile(vp); }
}
