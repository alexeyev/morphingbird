package io.github.alexeyev.morphingbird.twol;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public final class TwolSyntaxHighlighter extends SyntaxHighlighterBase {
    public static final TextAttributesKey COMMENT = createTextAttributesKey(
            "APERTIUM_TWOL_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
    public static final TextAttributesKey SECTION = createTextAttributesKey(
            "APERTIUM_TWOL_SECTION", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey ARCHIPHONEME = createTextAttributesKey(
            "APERTIUM_TWOL_ARCHIPHONEME", DefaultLanguageHighlighterColors.INSTANCE_FIELD);
    public static final TextAttributesKey RULENAME = createTextAttributesKey(
            "APERTIUM_TWOL_RULENAME", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey OPERATOR = createTextAttributesKey(
            "APERTIUM_TWOL_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN);

    private static TextAttributesKey[] k(TextAttributesKey x){return new TextAttributesKey[]{x};}
    private static final TextAttributesKey[] EMPTY = new TextAttributesKey[0];

    @Override public @NotNull Lexer getHighlightingLexer() { return new TwolLexer(); }

    @Override public TextAttributesKey @NotNull [] getTokenHighlights(IElementType t) {
        if (t == TwolTokenTypes.COMMENT) return k(COMMENT);
        if (t == TwolTokenTypes.SECTION) return k(SECTION);
        if (t == TwolTokenTypes.ARCHIPHONEME) return k(ARCHIPHONEME);
        if (t == TwolTokenTypes.RULENAME) return k(RULENAME);
        if (t == TwolTokenTypes.OPERATOR) return k(OPERATOR);
        return EMPTY;
    }
}
