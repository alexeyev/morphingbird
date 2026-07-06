package io.github.alexeyev.morphingbird.lexc;

import com.intellij.psi.tree.IElementType;
import io.github.alexeyev.morphingbird.core.LexcToken;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

/**
 * IntelliJ {@link IElementType}s for lexc tokens, plus a mapping from the
 * core {@link LexcToken.Kind} (produced by the tested, IntelliJ-free
 * {@code LexcScanner}) to these element types. Keeping the scanner separate and
 * mapping here means the hard escape logic is unit-tested once and reused.
 */
public final class LexcTokenTypes {
    private LexcTokenTypes() {}

    public static final class LexcTokenType extends IElementType {
        public LexcTokenType(@NonNls @NotNull String debugName) {
            super(debugName, LexcLanguage.INSTANCE);
        }
    }

    public static final IElementType WHITESPACE   = new LexcTokenType("WHITESPACE");
    public static final IElementType COMMENT       = new LexcTokenType("COMMENT");
    public static final IElementType KW_MULTICHAR  = new LexcTokenType("KW_MULTICHAR");
    public static final IElementType KW_LEXICON    = new LexcTokenType("KW_LEXICON");
    public static final IElementType KW_DEFINITIONS= new LexcTokenType("KW_DEFINITIONS");
    public static final IElementType TAG           = new LexcTokenType("TAG");
    public static final IElementType ARCHIPHONEME  = new LexcTokenType("ARCHIPHONEME");
    public static final IElementType IDENTIFIER    = new LexcTokenType("IDENTIFIER");
    public static final IElementType COLON         = new LexcTokenType("COLON");
    public static final IElementType SEMICOLON     = new LexcTokenType("SEMICOLON");
    public static final IElementType STRING        = new LexcTokenType("STRING");
    public static final IElementType WORD          = new LexcTokenType("WORD");
    public static final IElementType OTHER         = new LexcTokenType("OTHER");
    public static final IElementType BAD_CHARACTER = new LexcTokenType("BAD_CHARACTER");

    private static final Map<LexcToken.Kind, IElementType> MAP =
            new EnumMap<>(LexcToken.Kind.class);
    static {
        MAP.put(LexcToken.Kind.WHITESPACE, WHITESPACE);
        MAP.put(LexcToken.Kind.COMMENT, COMMENT);
        MAP.put(LexcToken.Kind.KW_MULTICHAR, KW_MULTICHAR);
        MAP.put(LexcToken.Kind.KW_LEXICON, KW_LEXICON);
        MAP.put(LexcToken.Kind.KW_DEFINITIONS, KW_DEFINITIONS);
        MAP.put(LexcToken.Kind.TAG, TAG);
        MAP.put(LexcToken.Kind.ARCHIPHONEME, ARCHIPHONEME);
        MAP.put(LexcToken.Kind.IDENTIFIER, IDENTIFIER);
        MAP.put(LexcToken.Kind.COLON, COLON);
        MAP.put(LexcToken.Kind.SEMICOLON, SEMICOLON);
        MAP.put(LexcToken.Kind.STRING, STRING);
        MAP.put(LexcToken.Kind.WORD, WORD);
        MAP.put(LexcToken.Kind.OTHER, OTHER);
        // EOF has no element type (the lexer returns null at end).
    }

    public static IElementType of(LexcToken.Kind kind) {
        return MAP.getOrDefault(kind, OTHER);
    }
}
