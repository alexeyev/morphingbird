package io.github.alexeyev.morphingbird.cg3;

import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Cg3SyntaxHighlighterFactory extends SyntaxHighlighterFactory {
    @Override public @NotNull SyntaxHighlighter getSyntaxHighlighter(
            @Nullable Project p, @Nullable VirtualFile f) { return new Cg3SyntaxHighlighter(); }
}
