package io.github.alexeyev.morphingbird.cg3.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import io.github.alexeyev.morphingbird.cg3.Cg3FileType;
import io.github.alexeyev.morphingbird.cg3.Cg3Language;
import org.jetbrains.annotations.NotNull;

public final class Cg3File extends PsiFileBase {
    public Cg3File(@NotNull FileViewProvider vp) { super(vp, Cg3Language.INSTANCE); }
    @Override public @NotNull FileType getFileType() { return Cg3FileType.INSTANCE; }
    @Override public String toString() { return "Apertium CG3 file"; }
}
