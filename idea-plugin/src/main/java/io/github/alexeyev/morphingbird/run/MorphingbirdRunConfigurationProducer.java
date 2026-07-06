package io.github.alexeyev.morphingbird.run;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.LazyRunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

/**
 * Creates an {@link MorphingbirdRunConfiguration} from a {@code <mode>} entry in
 * modes.xml (used by the gutter run arrow and the Run context menu). The mode
 * name comes from the tag's {@code name} attribute; the data directory defaults
 * to the directory containing modes.xml (where compiled artifacts live).
 */
public final class MorphingbirdRunConfigurationProducer
        extends LazyRunConfigurationProducer<MorphingbirdRunConfiguration> {

    @Override
    public @NotNull ConfigurationFactory getConfigurationFactory() {
        MorphingbirdRunConfigurationType type =
                com.intellij.execution.configurations.ConfigurationTypeUtil
                        .findConfigurationType(MorphingbirdRunConfigurationType.class);
        return type.getConfigurationFactories()[0];
    }

    @Override
    protected boolean setupConfigurationFromContext(
            @NotNull MorphingbirdRunConfiguration configuration,
            @NotNull ConfigurationContext context,
            @NotNull Ref<PsiElement> sourceElement) {
        XmlTag mode = findModeTag(context);
        if (mode == null) return false;

        String name = modeName(mode);
        if (name == null || name.isEmpty()) return false;

        configuration.options().setMode(name);
        String dir = modesDir(context);
        if (dir != null) configuration.options().setDataDir(dir);
        configuration.setName("Apertium: " + name);
        sourceElement.set(mode);
        return true;
    }

    @Override
    public boolean isConfigurationFromContext(
            @NotNull MorphingbirdRunConfiguration configuration,
            @NotNull ConfigurationContext context) {
        XmlTag mode = findModeTag(context);
        if (mode == null) return false;
        String name = modeName(mode);
        return name != null && name.equals(configuration.options().getMode());
    }

    private static XmlTag findModeTag(ConfigurationContext context) {
        PsiElement loc = context.getPsiLocation();
        if (loc == null) return null;
        var file = loc.getContainingFile();
        if (file == null || !"modes.xml".equals(file.getName())) return null;
        PsiElement cur = loc;
        while (cur != null) {
            if (cur instanceof XmlTag && "mode".equals(((XmlTag) cur).getName())) {
                return (XmlTag) cur;
            }
            cur = cur.getParent();
        }
        return null;
    }

    private static String modeName(XmlTag mode) {
        XmlAttribute a = mode.getAttribute("name");
        return a == null ? null : a.getValue();
    }

    private static String modesDir(ConfigurationContext context) {
        PsiElement loc = context.getPsiLocation();
        if (loc == null) return null;
        var file = loc.getContainingFile();
        if (file == null) return null;
        var vf = file.getVirtualFile();
        if (vf == null || vf.getParent() == null) return null;
        return vf.getParent().getPath();
    }
}
