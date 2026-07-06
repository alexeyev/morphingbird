package io.github.alexeyev.morphingbird.spellrelax.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import io.github.alexeyev.morphingbird.spellrelax.SpellRelaxFileType;
import io.github.alexeyev.morphingbird.spellrelax.SpellRelaxLanguage;
import org.jetbrains.annotations.NotNull;

public final class SpellRelaxFile extends PsiFileBase {
    public SpellRelaxFile(@NotNull FileViewProvider vp) { super(vp, SpellRelaxLanguage.INSTANCE); }
    @Override public @NotNull FileType getFileType() { return SpellRelaxFileType.INSTANCE; }
    @Override public String toString() { return "Apertium spellrelax file"; }
}
