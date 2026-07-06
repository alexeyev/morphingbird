package io.github.alexeyev.morphingbird.lexd;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/** Element types for lexd highlighting. */
public final class LexdTokenTypes {
    private LexdTokenTypes() {}
    public static final class T extends IElementType {
        public T(@NonNls @NotNull String n) { super(n, LexdLanguage.INSTANCE); }
    }
    public static final IElementType WHITESPACE = new T("WHITESPACE");
    public static final IElementType COMMENT    = new T("COMMENT");
    public static final IElementType KEYWORD    = new T("KEYWORD");   // PATTERNS/PATTERN/LEXICON
    public static final IElementType TAG        = new T("TAG");       // <np>
    public static final IElementType ARCHIPHONEME = new T("ARCHIPHONEME"); // {Z}
    public static final IElementType SIEVE      = new T("SIEVE");     // [l,impf,0cm]
    public static final IElementType IDENT      = new T("IDENT");     // lexicon/pattern names
    public static final IElementType OTHER      = new T("OTHER");
}
