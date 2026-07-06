package io.github.alexeyev.morphingbird.spellrelax;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public final class SpellRelaxSyntaxHighlighter extends SyntaxHighlighterBase {
    public static final TextAttributesKey COMMENT = createTextAttributesKey(
            "MORPHINGBIRD_SPELLRELAX_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
    public static final TextAttributesKey OPERATOR = createTextAttributesKey(
            "MORPHINGBIRD_SPELLRELAX_OPERATOR", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey BRACKET = createTextAttributesKey(
            "MORPHINGBIRD_SPELLRELAX_BRACKET", DefaultLanguageHighlighterColors.PARENTHESES);
    public static final TextAttributesKey ESCAPE = createTextAttributesKey(
            "MORPHINGBIRD_SPELLRELAX_ESCAPE", DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE);

    private static TextAttributesKey[] k(TextAttributesKey x){return new TextAttributesKey[]{x};}
    private static final TextAttributesKey[] EMPTY = new TextAttributesKey[0];

    @Override public @NotNull Lexer getHighlightingLexer() { return new SpellRelaxLexer(); }
    @Override public TextAttributesKey @NotNull [] getTokenHighlights(IElementType t) {
        if (t == SpellRelaxTokenTypes.COMMENT) return k(COMMENT);
        if (t == SpellRelaxTokenTypes.OPERATOR) return k(OPERATOR);
        if (t == SpellRelaxTokenTypes.BRACKET) return k(BRACKET);
        if (t == SpellRelaxTokenTypes.ESCAPE) return k(ESCAPE);
        return EMPTY;
    }
}
