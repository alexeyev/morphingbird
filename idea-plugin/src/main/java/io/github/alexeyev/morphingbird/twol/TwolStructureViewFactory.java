package io.github.alexeyev.morphingbird.twol;

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
import io.github.alexeyev.morphingbird.core.TwolModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.List;

/**
 * Structure view for a twol file: nodes for each named Set, Definition, and Rule,
 * built from the core {@link TwolModel}. Makes twol files navigable via the
 * structure popup (the same affordance lexc has).
 */
public final class TwolStructureViewFactory implements PsiStructureViewFactory {

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
        Model(@NotNull PsiFile file) {
            super(file, new Root(file));
        }
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
            TwolModel m = TwolModel.parse(file.getText());
            for (TwolModel.Named s : m.sets) {
                kids.add(new Item(file, s.name, s.start, "set", MorphingbirdIcons.TAG));
            }
            for (TwolModel.Named d : m.definitions) {
                kids.add(new Item(file, d.name, d.start, "definition", MorphingbirdIcons.TAG));
            }
            for (TwolModel.Rule r : m.rules) {
                kids.add(new Item(file, r.name, r.start, "rule", MorphingbirdIcons.RULE));
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
