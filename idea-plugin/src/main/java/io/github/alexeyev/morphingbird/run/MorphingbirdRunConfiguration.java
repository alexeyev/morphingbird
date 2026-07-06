package io.github.alexeyev.morphingbird.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * A run configuration that executes one apertium mode over the configured input,
 * streaming the analysis/generation to the Run console. The toolchain is
 * auto-detected (overridable). Compiler/processor error locations in the output
 * are made navigable by {@link MorphingbirdConsoleFilter}.
 */
public final class MorphingbirdRunConfiguration
        extends RunConfigurationBase<MorphingbirdRunOptions> {

    protected MorphingbirdRunConfiguration(@NotNull Project project,
                                       @NotNull com.intellij.execution.configurations.ConfigurationFactory factory,
                                       @Nullable String name) {
        super(project, factory, name);
    }

    @Override
    protected @NotNull MorphingbirdRunOptions getOptions() {
        return (MorphingbirdRunOptions) super.getOptions();
    }

    public MorphingbirdRunOptions options() { return getOptions(); }

    @Override
    public @NotNull SettingsEditor<? extends RunConfigurationBase<MorphingbirdRunOptions>>
    getConfigurationEditor() {
        return new MorphingbirdRunSettingsEditor(getProject());
    }

    @Override
    public @Nullable RunProfileState getState(@NotNull Executor executor,
                                              @NotNull ExecutionEnvironment env)
            throws ExecutionException {
        MorphingbirdRunOptions opt = getOptions();

        String binary = ToolchainLocator.resolveApertium(opt.getApertiumBinary());
        if (binary == null) {
            throw new ExecutionException(ToolchainLocator.notFoundHint());
        }
        String dataDir = opt.getDataDir();
        String mode = opt.getMode();
        if (mode == null || mode.isBlank()) {
            throw new ExecutionException("No apertium mode selected.");
        }
        String input = opt.getInput() == null ? "" : opt.getInput();
        return buildState(env, binary, dataDir, mode, input);
    }

    /** Builds the actual command-line state (kept separate for clarity). */
    private RunProfileState buildState(ExecutionEnvironment env, String binary,
                                       String dataDir, String mode, String input) {
        CommandLineState state = new CommandLineState(env) {
            @Override
            protected @NotNull ProcessHandler startProcess() throws ExecutionException {
                GeneralCommandLine cmd = new GeneralCommandLine(binary);
                if (dataDir != null && !dataDir.isBlank()) {
                    cmd.addParameters("-d", dataDir);
                    cmd.setWorkDirectory(dataDir);
                }
                cmd.addParameter(mode);
                cmd.setCharset(StandardCharsets.UTF_8);
                OSProcessHandler handler = new OSProcessHandler(cmd);
                ProcessTerminatedListener.attach(handler);
                feedStdin(handler, input);
                return handler;
            }
        };
        state.setConsoleBuilder(
                TextConsoleBuilderFactory.getInstance().createBuilder(getProject()));
        // Make hfst/lt/cg error locations clickable.
        state.addConsoleFilters(new MorphingbirdConsoleFilter(getProject()));
        return state;
    }

    private static void feedStdin(OSProcessHandler handler, String input) {
        handler.addProcessListener(
                new com.intellij.execution.process.ProcessAdapter() {
                    @Override
                    public void startNotified(
                            @NotNull com.intellij.execution.process.ProcessEvent event) {
                        OutputStream os = handler.getProcessInput();
                        if (os == null) return;
                        try {
                            os.write(input.getBytes(StandardCharsets.UTF_8));
                            if (!input.endsWith("\n")) os.write('\n');
                            os.flush();
                        } catch (Exception ignored) {
                        } finally {
                            try { os.close(); } catch (Exception ignored) {}
                        }
                    }
                });
    }
}
