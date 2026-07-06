package io.github.alexeyev.morphingbird.lexd;

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

/** Settings → Editor → Color Scheme → Apertium lexd. */
public final class LexdColorSettingsPage implements ColorSettingsPage {

    private static final AttributesDescriptor[] DESCRIPTORS = {
            new AttributesDescriptor("Comment", LexdSyntaxHighlighter.COMMENT),
            new AttributesDescriptor("Keyword (PATTERNS, PATTERN, LEXICON)",
                    LexdSyntaxHighlighter.KEYWORD),
            new AttributesDescriptor("Tag (<np>, <s_1sg>, …)", LexdSyntaxHighlighter.TAG),
            new AttributesDescriptor("Archiphoneme ({Z}, {E}, …)",
                    LexdSyntaxHighlighter.ARCHIPHONEME),
            new AttributesDescriptor("Tag-filter / sieve ([I,m], [l,perf])",
                    LexdSyntaxHighlighter.SIEVE),
    };

    @Override public @Nullable Icon getIcon() { return MorphingbirdIcons.LEXC; }
    @Override public @NotNull SyntaxHighlighter getHighlighter() { return new LexdSyntaxHighlighter(); }

    @Override
    public @NotNull String getDemoText() {
        return "# lexd pattern/lexicon morphology\n"
                + "PATTERNS\n"
                + "NOUN\n\n"
                + "PATTERN NOUN\n"
                + "NounRoot[I,m] [<n><m>:] NounInfl\n\n"
                + "LEXICON NounInfl\n"
                + "<n><m><sg>:{Z}\n"
                + "<n><m><pl>:lar\n";
    }

    @Override
    public @Nullable Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return null;
    }

    @Override public AttributesDescriptor @NotNull [] getAttributeDescriptors() { return DESCRIPTORS; }
    @Override public ColorDescriptor @NotNull [] getColorDescriptors() { return ColorDescriptor.EMPTY_ARRAY; }
    @Override public @NotNull String getDisplayName() { return "Apertium lexd"; }
}
