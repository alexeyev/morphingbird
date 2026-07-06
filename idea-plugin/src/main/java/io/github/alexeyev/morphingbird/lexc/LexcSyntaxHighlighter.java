package io.github.alexeyev.morphingbird.lexc;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

/**
 * Maps lexc token types to editor colors. Tags and archiphonemes get distinct
 * colors because they are the load-bearing symbols of the language; continuation
 * classes (identifiers) are highlighted as identifiers so they read as the
 * "links" they are.
 */
public final class LexcSyntaxHighlighter extends SyntaxHighlighterBase {

    public static final TextAttributesKey COMMENT = createTextAttributesKey(
            "APERTIUM_LEXC_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
    public static final TextAttributesKey KEYWORD = createTextAttributesKey(
            "APERTIUM_LEXC_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey TAG = createTextAttributesKey(
            "APERTIUM_LEXC_TAG", DefaultLanguageHighlighterColors.METADATA);
    public static final TextAttributesKey ARCHIPHONEME = createTextAttributesKey(
            "APERTIUM_LEXC_ARCHIPHONEME", DefaultLanguageHighlighterColors.INSTANCE_FIELD);
    public static final TextAttributesKey IDENTIFIER = createTextAttributesKey(
            "APERTIUM_LEXC_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER);
    public static final TextAttributesKey STRING = createTextAttributesKey(
            "APERTIUM_LEXC_STRING", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey OPERATOR = createTextAttributesKey(
            "APERTIUM_LEXC_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN);
    public static final TextAttributesKey SEMICOLON = createTextAttributesKey(
            "APERTIUM_LEXC_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON);
    public static final TextAttributesKey BAD_CHARACTER = createTextAttributesKey(
            "APERTIUM_LEXC_BAD_CHARACTER",
            com.intellij.openapi.editor.HighlighterColors.BAD_CHARACTER);

    private static final TextAttributesKey[] EMPTY = new TextAttributesKey[0];
    private static final TextAttributesKey[] AS_COMMENT = one(COMMENT);
    private static final TextAttributesKey[] AS_KEYWORD = one(KEYWORD);
    private static final TextAttributesKey[] AS_TAG = one(TAG);
    private static final TextAttributesKey[] AS_ARCH = one(ARCHIPHONEME);
    private static final TextAttributesKey[] AS_IDENT = one(IDENTIFIER);
    private static final TextAttributesKey[] AS_STRING = one(STRING);
    private static final TextAttributesKey[] AS_OPERATOR = one(OPERATOR);
    private static final TextAttributesKey[] AS_SEMI = one(SEMICOLON);

    private static TextAttributesKey[] one(TextAttributesKey k) {
        return new TextAttributesKey[]{k};
    }

    @Override
    public @NotNull Lexer getHighlightingLexer() {
        return new LexcLexer();
    }

    @Override
    public TextAttributesKey @NotNull [] getTokenHighlights(IElementType tokenType) {
        if (tokenType == LexcTokenTypes.COMMENT) return AS_COMMENT;
        if (tokenType == LexcTokenTypes.KW_LEXICON
                || tokenType == LexcTokenTypes.KW_MULTICHAR
                || tokenType == LexcTokenTypes.KW_DEFINITIONS) return AS_KEYWORD;
        if (tokenType == LexcTokenTypes.TAG) return AS_TAG;
        if (tokenType == LexcTokenTypes.ARCHIPHONEME) return AS_ARCH;
        if (tokenType == LexcTokenTypes.IDENTIFIER) return AS_IDENT;
        if (tokenType == LexcTokenTypes.STRING) return AS_STRING;
        if (tokenType == LexcTokenTypes.COLON) return AS_OPERATOR;
        if (tokenType == LexcTokenTypes.SEMICOLON) return AS_SEMI;
        return EMPTY;
    }
}
