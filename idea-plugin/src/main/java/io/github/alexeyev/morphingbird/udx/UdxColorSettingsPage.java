package io.github.alexeyev.morphingbird.udx;

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

/** Settings → Editor → Color Scheme → Apertium UD mapping. */
public final class UdxColorSettingsPage implements ColorSettingsPage {

    private static final AttributesDescriptor[] DESCRIPTORS = {
            new AttributesDescriptor("Comment", UdxSyntaxHighlighter.COMMENT),
            new AttributesDescriptor("Apertium tag (n, nom, …)", UdxSyntaxHighlighter.TAG),
            new AttributesDescriptor("Universal POS (NOUN, VERB, …)",
                    UdxSyntaxHighlighter.UPOS),
            new AttributesDescriptor("Universal feature (Case=Nom, …)",
                    UdxSyntaxHighlighter.UFEAT),
            new AttributesDescriptor("Wildcard (_)", UdxSyntaxHighlighter.WILDCARD),
    };

    @Override public @Nullable Icon getIcon() { return MorphingbirdIcons.UDX; }
    @Override public @NotNull SyntaxHighlighter getHighlighter() { return new UdxSyntaxHighlighter(); }

    @Override
    public @NotNull String getDemoText() {
        // Real .udx is 8 tab-separated columns; the Apertium tag sits in column 3
        // (0-based 2), the UD POS in column 6, the UD features in column 7.
        return "# Apertium -> Universal Dependencies mapping\n"
                + "_\t_\tn\t_\t_\tNOUN\t_\t_\n"
                + "_\t_\tsg\t_\t_\t_\tNumber=Sing\t_\n"
                + "_\t_\tpl\t_\t_\t_\tNumber=Plur\t_\n"
                + "_\t_\tvblex\t_\t_\tVERB\tTense=Pres\t_\n"
                + "_\t_\tadj\t_\t_\tADJ\t_\t_\n";
    }

    @Override
    public @Nullable Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return null;
    }

    @Override public AttributesDescriptor @NotNull [] getAttributeDescriptors() { return DESCRIPTORS; }
    @Override public ColorDescriptor @NotNull [] getColorDescriptors() { return ColorDescriptor.EMPTY_ARRAY; }
    @Override public @NotNull String getDisplayName() { return "Apertium UD mapping"; }
}
