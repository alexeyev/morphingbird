package io.github.alexeyev.morphingbird.validation;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import io.github.alexeyev.morphingbird.core.SymbolIndex;
import io.github.alexeyev.morphingbird.index.MorphingbirdIndexService;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Quick-fix for an "undeclared tag" diagnostic: inserts the tag into a lexc
 * {@code Multichar_Symbols} block. The tag is written in lexc escaped form
 * ({@code %<nom%>}) at the end of the principal lexicon's symbol block, and that
 * file is opened so the user sees the change land.
 *
 * <p>If the principal lexc file differs from the one the squiggle is in (e.g.
 * the tag was flagged in a CG3 grammar), the fix still targets the lexc, since
 * {@code Multichar_Symbols} only lives there.</p>
 */
public final class AddTagDeclarationFix implements IntentionAction {

    private final String canonicalTag;   // e.g. "<nom>"

    public AddTagDeclarationFix(String canonicalTag) {
        this.canonicalTag = canonicalTag;
    }

    @Override
    public @NotNull String getText() {
        return "Declare tag " + canonicalTag + " in Multichar_Symbols";
    }

    @Override
    public @NotNull String getFamilyName() {
        return "Morphingbird";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        SymbolIndex index = MorphingbirdIndexService.getInstance(project).getIndex();
        return index != null && index.primaryMulticharFile() != null;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) {
        SymbolIndex index = MorphingbirdIndexService.getInstance(project).getIndex();
        if (index == null) return;
        String targetPath = index.primaryMulticharFile();
        if (targetPath == null) return;
        int offset = index.multicharInsertOffset(targetPath);
        if (offset < 0) return;

        VirtualFile vf = LocalFileSystem.getInstance().findFileByIoFile(new File(targetPath));
        if (vf == null) return;

        // The inner name without brackets, escaped for lexc: <nom> -> %<nom%>.
        String inner = canonicalTag;
        if (inner.startsWith("<") && inner.endsWith(">")) {
            inner = inner.substring(1, inner.length() - 1);
        }
        String insertion = "\n%<" + inner + "%>";

        WriteCommandAction.runWriteCommandAction(project, getText(), getFamilyName(), () -> {
            Document doc = FileDocumentManager.getInstance().getDocument(vf);
            if (doc == null) return;
            int at = Math.min(offset, doc.getTextLength());
            doc.insertString(at, insertion);
            PsiDocumentManager.getInstance(project).commitDocument(doc);
        });

        // Reveal the change.
        new OpenFileDescriptor(project, vf, Math.min(offset + 1, (int) vf.getLength()))
                .navigate(true);
    }

    @Override
    public boolean startInWriteAction() {
        // We manage our own write action (and navigate afterwards).
        return false;
    }
}
