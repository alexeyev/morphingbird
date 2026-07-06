package io.github.alexeyev.morphingbird.cg3;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
/** Element types for CG3 highlighting. */
public final class Cg3TokenTypes {
    private Cg3TokenTypes() {}
    public static final class T extends IElementType {
        public T(@NonNls @NotNull String n) { super(n, Cg3Language.INSTANCE); }
    }
    public static final IElementType WHITESPACE = new T("WHITESPACE");
    public static final IElementType COMMENT    = new T("COMMENT");
    public static final IElementType KEYWORD    = new T("KEYWORD");
    public static final IElementType SETNAME    = new T("SETNAME");
    public static final IElementType STRING     = new T("STRING");
    public static final IElementType TAG        = new T("TAG");
    public static final IElementType IDENT      = new T("IDENT");
    public static final IElementType OTHER      = new T("OTHER");
}
