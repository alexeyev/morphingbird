package io.github.alexeyev.morphingbird.cg3;

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
import io.github.alexeyev.morphingbird.cg3.psi.Cg3File;
import org.jetbrains.annotations.NotNull;

public final class Cg3ParserDefinition implements ParserDefinition {
    public static final IFileElementType FILE = new IFileElementType(Cg3Language.INSTANCE);
    private static final TokenSet COMMENTS = TokenSet.create(Cg3TokenTypes.COMMENT);
    private static final TokenSet WS = TokenSet.create(Cg3TokenTypes.WHITESPACE, TokenType.WHITE_SPACE);
    private static final TokenSet STR = TokenSet.create(Cg3TokenTypes.STRING);

    @Override public @NotNull Lexer createLexer(Project p) { return new Cg3Lexer(); }
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
    @Override public @NotNull TokenSet getStringLiteralElements() { return STR; }
    @Override public @NotNull PsiElement createElement(ASTNode n) {
        return new com.intellij.extapi.psi.ASTWrapperPsiElement(n);
    }
    @Override public @NotNull PsiFile createFile(@NotNull FileViewProvider vp) { return new Cg3File(vp); }
}
