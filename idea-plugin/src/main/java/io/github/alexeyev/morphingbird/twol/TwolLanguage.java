package io.github.alexeyev.morphingbird.twol;

import com.intellij.lang.Language;

/** The twol language (apertium two-level morphophonology rules). */
public final class TwolLanguage extends Language {
    public static final TwolLanguage INSTANCE = new TwolLanguage();
    private TwolLanguage() { super("MorphingbirdTwol"); }
    @Override public String getDisplayName() { return "Apertium twol"; }
    @Override public boolean isCaseSensitive() { return true; }
}
