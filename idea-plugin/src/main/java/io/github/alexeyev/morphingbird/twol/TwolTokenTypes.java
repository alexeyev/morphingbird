package io.github.alexeyev.morphingbird.twol;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/** Element types for twol highlighting. */
public final class TwolTokenTypes {
    private TwolTokenTypes() {}
    public static final class T extends IElementType {
        public T(@NonNls @NotNull String n) { super(n, TwolLanguage.INSTANCE); }
    }
    public static final IElementType WHITESPACE = new T("WHITESPACE");
    public static final IElementType COMMENT    = new T("COMMENT");
    public static final IElementType SECTION    = new T("SECTION");   // Alphabet/Sets/Rules/Definitions
    public static final IElementType ARCHIPHONEME = new T("ARCHIPHONEME"); // %{X%}
    public static final IElementType RULENAME   = new T("RULENAME");  // "..."
    public static final IElementType OPERATOR   = new T("OPERATOR");  // <=> => <= etc
    public static final IElementType IDENT      = new T("IDENT");
    public static final IElementType OTHER      = new T("OTHER");
}
