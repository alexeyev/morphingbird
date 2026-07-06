package io.github.alexeyev.morphingbird.cg3;

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
 * Index-driven completion for CG3. Offers:
 * <ul>
 *   <li>the morphological <b>tags</b> the analyser actually emits (as bare CG3
 *       tokens like {@code nom}), drawn from the shared symbol graph — so a CG3
 *       rule can only reference tags that exist on the lexc side;</li>
 *   <li>the project's <b>LIST / SET</b> names, for set references.</li>
 * </ul>
 */
public final class Cg3CompletionContributor extends CompletionContributor {

    public Cg3CompletionContributor() {
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

                        // Tags — presented as bare CG3 tokens (strip <>), since
                        // that is how CG3 lists reference them.
                        for (String tag : index.allTags()) {
                            String bare = bare(tag);
                            if (bare.isEmpty()) continue;
                            String gloss = io.github.alexeyev.morphingbird.common.ApertiumTagset.gloss(tag);
                            var element = LookupElementBuilder.create(bare)
                                    .withIcon(MorphingbirdIcons.TAG)
                                    .withTypeText("tag");
                            if (gloss != null) {
                                element = element.withTailText("  " + gloss, true);
                            }
                            result.addElement(element);
                        }

                        // LIST / SET names.
                        for (String name : index.namedDefinitionNames()) {
                            result.addElement(LookupElementBuilder.create(name)
                                    .withIcon(MorphingbirdIcons.RULE)
                                    .withTypeText("set"));
                        }
                    }
                });
    }

    /** {@code <nom>} → {@code nom}. */
    private static String bare(String canonical) {
        String t = canonical;
        if (t.startsWith("<")) t = t.substring(1);
        if (t.endsWith(">")) t = t.substring(0, t.length() - 1);
        return t;
    }
}
