package io.github.alexeyev.morphingbird.udx;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public final class UdxSyntaxHighlighter extends SyntaxHighlighterBase {
    public static final TextAttributesKey COMMENT = createTextAttributesKey(
            "APERTIUM_UDX_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
    public static final TextAttributesKey TAG = createTextAttributesKey(
            "APERTIUM_UDX_TAG", DefaultLanguageHighlighterColors.METADATA);
    public static final TextAttributesKey UPOS = createTextAttributesKey(
            "APERTIUM_UDX_UPOS", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey UFEAT = createTextAttributesKey(
            "APERTIUM_UDX_UFEAT", DefaultLanguageHighlighterColors.INSTANCE_FIELD);
    public static final TextAttributesKey WILDCARD = createTextAttributesKey(
            "APERTIUM_UDX_WILDCARD", DefaultLanguageHighlighterColors.LINE_COMMENT);

    private static TextAttributesKey[] k(TextAttributesKey x){return new TextAttributesKey[]{x};}
    private static final TextAttributesKey[] EMPTY = new TextAttributesKey[0];

    @Override public @NotNull Lexer getHighlightingLexer() { return new UdxLexer(); }
    @Override public TextAttributesKey @NotNull [] getTokenHighlights(IElementType t) {
        if (t == UdxTokenTypes.COMMENT) return k(COMMENT);
        if (t == UdxTokenTypes.TAG) return k(TAG);
        if (t == UdxTokenTypes.UPOS) return k(UPOS);
        if (t == UdxTokenTypes.UFEAT) return k(UFEAT);
        if (t == UdxTokenTypes.WILDCARD) return k(WILDCARD);
        return EMPTY;
    }
}
