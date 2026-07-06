package io.github.alexeyev.morphingbird.udx;

import com.intellij.lang.Language;

/** The udx language (Apertium -> Universal Dependencies tag mapping rules). */
public final class UdxLanguage extends Language {
    public static final UdxLanguage INSTANCE = new UdxLanguage();
    private UdxLanguage() { super("MorphingbirdUdx"); }
    @Override public String getDisplayName() { return "Apertium UD mapping"; }
    @Override public boolean isCaseSensitive() { return true; }
}
