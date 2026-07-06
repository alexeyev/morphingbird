package io.github.alexeyev.morphingbird.cg3;
import com.intellij.lang.Language;
/** The CG3 language (apertium constraint-grammar disambiguation). */
public final class Cg3Language extends Language {
    public static final Cg3Language INSTANCE = new Cg3Language();
    private Cg3Language() { super("MorphingbirdCg3"); }
    @Override public String getDisplayName() { return "Apertium CG3"; }
    @Override public boolean isCaseSensitive() { return true; }
}
