package io.github.alexeyev.morphingbird.udx;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/** Element types for udx highlighting. */
public final class UdxTokenTypes {
    private UdxTokenTypes() {}
    public static final class T extends IElementType {
        public T(@NonNls @NotNull String n) { super(n, UdxLanguage.INSTANCE); }
    }
    public static final IElementType WHITESPACE = new T("WHITESPACE");
    public static final IElementType COMMENT    = new T("COMMENT");
    public static final IElementType TAG        = new T("TAG");       // Apertium tag (cols 2-3)
    public static final IElementType UPOS       = new T("UPOS");      // UD POS (col 6)
    public static final IElementType UFEAT      = new T("UFEAT");     // UD features (col 7)
    public static final IElementType WILDCARD   = new T("WILDCARD");  // _
    public static final IElementType PIPE       = new T("PIPE");      // | between tags
    public static final IElementType OTHER      = new T("OTHER");
}
