package io.github.alexeyev.morphingbird.refs;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import io.github.alexeyev.morphingbird.core.SymbolIndex;
import io.github.alexeyev.morphingbird.index.MorphingbirdIndexService;
import io.github.alexeyev.morphingbird.lexc.LexcTokenTypes;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Renames a LEXICON across the whole project: the declaration and every
 * continuation reference, in one undoable command. Implemented as an action
 * (rather than a {@code RenamePsiElementProcessor}) because the plugin uses a
 * flat PSI backed by the {@link SymbolIndex}; the index already knows every edit
 * site precisely (verified: offsets land exactly on the name text), so a
 * direct multi-file text refactor is correct and avoids inventing PSI structure.
 *
 * <p>Registered on the editor popup; enabled only when the caret sits on a lexc
 * IDENTIFIER that names a LEXICON.</p>
 */
public final class MorphingbirdRenameLexiconAction
        extends com.intellij.openapi.actionSystem.AnAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(targetLexicon(e) != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        String name = targetLexicon(e);
        if (project == null || name == null) return;

        SymbolIndex index = MorphingbirdIndexService.getInstance(project).getIndex();
        if (index == null) return;

        String newName = Messages.showInputDialog(project,
                "Rename LEXICON '" + name + "' to:", "Rename Apertium Lexicon",
                Messages.getQuestionIcon(), name, new LexiconNameValidator(index, name));
        if (newName == null || newName.isBlank() || newName.equals(name)) return;

        Map<String, List<SymbolIndex.Loc>> sites = index.lexiconRenameSites(name);
        if (sites.isEmpty()) return;

        WriteCommandAction.runWriteCommandAction(project,
                "Rename Apertium Lexicon", null, () -> {
                    for (Map.Entry<String, List<SymbolIndex.Loc>> entry : sites.entrySet()) {
                        VirtualFile vf = LocalFileSystem.getInstance()
                                .findFileByPath(entry.getKey());
                        if (vf == null) continue;
                        Document doc = FileDocumentManager.getInstance().getDocument(vf);
                        if (doc == null) continue;
                        // entry value is pre-sorted descending by start offset, so
                        // each replace leaves earlier offsets valid.
                        for (SymbolIndex.Loc loc : entry.getValue()) {
                            if (loc.start >= 0 && loc.end <= doc.getTextLength()) {
                                doc.replaceString(loc.start, loc.end, newName);
                            }
                        }
                        PsiDocumentManager.getInstance(project).commitDocument(doc);
                    }
                });
    }

    /** The LEXICON name under the caret, or null if not applicable. */
    private static String targetLexicon(AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
        if (project == null || editor == null || file == null) return null;
        int offset = editor.getCaretModel().getOffset();
        PsiElement el = file.findElementAt(offset);
        if (el == null || el.getNode() == null) return null;
        if (el.getNode().getElementType() != LexcTokenTypes.IDENTIFIER) return null;
        String text = el.getText();
        if (text == null || text.isEmpty()) return null;
        SymbolIndex index = MorphingbirdIndexService.getInstance(project).getIndex();
        if (index == null) return null;
        return index.lexiconDefinition(text) != null ? text : null;
    }

    /** Rejects names that collide with an existing LEXICON or are malformed. */
    private static final class LexiconNameValidator
            implements com.intellij.openapi.ui.InputValidator {
        private final SymbolIndex index;
        private final String original;

        LexiconNameValidator(SymbolIndex index, String original) {
            this.index = index;
            this.original = original;
        }

        @Override
        public boolean checkInput(String input) {
            if (input == null || input.isBlank()) return false;
            if (input.equals(original)) return true;
            // No whitespace, no lexc structural chars.
            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);
                if (Character.isWhitespace(c) || c == ':' || c == ';') return false;
            }
            // Must not collide with an existing LEXICON.
            return index.lexiconDefinition(input) == null;
        }

        @Override
        public boolean canClose(String input) {
            return checkInput(input);
        }
    }
}
