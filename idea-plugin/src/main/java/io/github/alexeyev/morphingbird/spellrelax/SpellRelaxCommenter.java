package io.github.alexeyev.morphingbird.spellrelax;

import com.intellij.lang.Commenter;
import org.jetbrains.annotations.Nullable;

/** spellrelax uses ! line comments (HFST/twol convention). */
public final class SpellRelaxCommenter implements Commenter {
    @Override public @Nullable String getLineCommentPrefix() { return "!"; }
    @Override public @Nullable String getBlockCommentPrefix() { return null; }
    @Override public @Nullable String getBlockCommentSuffix() { return null; }
    @Override public @Nullable String getCommentedBlockCommentPrefix() { return null; }
    @Override public @Nullable String getCommentedBlockCommentSuffix() { return null; }
}
