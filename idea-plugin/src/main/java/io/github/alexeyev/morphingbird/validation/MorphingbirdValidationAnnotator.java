package io.github.alexeyev.morphingbird.validation;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import io.github.alexeyev.morphingbird.core.SymbolIndex;
import io.github.alexeyev.morphingbird.index.MorphingbirdIndexService;
import org.jetbrains.annotations.NotNull;

/**
 * Tier-1 (compiler-free) validation: surfaces the {@link SymbolIndex}
 * consistency diagnostics inline. Runs once per file (keyed off the file PSI
 * root) and annotates the diagnostics whose location falls in this file.
 *
 * <p>These are pure cross-file graph findings — unresolved continuation
 * classes, tags used but never declared, archiphonemes never resolved by twol,
 * unreachable lexicons — none of which the slow compiler surfaces incrementally.
 * Per the plan, the parser never reds valid code; all squiggles originate here
 * and are therefore tunable.</p>
 */
public final class MorphingbirdValidationAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        // Only act once per file, on the file element.
        if (!(element instanceof PsiFile)) return;
        PsiFile file = (PsiFile) element;
        VirtualFile vf = file.getVirtualFile();
        if (vf == null) return;
        String fileId = vf.getPath();

        SymbolIndex index = MorphingbirdIndexService.getInstance(file.getProject()).getIndex();
        if (index == null) return;

        Document doc = FileDocumentManager.getInstance().getDocument(vf);
        int max = file.getTextLength();

        for (SymbolIndex.Diagnostic d : index.validate()) {
            if (d.loc == null || !fileId.equals(d.loc.file)) continue;
            int start = d.loc.start;
            int end = d.loc.end;
            if (start < 0 || end > max || start >= end) continue;

            HighlightSeverity sev =
                    d.severity == SymbolIndex.Diagnostic.Severity.ERROR
                            ? HighlightSeverity.ERROR
                            : HighlightSeverity.WARNING;

            var builder = holder.newAnnotation(sev, d.message)
                    .range(new TextRange(start, end));

            // Offer a targeted quick-fix where one makes sense.
            if (d.kind == SymbolIndex.Diagnostic.Kind.UNDECLARED_TAG
                    && d.symbol != null) {
                builder = builder.withFix(new AddTagDeclarationFix(d.symbol));
            }

            builder.create();
        }
    }
}
