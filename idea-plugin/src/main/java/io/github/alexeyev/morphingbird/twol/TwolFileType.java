package io.github.alexeyev.morphingbird.twol;

import com.intellij.openapi.fileTypes.LanguageFileType;
import io.github.alexeyev.morphingbird.common.MorphingbirdIcons;
import org.jetbrains.annotations.NotNull;
import javax.swing.Icon;

/** The {@code .twol} / {@code .twoc} file type. */
public final class TwolFileType extends LanguageFileType {
    public static final TwolFileType INSTANCE = new TwolFileType();
    private TwolFileType() { super(TwolLanguage.INSTANCE); }
    @Override public @NotNull String getName() { return "Apertium twol"; }
    @Override public @NotNull String getDescription() { return "Apertium twol morphophonology rules"; }
    @Override public @NotNull String getDefaultExtension() { return "twol"; }
    @Override public Icon getIcon() { return MorphingbirdIcons.TWOL; }
}
