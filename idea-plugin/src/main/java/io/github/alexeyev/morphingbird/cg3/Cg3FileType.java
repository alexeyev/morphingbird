package io.github.alexeyev.morphingbird.cg3;
import com.intellij.openapi.fileTypes.LanguageFileType;
import io.github.alexeyev.morphingbird.common.MorphingbirdIcons;
import org.jetbrains.annotations.NotNull;
import javax.swing.Icon;
/** The {@code .rlx} CG3 file type. */
public final class Cg3FileType extends LanguageFileType {
    public static final Cg3FileType INSTANCE = new Cg3FileType();
    private Cg3FileType() { super(Cg3Language.INSTANCE); }
    @Override public @NotNull String getName() { return "Apertium CG3"; }
    @Override public @NotNull String getDescription() { return "Apertium CG3 constraint grammar"; }
    @Override public @NotNull String getDefaultExtension() { return "rlx"; }
    @Override public Icon getIcon() { return MorphingbirdIcons.CG3; }
}
