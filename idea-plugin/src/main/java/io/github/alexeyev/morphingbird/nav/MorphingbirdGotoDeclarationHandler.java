package io.github.alexeyev.morphingbird.nav;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import io.github.alexeyev.morphingbird.core.SymbolIndex;
import io.github.alexeyev.morphingbird.index.MorphingbirdIndexService;
import io.github.alexeyev.morphingbird.lexc.LexcTokenTypes;
import io.github.alexeyev.morphingbird.twol.TwolTokenTypes;
import io.github.alexeyev.morphingbird.cg3.Cg3TokenTypes;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Cross-file, cross-DSL go-to-declaration. Resolves, depending on what the caret
 * sits on:
 * <ul>
 *   <li>a continuation-class reference (lexc IDENTIFIER) → its {@code LEXICON};</li>
 *   <li>a tag ({@code <nom>}) → its declaration in {@code Multichar_Symbols};</li>
 *   <li>an archiphoneme ({@code {G}}) → the twol rule(s) resolving it — the
 *       cross-DSL jump that nothing else provides.</li>
 * </ul>
 *
 * <p>All resolution goes through the cached {@link SymbolIndex}; this handler
 * only translates the index's {@link SymbolIndex.Loc} results into navigable
 * PSI targets.</p>
 */
public final class MorphingbirdGotoDeclarationHandler implements GotoDeclarationHandler {

    @Override
    public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement source,
                                                             int offset,
                                                             Editor editor) {
        if (source == null) return null;
        Project project = source.getProject();
        SymbolIndex index = MorphingbirdIndexService.getInstance(project).getIndex();
        if (index == null) return null;

        var type = source.getNode() != null ? source.getNode().getElementType() : null;
        String text = source.getText();
        if (text == null || text.isEmpty()) return null;

        List<SymbolIndex.Loc> targets = new ArrayList<>();

        // ---- lexc tokens ----
        if (type == LexcTokenTypes.TAG) {
            // PSI text for a lexc TAG is the raw "%<nom%>"; strip escapes.
            String canonical = stripEscapes(text);
            targets.addAll(index.tagDeclarations(canonical));
        } else if (type == LexcTokenTypes.ARCHIPHONEME) {
            String canonical = stripEscapes(text);
            // Cross-DSL: jump to the twol rule(s) that resolve this archiphoneme.
            targets.addAll(index.archiphonemeResolutions(canonical));
            if (targets.isEmpty()) {
                targets.addAll(index.archiphonemeDeclarations(canonical));
            }
        } else if (type == LexcTokenTypes.IDENTIFIER) {
            // Could be a continuation class (→ LEXICON) or a named def reference.
            SymbolIndex.Loc lex = index.lexiconDefinition(text);
            if (lex != null) {
                targets.add(lex);
            } else {
                SymbolIndex.Loc named = index.namedDefinition(text);
                if (named != null) targets.add(named);
            }
        }
        // ---- twol tokens ----
        else if (type == TwolTokenTypes.ARCHIPHONEME) {
            // From a twol Alphabet/rule archiphoneme %{G%}: jump to where lexc
            // declares it (the reverse of the lexc→twol jump above).
            String canonical = stripEscapes(text);
            targets.addAll(index.archiphonemeDeclarations(canonical));
            if (targets.isEmpty()) targets.addAll(index.archiphonemeResolutions(canonical));
        } else if (type == TwolTokenTypes.IDENT) {
            // A twol Set name reference → its definition.
            SymbolIndex.Loc named = index.namedDefinition(text);
            if (named != null) targets.add(named);
        }
        // ---- CG3 tokens ----
        else if (type == Cg3TokenTypes.TAG) {
            // A CG3 tag (bare "nom" or "<nom>") → its lexc declaration; this is
            // the high-value CG3→lexc jump. Normalise to the <tag> canonical.
            String canonical = normalizeCg3Tag(text);
            targets.addAll(index.tagDeclarations(canonical));
        } else if (type == Cg3TokenTypes.SETNAME) {
            // A CG3 LIST/SET reference → its definition.
            SymbolIndex.Loc named = index.namedDefinition(stripLeadingAt(text));
            if (named != null) targets.add(named);
        }

        if (targets.isEmpty()) return null;
        return toPsi(project, targets);
    }

    /** CG3 tags appear bare ({@code nom}) or bracketed ({@code <nom>}); → {@code <nom>}. */
    private static String normalizeCg3Tag(String raw) {
        String t = raw.trim();
        if (t.startsWith("<") && t.endsWith(">")) return t;
        return "<" + t + ">";
    }

    /** CG3 set names may carry a leading {@code @} (mapping prefix); strip it. */
    private static String stripLeadingAt(String s) {
        int i = 0;
        while (i < s.length() && s.charAt(i) == '@') i++;
        return s.substring(i);
    }

    /** Strips lexc {@code %} escapes from a raw symbol like {@code %<nom%>}. */
    private static String stripEscapes(String raw) {
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '%' && i + 1 < raw.length()) {
                sb.append(raw.charAt(++i));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static PsiElement[] toPsi(Project project, List<SymbolIndex.Loc> locs) {
        List<PsiElement> out = new ArrayList<>();
        PsiManager pm = PsiManager.getInstance(project);
        for (SymbolIndex.Loc loc : locs) {
            VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(loc.file);
            if (vf == null) continue;
            var psiFile = pm.findFile(vf);
            if (psiFile == null) continue;
            PsiElement leaf = psiFile.findElementAt(loc.start);
            out.add(leaf != null ? leaf : psiFile);
        }
        return out.isEmpty() ? null : out.toArray(PsiElement.EMPTY_ARRAY);
    }
}
