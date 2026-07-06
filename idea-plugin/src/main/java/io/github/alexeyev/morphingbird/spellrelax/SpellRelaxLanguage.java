package io.github.alexeyev.morphingbird.spellrelax;

import com.intellij.lang.Language;

/** The spellrelax language (HFST regex rules for accepting orthographic variants). */
public final class SpellRelaxLanguage extends Language {
    public static final SpellRelaxLanguage INSTANCE = new SpellRelaxLanguage();
    private SpellRelaxLanguage() { super("MorphingbirdSpellRelax"); }
    @Override public String getDisplayName() { return "Apertium spellrelax"; }
    @Override public boolean isCaseSensitive() { return true; }
}
