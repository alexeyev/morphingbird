package io.github.alexeyev.morphingbird.udx.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import io.github.alexeyev.morphingbird.udx.UdxFileType;
import io.github.alexeyev.morphingbird.udx.UdxLanguage;
import org.jetbrains.annotations.NotNull;

public final class UdxFile extends PsiFileBase {
    public UdxFile(@NotNull FileViewProvider vp) { super(vp, UdxLanguage.INSTANCE); }
    @Override public @NotNull FileType getFileType() { return UdxFileType.INSTANCE; }
    @Override public String toString() { return "Apertium UD mapping file"; }
}
