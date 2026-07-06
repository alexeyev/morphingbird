package io.github.alexeyev.morphingbird.cg3;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import io.github.alexeyev.morphingbird.common.MorphingbirdIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Map;

/** Settings → Editor → Color Scheme → Apertium CG3. */
public final class Cg3ColorSettingsPage implements ColorSettingsPage {

    private static final AttributesDescriptor[] DESCRIPTORS = {
            new AttributesDescriptor("Comment", Cg3SyntaxHighlighter.COMMENT),
            new AttributesDescriptor("Keyword (LIST, SET, SELECT, REMOVE)",
                    Cg3SyntaxHighlighter.KEYWORD),
            new AttributesDescriptor("Set name", Cg3SyntaxHighlighter.SETNAME),
            new AttributesDescriptor("String / wordform (\"...\")",
                    Cg3SyntaxHighlighter.STRING),
            new AttributesDescriptor("Tag (nom, n, …)", Cg3SyntaxHighlighter.TAG),
    };

    @Override public @Nullable Icon getIcon() { return MorphingbirdIcons.CG3; }
    @Override public @NotNull SyntaxHighlighter getHighlighter() { return new Cg3SyntaxHighlighter(); }

    @Override
    public @NotNull String getDemoText() {
        return "# Constraint Grammar disambiguation\n"
                + "LIST Noun = n ;\n"
                + "LIST Verb = vblex vbser ;\n"
                + "SET NounOrVerb = Noun OR Verb ;\n\n"
                + "SELECT Noun IF (1 (det)) ;\n"
                + "REMOVE Verb IF (-1 (\"the\")) ;\n";
    }

    @Override
    public @Nullable Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return null;
    }

    @Override public AttributesDescriptor @NotNull [] getAttributeDescriptors() { return DESCRIPTORS; }
    @Override public ColorDescriptor @NotNull [] getColorDescriptors() { return ColorDescriptor.EMPTY_ARRAY; }
    @Override public @NotNull String getDisplayName() { return "Apertium CG3"; }
}
