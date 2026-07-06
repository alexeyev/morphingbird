package io.github.alexeyev.morphingbird.lexc.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import io.github.alexeyev.morphingbird.lexc.LexcFileType;
import io.github.alexeyev.morphingbird.lexc.LexcLanguage;
import org.jetbrains.annotations.NotNull;

/** The PSI file root for a lexc file. */
public final class LexcFile extends PsiFileBase {
    public LexcFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, LexcLanguage.INSTANCE);
    }

    @Override
    public @NotNull FileType getFileType() {
        return LexcFileType.INSTANCE;
    }

    @Override
    public String toString() {
        return "Apertium lexc file";
    }
}
