package io.github.alexeyev.morphingbird.twol;

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

/** Settings → Editor → Color Scheme → Apertium twol. */
public final class TwolColorSettingsPage implements ColorSettingsPage {

    private static final AttributesDescriptor[] DESCRIPTORS = {
            new AttributesDescriptor("Comment", TwolSyntaxHighlighter.COMMENT),
            new AttributesDescriptor("Section (Alphabet, Rules, Sets)",
                    TwolSyntaxHighlighter.SECTION),
            new AttributesDescriptor("Archiphoneme ({A}, {G}, …)",
                    TwolSyntaxHighlighter.ARCHIPHONEME),
            new AttributesDescriptor("Rule name", TwolSyntaxHighlighter.RULENAME),
            new AttributesDescriptor("Operator (<=>, =>, /<=)",
                    TwolSyntaxHighlighter.OPERATOR),
    };

    @Override public @Nullable Icon getIcon() { return MorphingbirdIcons.TWOL; }
    @Override public @NotNull SyntaxHighlighter getHighlighter() { return new TwolSyntaxHighlighter(); }

    @Override
    public @NotNull String getDemoText() {
        return "! Two-level rules\n"
                + "Alphabet\n"
                + " %{A%}:a %{A%}:e ;\n\n"
                + "Sets\n"
                + " Vowel = a e i o u ;\n\n"
                + "Rules\n\n"
                + "\"Back harmony of {A}\"\n"
                + "%{A%}:a <=> Vowel: _ ;\n";
    }

    @Override
    public @Nullable Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return null;
    }

    @Override public AttributesDescriptor @NotNull [] getAttributeDescriptors() { return DESCRIPTORS; }
    @Override public ColorDescriptor @NotNull [] getColorDescriptors() { return ColorDescriptor.EMPTY_ARRAY; }
    @Override public @NotNull String getDisplayName() { return "Apertium twol"; }
}
