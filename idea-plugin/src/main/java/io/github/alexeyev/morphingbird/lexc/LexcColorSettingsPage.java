package io.github.alexeyev.morphingbird.lexc;

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

/** Settings → Editor → Color Scheme → Apertium lexc. */
public final class LexcColorSettingsPage implements ColorSettingsPage {

    private static final AttributesDescriptor[] DESCRIPTORS = {
            new AttributesDescriptor("Comment", LexcSyntaxHighlighter.COMMENT),
            new AttributesDescriptor("Keyword (LEXICON, Multichar_Symbols)",
                    LexcSyntaxHighlighter.KEYWORD),
            new AttributesDescriptor("Tag (<nom>, <n>, …)", LexcSyntaxHighlighter.TAG),
            new AttributesDescriptor("Archiphoneme ({A}, {G}, …)",
                    LexcSyntaxHighlighter.ARCHIPHONEME),
            new AttributesDescriptor("Identifier / continuation class",
                    LexcSyntaxHighlighter.IDENTIFIER),
            new AttributesDescriptor("Weight string", LexcSyntaxHighlighter.STRING),
            new AttributesDescriptor("Colon (upper:lower)", LexcSyntaxHighlighter.OPERATOR),
            new AttributesDescriptor("Semicolon", LexcSyntaxHighlighter.SEMICOLON),
    };

    @Override
    public @Nullable Icon getIcon() {
        return MorphingbirdIcons.LEXICON;
    }

    @Override
    public @NotNull SyntaxHighlighter getHighlighter() {
        return new LexcSyntaxHighlighter();
    }

    @Override
    public @NotNull String getDemoText() {
        return "Multichar_Symbols\n"
                + "%<n%>      ! Noun\n"
                + "%<nom%>    ! Nominative\n"
                + "%{A%}      ! back/front archiphoneme\n\n"
                + "LEXICON Nouns\n"
                + "ибарат:ибарат N-INFL ;   ! a stem\n"
                + "ким:ким PRON-ITG \"weight: 1.0\" ; ! who?\n\n"
                + "LEXICON N-INFL\n"
                + "%<n%>%<nom%>:%>%{A%} # ;\n";
    }

    @Override
    public @Nullable Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return null;
    }

    @Override
    public AttributesDescriptor @NotNull [] getAttributeDescriptors() {
        return DESCRIPTORS;
    }

    @Override
    public ColorDescriptor @NotNull [] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Apertium lexc";
    }
}
