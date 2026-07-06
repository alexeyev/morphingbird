package io.github.alexeyev.morphingbird.lexd;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public final class LexdSyntaxHighlighter extends SyntaxHighlighterBase {
    public static final TextAttributesKey COMMENT = createTextAttributesKey(
            "MORPHINGBIRD_LEXD_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
    public static final TextAttributesKey KEYWORD = createTextAttributesKey(
            "MORPHINGBIRD_LEXD_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey TAG = createTextAttributesKey(
            "MORPHINGBIRD_LEXD_TAG", DefaultLanguageHighlighterColors.METADATA);
    public static final TextAttributesKey ARCHIPHONEME = createTextAttributesKey(
            "MORPHINGBIRD_LEXD_ARCHIPHONEME", DefaultLanguageHighlighterColors.INSTANCE_FIELD);
    public static final TextAttributesKey SIEVE = createTextAttributesKey(
            "MORPHINGBIRD_LEXD_SIEVE", DefaultLanguageHighlighterColors.NUMBER);

    private static TextAttributesKey[] k(TextAttributesKey x){return new TextAttributesKey[]{x};}
    private static final TextAttributesKey[] EMPTY = new TextAttributesKey[0];

    @Override public @NotNull Lexer getHighlightingLexer() { return new LexdLexer(); }
    @Override public TextAttributesKey @NotNull [] getTokenHighlights(IElementType t) {
        if (t == LexdTokenTypes.COMMENT) return k(COMMENT);
        if (t == LexdTokenTypes.KEYWORD) return k(KEYWORD);
        if (t == LexdTokenTypes.TAG) return k(TAG);
        if (t == LexdTokenTypes.ARCHIPHONEME) return k(ARCHIPHONEME);
        if (t == LexdTokenTypes.SIEVE) return k(SIEVE);
        return EMPTY;
    }
}
