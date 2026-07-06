package io.github.alexeyev.morphingbird.udx;

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
import io.github.alexeyev.morphingbird.udx.psi.UdxFile;
import org.jetbrains.annotations.NotNull;

public final class UdxParserDefinition implements ParserDefinition {
    public static final IFileElementType FILE = new IFileElementType(UdxLanguage.INSTANCE);
    private static final TokenSet COMMENTS = TokenSet.create(UdxTokenTypes.COMMENT);
    private static final TokenSet WS = TokenSet.create(UdxTokenTypes.WHITESPACE, TokenType.WHITE_SPACE);

    @Override public @NotNull Lexer createLexer(Project p) { return new UdxLexer(); }
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
    @Override public @NotNull PsiFile createFile(@NotNull FileViewProvider vp) { return new UdxFile(vp); }
}
