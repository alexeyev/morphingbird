package io.github.alexeyev.morphingbird.lttoolbox;

import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ProcessingContext;
import io.github.alexeyev.morphingbird.refs.MorphingbirdSymbolReference;
import org.jetbrains.annotations.NotNull;

/**
 * Wires lttoolbox {@code .dix}/{@code .lsx} XML into the shared symbol graph by
 * attaching references to the {@code n="..."} attribute values of the relevant
 * elements:
 * <ul>
 *   <li>{@code <s n="np"/>} and {@code <sdef n="np"/>} → the shared tag
 *       {@code <np>} (so navigation jumps to the lexc {@code %<np%>} declaration,
 *       and Find Usages spans lttoolbox + lexc + CG3);</li>
 *   <li>{@code <par n="..">} → the {@code <pardef n="..">} definition
 *       (lttoolbox-internal).</li>
 * </ul>
 *
 * <p>This is the "cheap half" working as intended: IntelliJ already provides the
 * XML PSI, so we only attach references on top of it rather than writing a
 * parser.</p>
 */
public final class DixReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        // Attach to any XML attribute value named "n"; the provider then checks
        // the owning tag to decide what kind of reference (if any) to produce.
        registrar.registerReferenceProvider(
                XmlPatterns.xmlAttributeValue().withParent(
                        XmlPatterns.xmlAttribute().withName("n")),
                new PsiReferenceProvider() {
                    @Override
                    public PsiReference @NotNull [] getReferencesByElement(
                            @NotNull PsiElement element,
                            @NotNull ProcessingContext context) {
                        if (!(element instanceof XmlAttributeValue)) {
                            return PsiReference.EMPTY_ARRAY;
                        }
                        XmlAttributeValue value = (XmlAttributeValue) element;
                        String name = value.getValue();
                        if (name == null || name.isEmpty()) return PsiReference.EMPTY_ARRAY;

                        XmlTag tag = tagOf(value);
                        if (tag == null) return PsiReference.EMPTY_ARRAY;
                        String tagName = tag.getName();

                        // Range of the value text inside the quotes.
                        TextRange range = valueRange(value);
                        if (range == null) return PsiReference.EMPTY_ARRAY;

                        switch (tagName) {
                            case "s":
                            case "sdef":
                                // tag symbol → shared <name> tag
                                return new PsiReference[]{
                                        new MorphingbirdSymbolReference(element, range,
                                                MorphingbirdSymbolReference.Kind.TAG,
                                                "<" + name + ">")};
                            case "par":
                                // paradigm use → pardef definition
                                return new PsiReference[]{
                                        new MorphingbirdSymbolReference(element, range,
                                                MorphingbirdSymbolReference.Kind.NAMED,
                                                name)};
                            default:
                                return PsiReference.EMPTY_ARRAY;
                        }
                    }
                });
    }

    private static XmlTag tagOf(XmlAttributeValue value) {
        PsiElement p = value.getParent();
        if (p instanceof XmlAttribute) {
            return ((XmlAttribute) p).getParent();
        }
        return null;
    }

    /** The value text range relative to the attribute-value element (inside quotes). */
    private static TextRange valueRange(XmlAttributeValue value) {
        // XmlAttributeValue text includes the surrounding quotes; the value
        // proper starts at offset 1. getValueTextRange gives the in-element range.
        TextRange r = value.getValueTextRange();
        int startInElement = r.getStartOffset() - value.getTextRange().getStartOffset();
        int endInElement = r.getEndOffset() - value.getTextRange().getStartOffset();
        if (startInElement < 0 || endInElement < startInElement) return null;
        return new TextRange(startInElement, endInElement);
    }
}
