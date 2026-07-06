package io.github.alexeyev.morphingbird.cg3;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public final class Cg3SyntaxHighlighter extends SyntaxHighlighterBase {
    public static final TextAttributesKey COMMENT = createTextAttributesKey(
            "APERTIUM_CG3_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
    public static final TextAttributesKey KEYWORD = createTextAttributesKey(
            "APERTIUM_CG3_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey SETNAME = createTextAttributesKey(
            "APERTIUM_CG3_SETNAME", DefaultLanguageHighlighterColors.CLASS_NAME);
    public static final TextAttributesKey STRING = createTextAttributesKey(
            "APERTIUM_CG3_STRING", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey TAG = createTextAttributesKey(
            "APERTIUM_CG3_TAG", DefaultLanguageHighlighterColors.METADATA);

    private static TextAttributesKey[] k(TextAttributesKey x){return new TextAttributesKey[]{x};}
    private static final TextAttributesKey[] EMPTY = new TextAttributesKey[0];

    @Override public @NotNull Lexer getHighlightingLexer() { return new Cg3Lexer(); }
    @Override public TextAttributesKey @NotNull [] getTokenHighlights(IElementType t) {
        if (t == Cg3TokenTypes.COMMENT) return k(COMMENT);
        if (t == Cg3TokenTypes.KEYWORD) return k(KEYWORD);
        if (t == Cg3TokenTypes.SETNAME) return k(SETNAME);
        if (t == Cg3TokenTypes.STRING) return k(STRING);
        if (t == Cg3TokenTypes.TAG) return k(TAG);
        return EMPTY;
    }
}
