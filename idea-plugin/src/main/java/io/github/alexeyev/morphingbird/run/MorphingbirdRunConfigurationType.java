package io.github.alexeyev.morphingbird.run;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationOptions;
import com.intellij.openapi.project.Project;
import io.github.alexeyev.morphingbird.common.MorphingbirdIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

/** The "Apertium mode" run configuration type. */
public final class MorphingbirdRunConfigurationType implements ConfigurationType {

    public static final String ID = "MorphingbirdRunConfiguration";

    @Override
    public @NotNull String getDisplayName() {
        return "Apertium mode";
    }

    @Override
    public String getConfigurationTypeDescription() {
        return "Run an apertium mode (analyse / generate / transliterate) over input text";
    }

    @Override
    public Icon getIcon() {
        return MorphingbirdIcons.MODE;
    }

    @Override
    public @NotNull String getId() {
        return ID;
    }

    @Override
    public ConfigurationFactory[] getConfigurationFactories() {
        return new ConfigurationFactory[]{new Factory(this)};
    }

    /** The factory that creates configuration instances and their options. */
    public static final class Factory extends ConfigurationFactory {
        public Factory(ConfigurationType type) {
            super(type);
        }

        @Override
        public @NotNull String getId() {
            return MorphingbirdRunConfigurationType.ID;
        }

        @Override
        public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
            return new MorphingbirdRunConfiguration(project, this, "Apertium");
        }

        @Override
        public @Nullable Class<? extends RunConfigurationOptions> getOptionsClass() {
            return MorphingbirdRunOptions.class;
        }
    }
}
