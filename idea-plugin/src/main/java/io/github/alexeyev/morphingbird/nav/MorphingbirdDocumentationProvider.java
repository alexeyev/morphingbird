package io.github.alexeyev.morphingbird.nav;

import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.psi.PsiElement;
import io.github.alexeyev.morphingbird.common.ApertiumTagset;
import io.github.alexeyev.morphingbird.core.SymbolIndex;
import io.github.alexeyev.morphingbird.index.MorphingbirdIndexService;
import io.github.alexeyev.morphingbird.cg3.Cg3TokenTypes;
import io.github.alexeyev.morphingbird.lexc.LexcTokenTypes;
import io.github.alexeyev.morphingbird.twol.TwolTokenTypes;
import io.github.alexeyev.morphingbird.udx.UdxTokenTypes;
import io.github.alexeyev.morphingbird.lexd.LexdTokenTypes;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Hover documentation across the Morphingbird languages. Turns cryptic symbols into
 * explanations: a tag like {@code <gen>} shows "Genitive case" plus where it is
 * declared and how many times it is used; an archiphoneme shows whether a twol
 * rule resolves it; a continuation class / set name shows where it is defined.
 *
 * <p>The gloss comes from {@link ApertiumTagset}; the counts and locations come
 * from the shared {@link SymbolIndex}, so the documentation reflects the whole
 * project, not just the current file.</p>
 */
public final class MorphingbirdDocumentationProvider extends AbstractDocumentationProvider {

    @Override
    public @Nullable String generateDoc(PsiElement element, @Nullable PsiElement original) {
        if (element == null) return null;
        var node = element.getNode();
        if (node == null) return null;
        var type = node.getElementType();
        String text = element.getText();
        if (text == null || text.isEmpty()) return null;

        SymbolIndex index =
                MorphingbirdIndexService.getInstance(element.getProject()).getIndex();

        // --- Tags (lexc/twol/cg3/udx/lexd) ---
        if (isTag(type)) {
            String canonical = canonicalTag(type, text);
            return tagDoc(canonical, index);
        }
        // --- Archiphonemes ---
        if (isArchiphoneme(type)) {
            String canonical = stripEscapes(text);
            return archiphonemeDoc(canonical, index);
        }
        // --- Continuation class / set / lexicon name ---
        if (isName(type)) {
            return nameDoc(text.trim(), index);
        }
        return null;
    }

    private static String tagDoc(String canonical, SymbolIndex index) {
        String inner = canonical.startsWith("<") && canonical.endsWith(">")
                ? canonical.substring(1, canonical.length() - 1) : canonical;
        StringBuilder sb = new StringBuilder();
        sb.append("<b>").append(escape(canonical)).append("</b>");
        String gloss = ApertiumTagset.gloss(canonical);
        if (gloss != null) {
            sb.append(" &mdash; ").append(escape(gloss));
        } else {
            sb.append(" &mdash; <i>tag</i>");
        }
        sb.append("<br/>");
        if (index != null) {
            int decls = index.tagDeclarations(canonical).size();
            int uses = index.tagUsages(canonical).size();
            sb.append("<br/>Declared in ").append(decls)
                    .append(decls == 1 ? " place" : " places");
            sb.append(", used ").append(uses)
                    .append(uses == 1 ? " time" : " times").append(".");
            if (decls == 0) {
                sb.append("<br/><span style=\"color:#C0392B\">Not declared in any "
                        + "Multichar_Symbols block.</span>");
            }
        }
        return sb.toString();
    }

    private static String archiphonemeDoc(String canonical, SymbolIndex index) {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>").append(escape(canonical)).append("</b> &mdash; "
                + "<i>archiphoneme</i> (morphophonological variable)<br/>");
        if (index != null) {
            int res = index.archiphonemeResolutions(canonical).size();
            int uses = index.archiphonemeUsages(canonical).size();
            sb.append("<br/>Used ").append(uses)
                    .append(uses == 1 ? " time" : " times").append(", ");
            if (res > 0) {
                sb.append("resolved by ").append(res)
                        .append(res == 1 ? " twol rule." : " twol rules.");
            } else {
                sb.append("<span style=\"color:#C0392B\">not resolved by any twol "
                        + "rule (may leak to the surface).</span>");
            }
        }
        return sb.toString();
    }

    private static String nameDoc(String name, SymbolIndex index) {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>").append(escape(name)).append("</b>");
        if (index == null) return sb.toString();

        SymbolIndex.Loc lex = index.lexiconDefinition(name);
        if (lex != null) {
            sb.append(" &mdash; <i>LEXICON</i><br/><br/>Defined in ")
                    .append(escape(shortFile(lex.file)))
                    .append(". Used as a continuation ")
                    .append(index.lexiconUsages(name).size()).append(" times.");
            return sb.toString();
        }
        SymbolIndex.Loc named = index.namedDefinition(name);
        if (named != null) {
            sb.append(" &mdash; <i>set / definition</i><br/><br/>Defined in ")
                    .append(escape(shortFile(named.file))).append(".");
            return sb.toString();
        }
        sb.append(" &mdash; <i>name</i>");
        return sb.toString();
    }

    // --- token classification helpers ---

    private static boolean isTag(com.intellij.psi.tree.IElementType t) {
        return t == LexcTokenTypes.TAG || t == Cg3TokenTypes.TAG
                || t == UdxTokenTypes.TAG || t == LexdTokenTypes.TAG;
    }

    private static boolean isArchiphoneme(com.intellij.psi.tree.IElementType t) {
        return t == LexcTokenTypes.ARCHIPHONEME || t == TwolTokenTypes.ARCHIPHONEME
                || t == LexdTokenTypes.ARCHIPHONEME;
    }

    private static boolean isName(com.intellij.psi.tree.IElementType t) {
        return t == LexcTokenTypes.IDENTIFIER || t == Cg3TokenTypes.SETNAME
                || t == TwolTokenTypes.IDENT || t == LexdTokenTypes.IDENT;
    }

    private static String canonicalTag(com.intellij.psi.tree.IElementType t, String text) {
        if (t == Cg3TokenTypes.TAG || t == UdxTokenTypes.TAG) {
            String s = text.trim();
            if (s.startsWith("<") && s.endsWith(">")) return s;
            return "<" + s + ">";
        }
        if (t == LexdTokenTypes.TAG) {
            String s = text.trim();
            return s.startsWith("<") ? s : "<" + s + ">";
        }
        // lexc: raw "%<nom%>"
        return stripEscapes(text);
    }

    private static String stripEscapes(String raw) {
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '%' && i + 1 < raw.length()) sb.append(raw.charAt(++i));
            else sb.append(c);
        }
        return sb.toString();
    }

    private static String shortFile(String path) {
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
