package io.github.alexeyev.morphingbird.nav;

import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import io.github.alexeyev.morphingbird.common.MorphingbirdIcons;
import io.github.alexeyev.morphingbird.core.SymbolIndex;
import io.github.alexeyev.morphingbird.index.MorphingbirdIndexService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Go to Symbol (Ctrl/Cmd+Alt+Shift+N) support: lists every LEXICON in the
 * project so the user can fuzzy-jump to it by name. Backed by the cached
 * {@link SymbolIndex}.
 */
public final class MorphingbirdSymbolContributor implements ChooseByNameContributor {

    @Override
    public String @NotNull [] getNames(Project project, boolean includeNonProjectItems) {
        SymbolIndex index = MorphingbirdIndexService.getInstance(project).getIndex();
        if (index == null) return new String[0];
        Set<String> names = index.lexiconNames();
        return names.toArray(new String[0]);
    }

    @Override
    public NavigationItem @NotNull [] getItemsByName(String name, String pattern,
                                                     Project project,
                                                     boolean includeNonProjectItems) {
        SymbolIndex index = MorphingbirdIndexService.getInstance(project).getIndex();
        if (index == null) return new NavigationItem[0];
        SymbolIndex.Loc loc = index.lexiconDefinition(name);
        if (loc == null) return new NavigationItem[0];

        VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(loc.file);
        if (vf == null) return new NavigationItem[0];

        List<NavigationItem> out = new ArrayList<>();
        out.add(new LexiconNavItem(project, name, vf, loc.start));
        return out.toArray(new NavigationItem[0]);
    }

    /** A navigable Go-to-Symbol entry for a LEXICON. */
    private static final class LexiconNavItem implements NavigationItem {
        private final Project project;
        private final String name;
        private final VirtualFile file;
        private final int offset;

        LexiconNavItem(Project project, String name, VirtualFile file, int offset) {
            this.project = project; this.name = name; this.file = file; this.offset = offset;
        }

        @Override public @Nullable String getName() { return name; }

        @Override public @Nullable ItemPresentation getPresentation() {
            return new ItemPresentation() {
                @Override public @Nullable String getPresentableText() { return name; }
                @Override public @Nullable String getLocationString() {
                    return file.getName();
                }
                @Override public @Nullable Icon getIcon(boolean unused) {
                    return MorphingbirdIcons.LEXICON;
                }
            };
        }

        @Override public void navigate(boolean requestFocus) {
            PsiManager pm = PsiManager.getInstance(project);
            var psiFile = pm.findFile(file);
            if (psiFile == null) return;
            PsiElement leaf = psiFile.findElementAt(offset);
            if (leaf instanceof Navigatable && ((Navigatable) leaf).canNavigate()) {
                ((Navigatable) leaf).navigate(requestFocus);
            } else if (psiFile instanceof Navigatable) {
                ((Navigatable) psiFile).navigate(requestFocus);
            }
        }
        @Override public boolean canNavigate() { return true; }
        @Override public boolean canNavigateToSource() { return true; }
    }
}
