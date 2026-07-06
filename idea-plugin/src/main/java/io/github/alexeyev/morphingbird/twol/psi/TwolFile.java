package io.github.alexeyev.morphingbird.twol.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import io.github.alexeyev.morphingbird.twol.TwolFileType;
import io.github.alexeyev.morphingbird.twol.TwolLanguage;
import org.jetbrains.annotations.NotNull;

public final class TwolFile extends PsiFileBase {
    public TwolFile(@NotNull FileViewProvider vp) { super(vp, TwolLanguage.INSTANCE); }
    @Override public @NotNull FileType getFileType() { return TwolFileType.INSTANCE; }
    @Override public String toString() { return "Apertium twol file"; }
}
