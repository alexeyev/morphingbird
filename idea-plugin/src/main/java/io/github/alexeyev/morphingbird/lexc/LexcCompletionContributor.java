package io.github.alexeyev.morphingbird.lexc;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.util.ProcessingContext;
import io.github.alexeyev.morphingbird.common.MorphingbirdIcons;
import io.github.alexeyev.morphingbird.core.SymbolIndex;
import io.github.alexeyev.morphingbird.index.MorphingbirdIndexService;
import org.jetbrains.annotations.NotNull;

/**
 * Index-driven completion for lexc. Offers the project's real continuation
 * classes (LEXICON names) and declared tags, rather than a static keyword list —
 * so completions reflect the actual morphology and a typo'd tag is hard to write.
 */
public final class LexcCompletionContributor extends CompletionContributor {

    public LexcCompletionContributor() {
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement(),
                new CompletionProvider<>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters params,
                                                  @NotNull ProcessingContext ctx,
                                                  @NotNull CompletionResultSet result) {
                        var project = params.getPosition().getProject();
                        SymbolIndex index =
                                MorphingbirdIndexService.getInstance(project).getIndex();
                        if (index == null) return;

                        // Continuation classes (LEXICON names).
                        for (String name : index.lexiconNames()) {
                            result.addElement(LookupElementBuilder.create(name)
                                    .withIcon(MorphingbirdIcons.LEXICON)
                                    .withTypeText("continuation class"));
                        }

                        // Declared tags — offered with the % escapes so they are
                        // valid lexc when inserted, and with the human-readable
                        // gloss shown as grey tail text where one is known.
                        for (String tag : index.declaredTags()) {
                            String escaped = escapeTag(tag);
                            String gloss = io.github.alexeyev.morphingbird.common.ApertiumTagset.gloss(tag);
                            var element = LookupElementBuilder.create(escaped)
                                    .withPresentableText(tag)
                                    .withIcon(MorphingbirdIcons.TAG)
                                    .withTypeText("tag");
                            if (gloss != null) {
                                element = element.withTailText("  " + gloss, true);
                            }
                            result.addElement(element);
                        }

                        // Declared archiphonemes.
                        for (String arch : index.declaredArchiphonemes()) {
                            String escaped = escapeArch(arch);
                            result.addElement(LookupElementBuilder.create(escaped)
                                    .withPresentableText(arch)
                                    .withIcon(MorphingbirdIcons.TAG)
                                    .withTypeText("archiphoneme"));
                        }
                    }
                });
    }

    /** {@code <nom>} → {@code %<nom%>} (the form valid in lexc source). */
    private static String escapeTag(String canonical) {
        // canonical is "<nom>"; produce "%<nom%>"
        String inner = canonical.substring(1, canonical.length() - 1);
        return "%<" + inner + "%>";
    }

    private static String escapeArch(String canonical) {
        String inner = canonical.substring(1, canonical.length() - 1);
        return "%{" + inner + "%}";
    }
}
