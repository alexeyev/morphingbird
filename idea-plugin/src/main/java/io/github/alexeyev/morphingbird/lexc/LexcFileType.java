package io.github.alexeyev.morphingbird.lexc;

import com.intellij.openapi.fileTypes.LanguageFileType;
import io.github.alexeyev.morphingbird.common.MorphingbirdIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/** The {@code .lexc} file type. */
public final class LexcFileType extends LanguageFileType {
    public static final LexcFileType INSTANCE = new LexcFileType();

    private LexcFileType() {
        super(LexcLanguage.INSTANCE);
    }

    @Override
    public @NotNull String getName() {
        return "Apertium lexc";
    }

    @Override
    public @NotNull String getDescription() {
        return "Apertium lexc morphological lexicon";
    }

    @Override
    public @NotNull String getDefaultExtension() {
        return "lexc";
    }

    @Override
    public Icon getIcon() {
        return MorphingbirdIcons.LEXC;
    }
}
