package io.github.alexeyev.morphingbird.cg3;
import com.intellij.lang.Commenter;
import org.jetbrains.annotations.Nullable;
/** CG3 uses {@code #} for line comments. */
public final class Cg3Commenter implements Commenter {
    @Override public @Nullable String getLineCommentPrefix() { return "#"; }
    @Override public @Nullable String getBlockCommentPrefix() { return null; }
    @Override public @Nullable String getBlockCommentSuffix() { return null; }
    @Override public @Nullable String getCommentedBlockCommentPrefix() { return null; }
    @Override public @Nullable String getCommentedBlockCommentSuffix() { return null; }
}
