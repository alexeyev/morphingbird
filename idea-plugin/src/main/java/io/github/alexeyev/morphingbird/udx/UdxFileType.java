package io.github.alexeyev.morphingbird.udx;

import com.intellij.openapi.fileTypes.LanguageFileType;
import io.github.alexeyev.morphingbird.common.MorphingbirdIcons;
import org.jetbrains.annotations.NotNull;
import javax.swing.Icon;

/** The {@code .udx} file type (Apertium tags -> Universal Dependencies). */
public final class UdxFileType extends LanguageFileType {
    public static final UdxFileType INSTANCE = new UdxFileType();
    private UdxFileType() { super(UdxLanguage.INSTANCE); }
    @Override public @NotNull String getName() { return "Apertium UD mapping"; }
    @Override public @NotNull String getDescription() {
        return "Apertium to Universal Dependencies tag mapping (.udx)";
    }
    @Override public @NotNull String getDefaultExtension() { return "udx"; }
    @Override public Icon getIcon() { return MorphingbirdIcons.UDX; }
}
