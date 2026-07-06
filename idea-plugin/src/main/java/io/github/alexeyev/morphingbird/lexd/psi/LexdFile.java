package io.github.alexeyev.morphingbird.lexd.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import io.github.alexeyev.morphingbird.lexd.LexdFileType;
import io.github.alexeyev.morphingbird.lexd.LexdLanguage;
import org.jetbrains.annotations.NotNull;

public final class LexdFile extends PsiFileBase {
    public LexdFile(@NotNull FileViewProvider vp) { super(vp, LexdLanguage.INSTANCE); }
    @Override public @NotNull FileType getFileType() { return LexdFileType.INSTANCE; }
    @Override public String toString() { return "Apertium lexd file"; }
}
