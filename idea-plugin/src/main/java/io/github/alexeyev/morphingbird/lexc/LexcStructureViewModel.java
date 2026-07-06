package io.github.alexeyev.morphingbird.lexc;

import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewModelBase;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.Sorter;
import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import io.github.alexeyev.morphingbird.common.MorphingbirdIcons;
import io.github.alexeyev.morphingbird.core.LexcModel;
import io.github.alexeyev.morphingbird.core.LexcParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.List;

/**
 * Structure view for a lexc file: one node per {@code LEXICON}, with its entry
 * count. Built from the core {@link LexcParser} so it stays consistent with the
 * index. In an 18.5k-line lexc this is the primary way to navigate.
 */
public final class LexcStructureViewModel extends StructureViewModelBase
        implements StructureViewModel.ElementInfoProvider {

    public LexcStructureViewModel(@NotNull PsiFile psiFile) {
        super(psiFile, new LexcFileElement(psiFile));
    }

    @Override
    public Sorter @NotNull [] getSorters() {
        return new Sorter[]{Sorter.ALPHA_SORTER};
    }

    @Override
    public boolean isAlwaysShowsPlus(StructureViewTreeElement element) {
        return false;
    }

    @Override
    public boolean isAlwaysLeaf(StructureViewTreeElement element) {
        return element instanceof LexiconElement;
    }

    /** Root node: the file, whose children are the LEXICONs. */
    static final class LexcFileElement implements StructureViewTreeElement {
        private final PsiFile file;

        LexcFileElement(PsiFile file) { this.file = file; }

        @Override public Object getValue() { return file; }

        @Override public @NotNull ItemPresentation getPresentation() {
            return new ItemPresentation() {
                @Override public @Nullable String getPresentableText() {
                    return file.getName();
                }
                @Override public @Nullable Icon getIcon(boolean unused) {
                    return MorphingbirdIcons.FILE;
                }
            };
        }

        @Override public StructureViewTreeElement @NotNull [] getChildren() {
            List<StructureViewTreeElement> kids = new ArrayList<>();
            LexcModel m = LexcParser.parse(file.getText());
            for (LexcModel.Lexicon lex : m.lexicons) {
                kids.add(new LexiconElement(file, lex));
            }
            return kids.toArray(StructureViewTreeElement.EMPTY_ARRAY);
        }

        @Override public void navigate(boolean requestFocus) {
            if (file instanceof com.intellij.pom.Navigatable) {
                ((com.intellij.pom.Navigatable) file).navigate(requestFocus);
            }
        }
        @Override public boolean canNavigate() { return true; }
        @Override public boolean canNavigateToSource() { return true; }
    }

    /** A LEXICON node; navigates to the LEXICON name offset. */
    static final class LexiconElement implements StructureViewTreeElement {
        private final PsiFile file;
        private final LexcModel.Lexicon lex;

        LexiconElement(PsiFile file, LexcModel.Lexicon lex) {
            this.file = file; this.lex = lex;
        }

        @Override public Object getValue() { return lex.name; }

        @Override public @NotNull ItemPresentation getPresentation() {
            return new ItemPresentation() {
                @Override public @Nullable String getPresentableText() {
                    return lex.name + "  (" + lex.entries.size() + ")";
                }
                @Override public @Nullable Icon getIcon(boolean unused) {
                    return MorphingbirdIcons.LEXICON;
                }
            };
        }

        @Override public StructureViewTreeElement @NotNull [] getChildren() {
            return StructureViewTreeElement.EMPTY_ARRAY;
        }

        @Override public void navigate(boolean requestFocus) {
            PsiElement leaf = file.findElementAt(lex.nameStart);
            if (leaf instanceof com.intellij.pom.Navigatable) {
                ((com.intellij.pom.Navigatable) leaf).navigate(requestFocus);
            }
        }
        @Override public boolean canNavigate() { return true; }
        @Override public boolean canNavigateToSource() { return true; }
    }
}
