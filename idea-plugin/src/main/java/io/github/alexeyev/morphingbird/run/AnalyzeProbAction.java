package io.github.alexeyev.morphingbird.run;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import io.github.alexeyev.morphingbird.common.ApertiumTagset;
import io.github.alexeyev.morphingbird.core.Cg3Model;
import io.github.alexeyev.morphingbird.core.LexcParser;
import io.github.alexeyev.morphingbird.core.LexdModel;
import io.github.alexeyev.morphingbird.core.ProbModel;
import io.github.alexeyev.morphingbird.core.SymbolIndex;
import io.github.alexeyev.morphingbird.core.UdxModel;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Right-click action on a {@code .prob} file: decodes the Apertium tagger model
 * (without running {@code apertium-tagger}) and opens a polished HTML report —
 * a status banner (trained vs untrained), the model's tagset ranked by how much
 * it relies on each tag (with glosses and frequency bars), and a cross-check
 * against the project's morphology highlighting tags the tagger can't
 * disambiguate and tags it carries that the morphology no longer emits.
 *
 * <p>The report is written as a self-contained {@code .html} file, which
 * IntelliJ renders in its built-in preview, so the result is visual without any
 * custom Swing UI.</p>
 */
public final class AnalyzeProbAction extends AnAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        boolean isProb = file != null && !file.isDirectory()
                && "prob".equals(file.getExtension());
        e.getPresentation().setEnabledAndVisible(isProb && e.getProject() != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (project == null || file == null) return;

        final byte[] bytes;
        try {
            bytes = file.contentsToByteArray();
        } catch (IOException ex) {
            return;
        }
        ProbModel model = ProbModel.parse(bytes);
        Set<String> morphologyTags = collectMorphologyTags(project);
        ProbModel.CrossCheck cc = model.crossCheck(morphologyTags);

        String html = renderHtml(model, cc, file.getName(), !morphologyTags.isEmpty());

        VirtualFile dir = file.getParent();
        if (dir == null) return;
        String outName = file.getNameWithoutExtension() + "-tagger-report.html";
        writeAndOpen(project, dir, outName, html);
    }

    // ---- HTML rendering ----

    private static String renderHtml(ProbModel m, ProbModel.CrossCheck cc,
                                     String fileName, boolean haveMorphology) {
        StringBuilder h = new StringBuilder();
        h.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><style>")
                .append(CSS).append("</style></head><body>");

        h.append("<h1>Tagger model — ").append(esc(fileName)).append("</h1>");

        // Status banner.
        boolean ok = !m.looksUntrained;
        String bannerStyle = ok
                ? "background:#E6F4EA;border-left:5px solid #59A869;color:#1E5631;"
                : "background:#FCEFE3;border-left:5px solid #E08E3C;color:#8A4B12;";
        h.append("<div style=\"").append(bannerStyle)
                .append("padding:12px 16px;border-radius:8px;margin:14px 0;font-weight:bold;\">");
        h.append(ok ? "✓ " : "⚠ ").append(esc(m.note));
        h.append("</div>");

        // Quick stats (inline-block cards, no flex needed).
        h.append("<div style=\"margin:16px 0;\">");
        stat(h, "File size", humanSize(m.byteSize));
        stat(h, "Tags known", String.valueOf(m.tags.size()));
        if (!m.looksUntrained && m.topTag() != null) {
            stat(h, "Most-used tag", m.topTag());
        }
        if (haveMorphology && !m.looksUntrained) {
            stat(h, "Tagset match", percent(cc.shared.size(),
                    cc.shared.size() + cc.morphologyOnly.size()));
        }
        h.append("</div>");

        if (m.looksUntrained || m.tags.isEmpty()) {
            h.append("<p style=\"color:#6C707E;font-size:13px;\">No trained tagset to display. Once the model "
                    + "is trained with <code>apertium-tagger</code>, this report will "
                    + "show its tagset and how it compares to your morphology.</p>");
            h.append("</body></html>");
            return h.toString();
        }

        // Cross-check section (most actionable — show first).
        if (haveMorphology) {
            h.append("<h2>Compared with your morphology</h2>");
            if (cc.isClean()) {
                h.append("<div class=\"banner ok\">✓ The tagger's tagset matches the "
                        + "morphology exactly — every tag is accounted for.</div>");
            } else {
                if (!cc.morphologyOnly.isEmpty()) {
                    h.append("<div style=\"background:#FCF6EF;border-left:5px solid "
                            + "#E0A04C;padding:10px 16px;margin:12px 0;border-radius:8px;\">");
                    h.append("<h3>⚠ ").append(cc.morphologyOnly.size())
                            .append(" tag(s) the tagger can't disambiguate</h3>");
                    h.append("<p style=\"color:#6C707E;font-size:13px;\">Your morphology emits these, but the "
                            + "tagger was never trained on them, so words bearing them "
                            + "fall back to undisambiguated output. Consider retraining "
                            + "with <code>apertium-tagger</code>.</p>");
                    h.append(tagChips(cc.morphologyOnly));
                    h.append("</div>");
                }
                if (!cc.taggerOnly.isEmpty()) {
                    h.append("<div style=\"background:#F0F4FA;border-left:5px solid "
                            + "#6E9CD2;padding:10px 16px;margin:12px 0;border-radius:8px;\">");
                    h.append("<h3>").append(cc.taggerOnly.size())
                            .append(" tag(s) only in the tagger</h3>");
                    h.append("<p style=\"color:#6C707E;font-size:13px;\">The model still carries these, but your "
                            + "morphology no longer emits them — likely removed since the "
                            + "tagger was trained.</p>");
                    h.append(tagChips(cc.taggerOnly));
                    h.append("</div>");
                }
            }
        }

        // Tagset ranked by reliance.
        h.append("<h2>Tagset by reliance</h2>");
        h.append("<p style=\"color:#6C707E;font-size:13px;\">How often each tag appears in the learned "
                + "ambiguity classes — a rough measure of how central it is to the "
                + "tagger's decisions.</p>");
        h.append("<table class=\"freq\">");
        List<Map.Entry<String, Integer>> ranked = m.tagsByFrequency();
        int max = ranked.isEmpty() ? 1 : ranked.get(0).getValue();
        for (Map.Entry<String, Integer> entry : ranked) {
            String tag = entry.getKey();
            int n = entry.getValue();
            String gloss = ApertiumTagset.gloss(tag);
            int pct = (int) Math.round(100.0 * n / max);
            h.append("<tr>");
            h.append("<td style=\"padding:4px 8px;border-bottom:1px solid #F0F0F0;"
                    + "font-family:ui-monospace,Menlo,Consolas,monospace;color:#2D6A9F;"
                    + "font-weight:bold;white-space:nowrap;\">&lt;").append(esc(tag)).append("&gt;</td>");
            h.append("<td style=\"padding:4px 8px;border-bottom:1px solid #F0F0F0;"
                    + "color:#444;width:38%;\">").append(gloss == null ? "" : esc(gloss)).append("</td>");
            h.append("<td style=\"padding:4px 8px;border-bottom:1px solid #F0F0F0;width:34%;\">");
            h.append("<div style=\"background:#EDEFF2;border-radius:5px;height:12px;width:100%;\">");
            h.append("<div style=\"background:#3592C4;height:12px;border-radius:5px;width:")
                    .append(Math.max(pct, 2)).append("%;\"></div></div></td>");
            h.append("<td style=\"padding:4px 8px;border-bottom:1px solid #F0F0F0;"
                    + "text-align:right;color:#555;width:48px;\">").append(n).append("</td>");
            h.append("</tr>");
        }
        h.append("</table>");

        h.append("<p style=\"margin-top:24px;font-size:11px;color:#9AA0AB;border-top:1px solid #eee;padding-top:10px;\">Decoded by Morphingbird directly from the binary "
                + ".prob — no apertium-tagger run required. Frequencies are a "
                + "heuristic read of the model's ambiguity classes.</p>");
        h.append("</body></html>");
        return h.toString();
    }

    private static void stat(StringBuilder h, String label, String value) {
        h.append("<div style=\"display:inline-block;background:#F5F7FA;"
                + "border:1px solid #E0E4E8;border-radius:8px;padding:8px 16px;"
                + "margin:6px 10px 6px 0;vertical-align:top;\">");
        h.append("<div style=\"font-size:20px;font-weight:bold;color:#1A1A1A;\">")
                .append(esc(value)).append("</div>");
        h.append("<div style=\"font-size:11px;color:#6C707E;text-transform:uppercase;"
                + "letter-spacing:.04em;\">").append(esc(label)).append("</div>");
        h.append("</div>");
    }

    private static String tagChips(List<String> tags) {
        StringBuilder h = new StringBuilder("<div style=\"margin-top:8px;\">");
        for (String t : tags) {
            String bare = t.startsWith("<") && t.endsWith(">")
                    ? t.substring(1, t.length() - 1) : t;
            String gloss = ApertiumTagset.gloss(bare);
            h.append("<span style=\"display:inline-block;background:#fff;"
                    + "border:1px solid #C9CDD6;border-radius:14px;padding:3px 10px;"
                    + "margin:3px 4px 3px 0;font-size:12px;\">");
            h.append("<span style=\"font-family:ui-monospace,Menlo,Consolas,monospace;"
                    + "color:#2D6A9F;font-weight:bold;\">").append(esc(t)).append("</span>");
            if (gloss != null) {
                h.append(" <span style=\"color:#6C707E;\">").append(esc(gloss)).append("</span>");
            }
            h.append("</span>");
        }
        h.append("</div>");
        return h.toString();
    }

    // ---- project morphology tagset ----

    private static Set<String> collectMorphologyTags(Project project) {
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

    // ---- file output ----

    private static void writeAndOpen(Project project, VirtualFile dir,
                                     String fileName, String content) {
        WriteCommandAction.runWriteCommandAction(project, "Analyze Tagger Model", "Morphingbird", () -> {
            try {
                VirtualFile file = dir.findOrCreateChildData(AnalyzeProbAction.class, fileName);
                file.setBinaryContent(content.getBytes(StandardCharsets.UTF_8));
                FileEditorManager.getInstance(project).openFile(file, true);
            } catch (IOException ignored) {
                // read-only location; nothing safe to do in v1
            }
        });
    }

    // ---- helpers ----

    private static String percent(int part, int whole) {
        if (whole == 0) return "—";
        return Math.round(100.0 * part / whole) + "%";
    }

    private static String humanSize(int bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static final String CSS =
            "body{font-family:-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;"
            + "max-width:880px;margin:24px auto;padding:0 20px;color:#2b2b2b;line-height:1.5;}"
            + "h1{font-size:22px;margin-bottom:4px;}"
            + "h2{font-size:17px;margin-top:30px;border-bottom:2px solid #e0e0e0;padding-bottom:6px;}"
            + "h3{font-size:14px;margin:0 0 6px;}"
            + "code{background:#F0F0F0;padding:1px 4px;border-radius:3px;font-size:90%;}"
            + "table{border-collapse:collapse;width:100%;margin-top:10px;font-size:13px;}";
}
