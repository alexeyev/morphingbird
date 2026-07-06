package io.github.alexeyev.morphingbird.lexd;

import com.intellij.lang.Language;

/** The lexd lexicon language (Apertium's newer pattern/lexicon compiler). */
public final class LexdLanguage extends Language {
    public static final LexdLanguage INSTANCE = new LexdLanguage();
    private LexdLanguage() { super("MorphingbirdLexd"); }
    @Override public String getDisplayName() { return "Apertium lexd"; }
    @Override public boolean isCaseSensitive() { return true; }
}
