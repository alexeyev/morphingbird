package io.github.alexeyev.morphingbird.spellrelax;

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

/** Settings → Editor → Color Scheme → Apertium spellrelax. */
public final class SpellRelaxColorSettingsPage implements ColorSettingsPage {

    private static final AttributesDescriptor[] DESCRIPTORS = {
            new AttributesDescriptor("Comment", SpellRelaxSyntaxHighlighter.COMMENT),
            new AttributesDescriptor("Operator ((->), .o., <-)",
                    SpellRelaxSyntaxHighlighter.OPERATOR),
            new AttributesDescriptor("Bracket / grouping ([ ] ( ))",
                    SpellRelaxSyntaxHighlighter.BRACKET),
            new AttributesDescriptor("Escape (%x)", SpellRelaxSyntaxHighlighter.ESCAPE),
    };

    @Override public @Nullable Icon getIcon() { return MorphingbirdIcons.TWOL; }
    @Override public @NotNull SyntaxHighlighter getHighlighter() { return new SpellRelaxSyntaxHighlighter(); }

    @Override
    public @NotNull String getDemoText() {
        return "! spellrelax: accept orthographic variants\n"
                + "[\n"
                + "  [ %' (->) {’} ] .o.   ! straight to curly apostrophe\n"
                + "  [ e (->) [ e | é ] ]  ! accept accented e\n"
                + "]\n";
    }

    @Override
    public @Nullable Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return null;
    }

    @Override public AttributesDescriptor @NotNull [] getAttributeDescriptors() { return DESCRIPTORS; }
    @Override public ColorDescriptor @NotNull [] getColorDescriptors() { return ColorDescriptor.EMPTY_ARRAY; }
    @Override public @NotNull String getDisplayName() { return "Apertium spellrelax"; }
}
