package io.github.alexeyev.morphingbird.twol;

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
 * Index-driven completion for twol. Offers:
 * <ul>
 *   <li>the <b>archiphonemes</b> declared on the lexc side (as {@code %{G%}}),
 *       so an Alphabet pair or rule references a real archiphoneme;</li>
 *   <li>the project's named <b>Sets / Definitions</b>.</li>
 * </ul>
 */
public final class TwolCompletionContributor extends CompletionContributor {

    public TwolCompletionContributor() {
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

                        // Archiphonemes — presented with twol/lexc % escapes.
                        for (String arch : index.declaredArchiphonemes()) {
                            String escaped = escapeArch(arch);
                            result.addElement(LookupElementBuilder.create(escaped)
                                    .withPresentableText(arch)
                                    .withIcon(MorphingbirdIcons.TAG)
                                    .withTypeText("archiphoneme"));
                        }

                        // Named Sets / Definitions.
                        for (String name : index.namedDefinitionNames()) {
                            result.addElement(LookupElementBuilder.create(name)
                                    .withIcon(MorphingbirdIcons.RULE)
                                    .withTypeText("set"));
                        }
                    }
                });
    }

    /** {@code {G}} → {@code %{G%}}. */
    private static String escapeArch(String canonical) {
        String inner = canonical.substring(1, canonical.length() - 1);
        return "%{" + inner + "%}";
    }
}
