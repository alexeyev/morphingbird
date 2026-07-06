package io.github.alexeyev.morphingbird.lexc;

import com.intellij.lang.Language;

/** The lexc language (apertium morphological lexicon / morphotactics). */
public final class LexcLanguage extends Language {
    public static final LexcLanguage INSTANCE = new LexcLanguage();

    private LexcLanguage() {
        super("MorphingbirdLexc");
    }

    @Override
    public String getDisplayName() {
        return "Apertium lexc";
    }

    @Override
    public boolean isCaseSensitive() {
        return true;
    }
}
