package io.github.alexeyev.morphingbird.refs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.PsiElementResolveResult;
import io.github.alexeyev.morphingbird.core.SymbolIndex;
import io.github.alexeyev.morphingbird.index.MorphingbirdIndexService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A cross-file reference from a lexc token to its declaration(s), resolved
 * through the cached {@link SymbolIndex}. Implementing a real
 * {@link PsiPolyVariantReference} (rather than only a goto handler) is what lets
 * the platform's Find Usages, reference highlighting, and Rename work for free.
 *
 * <p>The reference kind is decided by the token: a continuation-class IDENTIFIER
 * resolves to its {@code LEXICON}; a TAG to its {@code Multichar_Symbols}
 * declaration; an ARCHIPHONEME to the twol rule(s) that resolve it (the
 * cross-DSL jump). Targets are the leaf PSI elements at the index offsets.</p>
 */
public final class MorphingbirdSymbolReference extends PsiReferenceBase<PsiElement>
        implements PsiPolyVariantReference {

    /** What the referenced token denotes. */
    public enum Kind { CONTINUATION, TAG, ARCHIPHONEME, NAMED }

    private final Kind kind;
    private final String key;     // canonical key into the index

    public MorphingbirdSymbolReference(@NotNull PsiElement element, TextRange rangeInElement,
                                   Kind kind, String key) {
        super(element, rangeInElement);
        this.kind = kind;
        this.key = key;
    }

    @Override
    public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
        Project project = getElement().getProject();
        SymbolIndex index = MorphingbirdIndexService.getInstance(project).getIndex();
        if (index == null) return ResolveResult.EMPTY_ARRAY;

        List<SymbolIndex.Loc> locs = new ArrayList<>();
        switch (kind) {
            case CONTINUATION:
                SymbolIndex.Loc def = index.lexiconDefinition(key);
                if (def != null) locs.add(def);
                break;
            case TAG:
                locs.addAll(index.tagDeclarations(key));
                break;
            case ARCHIPHONEME:
                locs.addAll(index.archiphonemeResolutions(key));
                if (locs.isEmpty()) locs.addAll(index.archiphonemeDeclarations(key));
                break;
            case NAMED:
                SymbolIndex.Loc named = index.namedDefinition(key);
                if (named != null) locs.add(named);
                break;
        }
        if (locs.isEmpty()) return ResolveResult.EMPTY_ARRAY;

        PsiManager pm = PsiManager.getInstance(project);
        List<ResolveResult> results = new ArrayList<>();
        for (SymbolIndex.Loc loc : locs) {
            VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(loc.file);
            if (vf == null) continue;
            var psiFile = pm.findFile(vf);
            if (psiFile == null) continue;
            PsiElement leaf = psiFile.findElementAt(loc.start);
            results.add(new PsiElementResolveResult(leaf != null ? leaf : psiFile));
        }
        return results.toArray(ResolveResult.EMPTY_ARRAY);
    }

    @Override
    public @Nullable PsiElement resolve() {
        ResolveResult[] r = multiResolve(false);
        return r.length == 1 ? r[0].getElement() : null;
    }

    @Override
    public Object @NotNull [] getVariants() {
        // Completion is handled by LexcCompletionContributor; no ref variants.
        return EMPTY_ARRAY;
    }
}
