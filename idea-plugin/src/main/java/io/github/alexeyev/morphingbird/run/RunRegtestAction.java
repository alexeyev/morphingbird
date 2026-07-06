package io.github.alexeyev.morphingbird.run;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.RunContentManager;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Runs the module's Apertium regression tests by invoking
 * {@code apertium-regtest test} in the directory that holds {@code modes.xml},
 * streaming its output into a console (with the Morphingbird filter making any
 * {@code file:line:col} diagnostics clickable). Available when the project
 * contains a {@code test/tests.json}.
 *
 * <p>This connects the test suite Morphingbird already <em>describes</em> to actually
 * <em>running</em> it. It shells the real tool rather than reimplementing the
 * harness, so results match exactly what a maintainer would see on the command
 * line. The module must be compiled first (regtest needs the {@code .bin} /
 * {@code .hfst} artifacts); if it is not, the tool prints a clear
 * "Failed command" message, which appears verbatim in the console.</p>
 */
public final class RunRegtestAction extends AnAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Only enable when a tests.json is present in the project.
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null && findModuleDir(project) != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        VirtualFile moduleDir = findModuleDir(project);
        if (moduleDir == null) {
            Messages.showInfoMessage(project,
                    "No modes.xml / test/tests.json found, so there is no Apertium "
                            + "regression test suite to run.", "Morphingbird");
            return;
        }

        GeneralCommandLine cmd = new GeneralCommandLine("apertium-regtest", "test");
        cmd.setWorkDirectory(moduleDir.getPath());
        cmd.setCharset(StandardCharsets.UTF_8);

        ConsoleView console = TextConsoleBuilderFactory.getInstance()
                .createBuilder(project)
                .filters(new MorphingbirdConsoleFilter(project))
                .getConsole();

        final ProcessHandler handler;
        try {
            handler = new OSProcessHandler(cmd);
        } catch (com.intellij.execution.ExecutionException ex) {
            Messages.showErrorDialog(project,
                    "Couldn't launch apertium-regtest. Is it on your PATH?\n\n"
                            + ex.getMessage(), "Morphingbird");
            return;
        }
        ProcessTerminatedListener.attach(handler, project, "\nRegression tests finished with exit code $EXIT_CODE$\n");
        console.attachToProcess(handler);

        showConsole(project, console, handler);
        handler.startNotify();
    }

    /** Finds the directory that contains modes.xml (and usually test/tests.json). */
    private static VirtualFile findModuleDir(Project project) {
        ProjectFileIndex index = ProjectFileIndex.getInstance(project);
        final VirtualFile[] found = {null};
        index.iterateContent(vf -> {
            if (!vf.isDirectory() && vf.getName().equals("tests.json")) {
                VirtualFile parent = vf.getParent();        // the test/ dir
                if (parent != null && parent.getParent() != null) {
                    found[0] = parent.getParent();          // the module root
                    return false;
                }
            }
            return true;
        });
        if (found[0] != null) return found[0];
        // Fall back to a modes.xml location.
        index.iterateContent(vf -> {
            if (!vf.isDirectory() && vf.getName().equals("modes.xml")) {
                found[0] = vf.getParent();
                return false;
            }
            return true;
        });
        return found[0];
    }

    private static void showConsole(Project project, ConsoleView console,
                                    ProcessHandler handler) {
        RunnerLayoutUi ui = RunnerLayoutUi.Factory.getInstance(project)
                .create("Morphingbird", "Apertium regtest", "Regression tests", project);
        ui.addContent(ui.createContent("morphingbird.regtest", console.getComponent(),
                "Output", null, console.getPreferredFocusableComponent()));

        RunContentDescriptor descriptor =
                new RunContentDescriptor(console, handler, console.getComponent(),
                        "Apertium regtest");
        RunContentManager.getInstance(project)
                .showRunContent(getExecutor(), descriptor);
    }

    private static com.intellij.execution.Executor getExecutor() {
        return com.intellij.execution.executors.DefaultRunExecutor.getRunExecutorInstance();
    }
}
