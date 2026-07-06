package io.github.alexeyev.morphingbird.run;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import io.github.alexeyev.morphingbird.core.BuildGraphModel;
import io.github.alexeyev.morphingbird.core.BuildGraphRenderer;
import io.github.alexeyev.morphingbird.core.Cg3Model;
import io.github.alexeyev.morphingbird.core.DixModel;
import io.github.alexeyev.morphingbird.core.LexcParser;
import io.github.alexeyev.morphingbird.core.LexdModel;
import io.github.alexeyev.morphingbird.core.ProbModel;
import io.github.alexeyev.morphingbird.core.SymbolIndex;
import io.github.alexeyev.morphingbird.core.TestSuiteModel;
import io.github.alexeyev.morphingbird.core.TwolModel;
import io.github.alexeyev.morphingbird.core.UdxModel;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * "Show Build Pipeline": scans the module for {@code modes.xml}, a Makefile and
 * the source files, reconstructs the build/runtime graph, and opens a generated
 * Markdown document containing a Mermaid flowchart of the pipeline plus
 * plain-language tips. Markdown + Mermaid render natively in IntelliJ's preview,
 * so this gives a real diagram without a bespoke UI.
 */
public final class ShowBuildPipelineAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
        final String[] modesXml = {null};
        final String[] makefile = {null};
        final String[] testsJson = {null};
        final byte[][] probBytes = {null};
        List<String> sources = new ArrayList<>();
        final VirtualFile[] anchorDir = {null};

        fileIndex.iterateContent(vf -> {
            if (vf.isDirectory()) return true;
            String name = vf.getName();
            try {
                if (name.equals("modes.xml")) {
                    modesXml[0] = new String(vf.contentsToByteArray(), StandardCharsets.UTF_8);
                    if (anchorDir[0] == null) anchorDir[0] = vf.getParent();
                } else if (name.equals("Makefile.am") || name.equals("Makefile")) {
                    if (makefile[0] == null) {
                        makefile[0] = new String(vf.contentsToByteArray(), StandardCharsets.UTF_8);
                    }
                } else if (name.equals("tests.json")) {
                    testsJson[0] = new String(vf.contentsToByteArray(), StandardCharsets.UTF_8);
                } else if (name.endsWith(".prob")) {
                    if (probBytes[0] == null) probBytes[0] = vf.contentsToByteArray();
                } else if (name.matches(".*\\.(lexc|lexd|twol|twoc|rlx|dix|lsx|metadix|udx|spellrelax)$")) {
                    sources.add(name);
                }
            } catch (IOException ignored) {
                // skip unreadable file
            }
            return true;
        });

        BuildGraphModel graph = BuildGraphModel.build(
                modesXml[0] == null ? "" : modesXml[0],
                makefile[0] == null ? "" : makefile[0],
                sources);

        // Build the morphology tagset from the lexc/lexd sources, so .prob hints
        // can compare the tagger against what the morphology actually emits.
        java.util.Set<String> morphologyTags = collectMorphologyTags(project);

        ProbModel prob = probBytes[0] != null ? ProbModel.parse(probBytes[0]) : null;

        String markdown = renderMarkdown(graph, sources, project.getName(),
                testsJson[0] == null ? "" : testsJson[0], prob, morphologyTags);

        // Write to a file next to modes.xml (or project root) and open it.
        VirtualFile dir = anchorDir[0] != null
                ? anchorDir[0]
                : com.intellij.openapi.project.ProjectUtil.guessProjectDir(project);
        if (dir == null) return;
        writeAndOpen(project, dir, "morphingbird-build-pipeline.md", markdown);
    }

    /** Builds the project's tag vocabulary from lexc/lexd files for the .prob cross-check. */
    private static java.util.Set<String> collectMorphologyTags(Project project) {
        SymbolIndex index = new SymbolIndex();
        ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
        fileIndex.iterateContent(vf -> {
            if (vf.isDirectory()) return true;
            String name = vf.getName();
            try {
                String text = new String(vf.contentsToByteArray(), StandardCharsets.UTF_8);
                if (name.endsWith(".lexc")) index.addLexc(vf.getPath(), LexcParser.parse(text));
                else if (name.endsWith(".lexd")) index.addLexd(vf.getPath(), LexdModel.parse(text));
                else if (name.endsWith(".rlx")) index.addCg3(vf.getPath(), Cg3Model.parse(text));
                else if (name.endsWith(".udx")) index.addUdx(vf.getPath(), UdxModel.parse(text));
            } catch (IOException ignored) {
                // skip
            }
            return true;
        });
        return index.allTags();
    }

    private static String renderMarkdown(BuildGraphModel graph, List<String> sources,
                                         String projectName, String testsJson,
                                         ProbModel prob, java.util.Set<String> morphologyTags) {
        StringBuilder md = new StringBuilder();
        md.append("# Build pipeline — ").append(projectName).append("\n\n");
        md.append("_Generated by Morphingbird from `modes.xml` and the Makefile. ");
        md.append("Sources flow left-to-right through the compiler tools into the ");
        md.append("binaries that each mode runs._\n\n");

        md.append("## How it fits together\n\n");
        TestSuiteModel suite = TestSuiteModel.parse(testsJson);
        for (String tip : BuildGraphRenderer.tips(graph, sources, suite)) {
            md.append("- ").append(tip).append("\n");
        }
        md.append("\n## Pipeline diagram\n\n");
        md.append("```mermaid\n");
        md.append(BuildGraphRenderer.toMermaid(graph));
        md.append("```\n\n");

        // A compact per-mode reference.
        if (!graph.modePipelines.isEmpty()) {
            md.append("## Modes\n\n");
            md.append("Each mode is a pipeline you can run with `apertium -d . <mode>`:\n\n");
            graph.modePipelines.forEach((mode, steps) -> {
                md.append("- **").append(mode).append("** — ");
                md.append(String.join(" → ", steps)).append("\n");
            });
            md.append("\n");
        }

        // Tests (apertium-regtest), linked to the modes they exercise.
        if (!suite.tests.isEmpty()) {
            md.append("## Tests\n\n");
            md.append("`test/tests.json` defines ").append(suite.tests.size());
            md.append(suite.tests.size() == 1 ? " regtest:" : " regtests, each running an input through a mode:");
            md.append("\n\n");
            for (TestSuiteModel.Test t : suite.tests) {
                md.append("- **").append(t.name).append("** — `").append(t.inputFile);
                md.append("` → mode `").append(t.mode).append("`\n");
            }
            md.append("\n");
        }

        // Tagger model (.prob) hints, decoded without running apertium-tagger.
        if (prob != null) {
            md.append("## Tagger model\n\n");
            for (String hint : BuildGraphRenderer.probHints(prob, morphologyTags)) {
                md.append("- ").append(hint).append("\n");
            }
            md.append("\n");
        }

        md.append("---\n_Legend: rounded = source file, slanted = compiler tool, ");
        md.append("box = built artifact, hexagon = mode._\n");
        return md.toString();
    }

    private static void writeAndOpen(Project project, VirtualFile dir,
                                     String fileName, String content) {
        WriteCommandAction.runWriteCommandAction(project, "Generate Build Pipeline", "Morphingbird", () -> {
            try {
                VirtualFile file = dir.findOrCreateChildData(ShowBuildPipelineAction.class, fileName);
                file.setBinaryContent(content.getBytes(StandardCharsets.UTF_8));
                FileEditorManager.getInstance(project).openFile(file, true);
            } catch (IOException ex) {
                // If the location is read-only we can't write the doc; nothing
                // else to do safely here in v1.
            }
        });
    }
}
