package io.github.alexeyev.morphingbird.spellrelax;

import com.intellij.openapi.fileTypes.LanguageFileType;
import io.github.alexeyev.morphingbird.common.MorphingbirdIcons;
import org.jetbrains.annotations.NotNull;
import javax.swing.Icon;

/**
 * The {@code .spellrelax} file type — HFST regex rules (a {@code (->)} / {@code .o.}
 * cascade) that let the analyser accept orthographic and typographic variants.
 * Standard in modern Apertium modules (bootstrapped via
 * {@code apertium-init --with-spellrelax}).
 */
public final class SpellRelaxFileType extends LanguageFileType {
    public static final SpellRelaxFileType INSTANCE = new SpellRelaxFileType();
    private SpellRelaxFileType() { super(SpellRelaxLanguage.INSTANCE); }
    @Override public @NotNull String getName() { return "Apertium spellrelax"; }
    @Override public @NotNull String getDescription() {
        return "Apertium spellrelax orthographic-variant rules (.spellrelax)";
    }
    @Override public @NotNull String getDefaultExtension() { return "spellrelax"; }
    @Override public Icon getIcon() { return MorphingbirdIcons.TWOL; }
}
