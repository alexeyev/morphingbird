package io.github.alexeyev.morphingbird.twol;

import com.intellij.lang.Commenter;
import org.jetbrains.annotations.Nullable;

/** twol uses {@code !} for line comments. */
public final class TwolCommenter implements Commenter {
    @Override public @Nullable String getLineCommentPrefix() { return "!"; }
    @Override public @Nullable String getBlockCommentPrefix() { return null; }
    @Override public @Nullable String getBlockCommentSuffix() { return null; }
    @Override public @Nullable String getCommentedBlockCommentPrefix() { return null; }
    @Override public @Nullable String getCommentedBlockCommentSuffix() { return null; }
}
