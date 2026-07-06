package io.github.alexeyev.morphingbird.spellrelax;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/** Element types for spellrelax highlighting. */
public final class SpellRelaxTokenTypes {
    private SpellRelaxTokenTypes() {}
    public static final class T extends IElementType {
        public T(@NonNls @NotNull String n) { super(n, SpellRelaxLanguage.INSTANCE); }
    }
    public static final IElementType WHITESPACE = new T("WHITESPACE");
    public static final IElementType COMMENT    = new T("COMMENT");   // ! to EOL
    public static final IElementType OPERATOR   = new T("OPERATOR");  // (->) -> .o. <-
    public static final IElementType BRACKET    = new T("BRACKET");   // [ ] ( )
    public static final IElementType ESCAPE     = new T("ESCAPE");    // %x
    public static final IElementType SYMBOL     = new T("SYMBOL");    // bare chars
    public static final IElementType OTHER      = new T("OTHER");
}
