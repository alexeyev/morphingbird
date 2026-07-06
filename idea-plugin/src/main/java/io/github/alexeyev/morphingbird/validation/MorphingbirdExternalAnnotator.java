package io.github.alexeyev.morphingbird.validation;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import io.github.alexeyev.morphingbird.core.CompilerRunner;
import org.jetbrains.annotations.Nullable;

/**
 * Tier-2 validation: compiles the current file with the real toolchain
 * ({@code hfst-lexc} / {@code hfst-twolc} / {@code cg-comp}) on a background
 * thread and surfaces the compiler's own diagnostics inline. Complements the
 * instant Tier-1 index checks (which catch cross-file issues the compiler does
 * not, e.g. missing continuation classes — verified that hfst-lexc does not flag
 * those), giving the editor both fast structural feedback and ground-truth from
 * the compiler.
 *
 * <p>Runs only when the toolchain is present; if not found it stays silent
 * rather than nagging. Compilation happens in an isolated temp dir
 * (Refalcon's pattern), so an in-progress buffer never disturbs the real build.</p>
 */
public final class MorphingbirdExternalAnnotator
        extends ExternalAnnotator<MorphingbirdExternalAnnotator.Info,
                                  MorphingbirdExternalAnnotator.Diags> {

    /** Collected on the EDT: what to compile. */
    public static final class Info {
        final CompilerRunner.Kind kind;
        final String fileName;
        final String text;
        Info(CompilerRunner.Kind kind, String fileName, String text) {
            this.kind = kind; this.fileName = fileName; this.text = text;
        }
    }

    /** Produced off the EDT: the compiler result. */
    public static final class Diags {
        final CompilerRunner.Result result;
        Diags(CompilerRunner.Result result) { this.result = result; }
    }

    @Override
    public @Nullable Info collectInformation(@org.jetbrains.annotations.NotNull PsiFile file,
                                             @org.jetbrains.annotations.NotNull Editor editor,
                                             boolean hasErrors) {
        return collectInformation(file);
    }

    @Override
    public @Nullable Info collectInformation(@org.jetbrains.annotations.NotNull PsiFile file) {
        VirtualFile vf = file.getVirtualFile();
        if (vf == null) return null;
        CompilerRunner.Kind kind = CompilerRunner.kindForFile(vf.getName());
        if (kind == null) return null;
        return new Info(kind, vf.getName(), file.getText());
    }

    @Override
    public @Nullable Diags doAnnotate(Info info) {
        if (info == null) return null;
        // The slow part — runs off the EDT by contract.
        CompilerRunner.Result r =
                CompilerRunner.compile(info.kind, info.fileName, info.text, null);
        return new Diags(r);
    }

    @Override
    public void apply(@org.jetbrains.annotations.NotNull PsiFile file, Diags annotationResult,
                      @org.jetbrains.annotations.NotNull AnnotationHolder holder) {
        if (annotationResult == null || !annotationResult.result.toolAvailable) return;
        VirtualFile vf = file.getVirtualFile();
        if (vf == null) return;
        Document doc = FileDocumentManager.getInstance().getDocument(vf);
        if (doc == null) return;
        int maxLine = doc.getLineCount();

        for (CompilerRunner.Diag d : annotationResult.result.diagnostics) {
            TextRange range = rangeFor(doc, d, maxLine);
            if (range == null) continue;
            HighlightSeverity sev =
                    d.severity == CompilerRunner.Diag.Severity.WARNING
                            ? HighlightSeverity.WARNING : HighlightSeverity.ERROR;
            holder.newAnnotation(sev,
                            annotationResult.result.toolName + ": " + d.message)
                    .range(range)
                    .create();
        }
    }

    /** Maps a 1-based line/col diagnostic to a document text range. */
    private static @Nullable TextRange rangeFor(Document doc, CompilerRunner.Diag d,
                                                int maxLine) {
        if (d.line <= 0 || d.line > maxLine) {
            return null;  // unknown position: skip rather than mis-place
        }
        int lineIdx = d.line - 1;
        int lineStart = doc.getLineStartOffset(lineIdx);
        int lineEnd = doc.getLineEndOffset(lineIdx);
        int start = lineStart;
        if (d.column > 0 && lineStart + (d.column - 1) <= lineEnd) {
            start = lineStart + (d.column - 1);
        }
        // Highlight from the position to end of line (compiler cols are coarse).
        if (start >= lineEnd) start = Math.max(lineStart, lineEnd - 1);
        return new TextRange(start, lineEnd);
    }
}
