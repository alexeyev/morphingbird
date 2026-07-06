package io.github.alexeyev.morphingbird.lexd;

import com.intellij.openapi.fileTypes.LanguageFileType;
import io.github.alexeyev.morphingbird.common.MorphingbirdIcons;
import org.jetbrains.annotations.NotNull;
import javax.swing.Icon;

/** The {@code .lexd} lexicon file type. */
public final class LexdFileType extends LanguageFileType {
    public static final LexdFileType INSTANCE = new LexdFileType();
    private LexdFileType() { super(LexdLanguage.INSTANCE); }
    @Override public @NotNull String getName() { return "Apertium lexd"; }
    @Override public @NotNull String getDescription() {
        return "Apertium lexd morphological lexicon (.lexd)";
    }
    @Override public @NotNull String getDefaultExtension() { return "lexd"; }
    @Override public Icon getIcon() { return MorphingbirdIcons.LEXC; }
}
