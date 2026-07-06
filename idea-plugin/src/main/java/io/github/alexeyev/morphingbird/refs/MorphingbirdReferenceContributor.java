package io.github.alexeyev.morphingbird.refs;

import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;
import io.github.alexeyev.morphingbird.cg3.Cg3Language;
import io.github.alexeyev.morphingbird.cg3.Cg3TokenTypes;
import io.github.alexeyev.morphingbird.lexc.LexcLanguage;
import io.github.alexeyev.morphingbird.lexc.LexcTokenTypes;
import io.github.alexeyev.morphingbird.twol.TwolLanguage;
import io.github.alexeyev.morphingbird.twol.TwolTokenTypes;
import org.jetbrains.annotations.NotNull;

/**
 * Attaches {@link MorphingbirdSymbolReference}s to tokens in all three apertium
 * languages, so the platform's Find Usages, Rename, and reference highlighting
 * work - and work <em>across</em> DSLs. Because a tag is one logical symbol
 * whether it appears as lexc {@code %<nom%>} or CG3 {@code nom}, references from
 * every language resolve to the same canonical declaration, which is what lets
 * Find Usages on a tag list its lexc and CG3 sites together.
 *
 * <p>Per-language token to reference mapping:</p>
 * <ul>
 *   <li><b>lexc</b>: IDENTIFIER->continuation (->LEXICON), TAG->tag,
 *       ARCHIPHONEME->archiphoneme.</li>
 *   <li><b>twol</b>: ARCHIPHONEME->archiphoneme, IDENT->named-def (Set).</li>
 *   <li><b>CG3</b>: TAG->tag (bare {@code nom} normalised to {@code <nom>}),
 *       SETNAME->named-def.</li>
 * </ul>
 */
public final class MorphingbirdReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(
                PlatformPatterns.psiElement().withLanguage(LexcLanguage.INSTANCE),
                new LexcRefProvider());
        registrar.registerReferenceProvider(
                PlatformPatterns.psiElement().withLanguage(TwolLanguage.INSTANCE),
                new TwolRefProvider());
        registrar.registerReferenceProvider(
                PlatformPatterns.psiElement().withLanguage(Cg3Language.INSTANCE),
                new Cg3RefProvider());
        registrar.registerReferenceProvider(
                PlatformPatterns.psiElement().withLanguage(
                        io.github.alexeyev.morphingbird.udx.UdxLanguage.INSTANCE),
                new UdxRefProvider());
        registrar.registerReferenceProvider(
                PlatformPatterns.psiElement().withLanguage(
                        io.github.alexeyev.morphingbird.lexd.LexdLanguage.INSTANCE),
                new LexdRefProvider());
    }

    // --- lexc --------------------------------------------------------------

    private static final class LexcRefProvider extends PsiReferenceProvider {
        @Override
        public PsiReference @NotNull [] getReferencesByElement(
                @NotNull PsiElement element, @NotNull ProcessingContext context) {
            var t = typeOf(element);
            String text = element.getText();
            if (t == null || text == null || text.isEmpty()) return PsiReference.EMPTY_ARRAY;
            TextRange whole = new TextRange(0, text.length());

            if (t == LexcTokenTypes.IDENTIFIER) {
                return ref(element, whole, MorphingbirdSymbolReference.Kind.CONTINUATION, text);
            }
            if (t == LexcTokenTypes.TAG) {
                return ref(element, whole, MorphingbirdSymbolReference.Kind.TAG, stripEscapes(text));
            }
            if (t == LexcTokenTypes.ARCHIPHONEME) {
                return ref(element, whole, MorphingbirdSymbolReference.Kind.ARCHIPHONEME, stripEscapes(text));
            }
            return PsiReference.EMPTY_ARRAY;
        }
    }

    // --- twol --------------------------------------------------------------

    private static final class TwolRefProvider extends PsiReferenceProvider {
        @Override
        public PsiReference @NotNull [] getReferencesByElement(
                @NotNull PsiElement element, @NotNull ProcessingContext context) {
            var t = typeOf(element);
            String text = element.getText();
            if (t == null || text == null || text.isEmpty()) return PsiReference.EMPTY_ARRAY;
            TextRange whole = new TextRange(0, text.length());

            if (t == TwolTokenTypes.ARCHIPHONEME) {
                return ref(element, whole, MorphingbirdSymbolReference.Kind.ARCHIPHONEME, stripEscapes(text));
            }
            if (t == TwolTokenTypes.IDENT) {
                return ref(element, whole, MorphingbirdSymbolReference.Kind.NAMED, text);
            }
            return PsiReference.EMPTY_ARRAY;
        }
    }

    // --- CG3 ---------------------------------------------------------------

    private static final class Cg3RefProvider extends PsiReferenceProvider {
        @Override
        public PsiReference @NotNull [] getReferencesByElement(
                @NotNull PsiElement element, @NotNull ProcessingContext context) {
            var t = typeOf(element);
            String text = element.getText();
            if (t == null || text == null || text.isEmpty()) return PsiReference.EMPTY_ARRAY;
            TextRange whole = new TextRange(0, text.length());

            if (t == Cg3TokenTypes.TAG) {
                return ref(element, whole, MorphingbirdSymbolReference.Kind.TAG, normalizeCg3Tag(text));
            }
            if (t == Cg3TokenTypes.SETNAME) {
                return ref(element, whole, MorphingbirdSymbolReference.Kind.NAMED, stripLeadingAt(text));
            }
            return PsiReference.EMPTY_ARRAY;
        }
    }

    // --- udx ---------------------------------------------------------------

    private static final class UdxRefProvider extends PsiReferenceProvider {
        @Override
        public PsiReference @NotNull [] getReferencesByElement(
                @NotNull PsiElement element, @NotNull ProcessingContext context) {
            var t = typeOf(element);
            String text = element.getText();
            if (t == null || text == null || text.isEmpty()) return PsiReference.EMPTY_ARRAY;
            // A udx Apertium tag (column 2-3) → its lexc declaration.
            if (t == io.github.alexeyev.morphingbird.udx.UdxTokenTypes.TAG) {
                TextRange whole = new TextRange(0, text.length());
                return ref(element, whole, MorphingbirdSymbolReference.Kind.TAG,
                        "<" + text.trim() + ">");
            }
            return PsiReference.EMPTY_ARRAY;
        }
    }

    // --- lexd --------------------------------------------------------------

    private static final class LexdRefProvider extends PsiReferenceProvider {
        @Override
        public PsiReference @NotNull [] getReferencesByElement(
                @NotNull PsiElement element, @NotNull ProcessingContext context) {
            var t = typeOf(element);
            String text = element.getText();
            if (t == null || text == null || text.isEmpty()) return PsiReference.EMPTY_ARRAY;
            TextRange whole = new TextRange(0, text.length());
            // A lexd <tag> → its declaration (here, the lexd itself / shared graph).
            if (t == io.github.alexeyev.morphingbird.lexd.LexdTokenTypes.TAG) {
                String inner = text.length() >= 2 ? text.substring(1, text.length() - 1) : text;
                return ref(element, whole, MorphingbirdSymbolReference.Kind.TAG, "<" + inner + ">");
            }
            // A lexd identifier (pattern/lexicon name reference) → its definition.
            if (t == io.github.alexeyev.morphingbird.lexd.LexdTokenTypes.IDENT) {
                return ref(element, whole, MorphingbirdSymbolReference.Kind.NAMED, text);
            }
            return PsiReference.EMPTY_ARRAY;
        }
    }

    // --- helpers -----------------------------------------------------------

    private static com.intellij.psi.tree.IElementType typeOf(PsiElement e) {
        return e.getNode() != null ? e.getNode().getElementType() : null;
    }

    private static PsiReference[] ref(PsiElement el, TextRange range,
                                      MorphingbirdSymbolReference.Kind kind, String key) {
        return new PsiReference[]{new MorphingbirdSymbolReference(el, range, kind, key)};
    }

    /** {@code %<nom%>} -> {@code <nom>}, {@code %{A%}} -> {@code {A}}. */
    private static String stripEscapes(String raw) {
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '%' && i + 1 < raw.length()) sb.append(raw.charAt(++i));
            else sb.append(c);
        }
        return sb.toString();
    }

    /** CG3 tags appear bare ({@code nom}) or bracketed ({@code <nom>}). */
    private static String normalizeCg3Tag(String raw) {
        String t = raw.trim();
        if (t.startsWith("<") && t.endsWith(">")) return t;
        return "<" + t + ">";
    }

    private static String stripLeadingAt(String s) {
        int i = 0;
        while (i < s.length() && s.charAt(i) == '@') i++;
        return s.substring(i);
    }
}
