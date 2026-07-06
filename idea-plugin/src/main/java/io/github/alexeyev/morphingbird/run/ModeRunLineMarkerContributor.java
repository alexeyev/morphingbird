package io.github.alexeyev.morphingbird.run;

import com.intellij.execution.lineMarker.RunLineMarkerContributor;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTokenType;
import org.jetbrains.annotations.Nullable;

/**
 * Puts a "Run" arrow in the editor gutter next to each {@code <mode name="…">}
 * entry in an apertium {@code modes.xml}. Clicking it creates/launches an
 * {@link MorphingbirdRunConfiguration} for that mode (via the standard
 * Run-line-marker machinery + {@link MorphingbirdRunConfigurationProducer}).
 *
 * <p>We deliberately anchor on the leaf token of the {@code name} attribute
 * value (not the {@code <mode>} tag itself) so the gutter icon lands on the
 * right line and the producer can read the mode name from context. We also
 * require the file to be named {@code modes.xml} to avoid marking unrelated XML.
 */
public final class ModeRunLineMarkerContributor extends RunLineMarkerContributor {

    @Override
    public @Nullable Info getInfo(PsiElement element) {
        // Only the identifier-ish leaf inside the attribute value, once per mode.
        if (element.getNode() == null) return null;
        if (element.getNode().getElementType() != XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN) {
            return null;
        }
        if (!isModesXml(element)) return null;

        XmlAttributeValue value = parentOfType(element, XmlAttributeValue.class);
        if (value == null) return null;
        XmlAttribute attr = parentOfType(value, XmlAttribute.class);
        if (attr == null || !"name".equals(attr.getName())) return null;
        XmlTag tag = attr.getParent();
        if (tag == null || !"mode".equals(tag.getName())) return null;

        // One info per mode; the actions come from the registered producers.
        AnAction[] actions = com.intellij.execution.lineMarker.ExecutorAction
                .getActions(0);
        return new Info(AllIcons.RunConfigurations.TestState.Run, actions,
                e -> "Run apertium mode '" + attr.getValue() + "'");
    }

    private static boolean isModesXml(PsiElement element) {
        var file = element.getContainingFile();
        return file != null && "modes.xml".equals(file.getName());
    }

    @SuppressWarnings("unchecked")
    private static <T> T parentOfType(PsiElement e, Class<T> type) {
        PsiElement cur = e;
        while (cur != null) {
            if (type.isInstance(cur)) return (T) cur;
            cur = cur.getParent();
        }
        return null;
    }
}
