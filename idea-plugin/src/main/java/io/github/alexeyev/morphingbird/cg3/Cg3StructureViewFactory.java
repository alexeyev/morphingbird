package io.github.alexeyev.morphingbird.cg3;

import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewModelBase;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder;
import com.intellij.ide.util.treeView.smartTree.Sorter;
import com.intellij.lang.PsiStructureViewFactory;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import io.github.alexeyev.morphingbird.common.MorphingbirdIcons;
import io.github.alexeyev.morphingbird.core.Cg3Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.List;

/**
 * Structure view for a CG3 file: one node per LIST / SET definition (and the
 * section markers), built from the core {@link Cg3Model}. Makes CG3 grammars
 * navigable via the structure popup.
 */
public final class Cg3StructureViewFactory implements PsiStructureViewFactory {

    @Override
    public @Nullable StructureViewBuilder getStructureViewBuilder(@NotNull PsiFile psiFile) {
        return new TreeBasedStructureViewBuilder() {
            @Override
            public @NotNull StructureViewModel createStructureViewModel(@Nullable Editor editor) {
                return new Model(psiFile);
            }
        };
    }

    static final class Model extends StructureViewModelBase
            implements StructureViewModel.ElementInfoProvider {
        Model(@NotNull PsiFile file) { super(file, new Root(file)); }
        @Override public Sorter @NotNull [] getSorters() {
            return new Sorter[]{Sorter.ALPHA_SORTER};
        }
        @Override public boolean isAlwaysShowsPlus(StructureViewTreeElement e) { return false; }
        @Override public boolean isAlwaysLeaf(StructureViewTreeElement e) {
            return e instanceof Item;
        }
    }

    static final class Root implements StructureViewTreeElement {
        private final PsiFile file;
        Root(PsiFile file) { this.file = file; }
        @Override public Object getValue() { return file; }
        @Override public @NotNull ItemPresentation getPresentation() {
            return present(file.getName(), MorphingbirdIcons.FILE);
        }
        @Override public StructureViewTreeElement @NotNull [] getChildren() {
            List<StructureViewTreeElement> kids = new ArrayList<>();
            Cg3Model m = Cg3Model.parse(file.getText());
            for (Cg3Model.Def d : m.definitions) {
                kids.add(new Item(file, (d.isList ? "LIST " : "SET ") + d.name,
                        d.start, d.isList ? "list" : "set", MorphingbirdIcons.TAG));
            }
            for (io.github.alexeyev.morphingbird.core.LexcModel.SymbolRef s : m.sections) {
                kids.add(new Item(file, "SECTION", s.start, "section", MorphingbirdIcons.RULE));
            }
            return kids.toArray(StructureViewTreeElement.EMPTY_ARRAY);
        }
        @Override public void navigate(boolean req) {
            if (file instanceof com.intellij.pom.Navigatable)
                ((com.intellij.pom.Navigatable) file).navigate(req);
        }
        @Override public boolean canNavigate() { return true; }
        @Override public boolean canNavigateToSource() { return true; }
    }

    static final class Item implements StructureViewTreeElement {
        private final PsiFile file;
        private final String name;
        private final int offset;
        private final String type;
        private final Icon icon;
        Item(PsiFile file, String name, int offset, String type, Icon icon) {
            this.file = file; this.name = name; this.offset = offset;
            this.type = type; this.icon = icon;
        }
        @Override public Object getValue() { return name + "@" + offset; }
        @Override public @NotNull ItemPresentation getPresentation() {
            return present(name, icon);
        }
        @Override public StructureViewTreeElement @NotNull [] getChildren() {
            return StructureViewTreeElement.EMPTY_ARRAY;
        }
        @Override public void navigate(boolean req) {
            PsiElement leaf = file.findElementAt(offset);
            if (leaf instanceof com.intellij.pom.Navigatable)
                ((com.intellij.pom.Navigatable) leaf).navigate(req);
        }
        @Override public boolean canNavigate() { return true; }
        @Override public boolean canNavigateToSource() { return true; }
    }

    private static ItemPresentation present(String text, Icon icon) {
        return new ItemPresentation() {
            @Override public @Nullable String getPresentableText() { return text; }
            @Override public @Nullable Icon getIcon(boolean unused) { return icon; }
        };
    }
}
