package io.github.alexeyev.morphingbird.lexc;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import io.github.alexeyev.morphingbird.lexc.psi.LexcFile;
import org.jetbrains.annotations.NotNull;

/**
 * A minimal {@link ParserDefinition} for lexc. The plugin's cross-file
 * intelligence lives in {@link io.github.alexeyev.morphingbird.index.MorphingbirdIndexService}
 * (backed by the tested core indexer), so the PSI here is intentionally light:
 * a flat tree of lexer tokens is enough to host highlighting, comment/brace
 * handling, the structure view, and annotators. This mirrors the plan's
 * "lenient, never red" stance — the parser builds structure and never reports
 * syntax errors.
 */
public final class LexcParserDefinition implements ParserDefinition {

    public static final IFileElementType FILE =
            new IFileElementType(LexcLanguage.INSTANCE);

    private static final TokenSet COMMENTS =
            TokenSet.create(LexcTokenTypes.COMMENT);
    private static final TokenSet WHITESPACE =
            TokenSet.create(LexcTokenTypes.WHITESPACE, TokenType.WHITE_SPACE);
    private static final TokenSet STRINGS =
            TokenSet.create(LexcTokenTypes.STRING);

    @Override
    public @NotNull Lexer createLexer(Project project) {
        return new LexcLexer();
    }

    @Override
    public @NotNull PsiParser createParser(Project project) {
        // A flat parser: consume every token under the file root. We deliberately
        // do not impose structure or report errors (see class doc).
        return (root, builder) -> {
            var marker = builder.mark();
            while (!builder.eof()) {
                builder.advanceLexer();
            }
            marker.done(root);
            return builder.getTreeBuilt();
        };
    }

    @Override
    public @NotNull IFileElementType getFileNodeType() {
        return FILE;
    }

    @Override
    public @NotNull TokenSet getCommentTokens() {
        return COMMENTS;
    }

    @Override
    public @NotNull TokenSet getWhitespaceTokens() {
        return WHITESPACE;
    }

    @Override
    public @NotNull TokenSet getStringLiteralElements() {
        return STRINGS;
    }

    @Override
    public @NotNull PsiElement createElement(ASTNode node) {
        return new com.intellij.extapi.psi.ASTWrapperPsiElement(node);
    }

    @Override
    public @NotNull PsiFile createFile(@NotNull FileViewProvider viewProvider) {
        return new LexcFile(viewProvider);
    }
}
