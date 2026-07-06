package io.github.alexeyev.morphingbird.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Renders a {@link BuildGraphModel} into things a person can read: a Mermaid
 * flowchart (which IntelliJ shows natively in Markdown preview, and which the
 * plugin can also display directly) and a set of plain-language tips that
 * explain what the pipeline does and flag anything that looks off.
 *
 * <p>Pure string output, no IntelliJ dependency — so it is unit-testable.</p>
 */
public final class BuildGraphRenderer {
    private BuildGraphRenderer() {}

    /**
     * Emits a Mermaid {@code flowchart LR}. Nodes are grouped/shaped by type:
     * sources as rounded boxes, tools as stadium shapes, artifacts as plain
     * boxes, modes as hexagons, with a class-based colour per type.
     */
    public static String toMermaid(BuildGraphModel g) {
        StringBuilder sb = new StringBuilder();
        sb.append("flowchart LR\n");

        // Node declarations with type-specific shapes.
        for (BuildGraphModel.Node n : g.nodes.values()) {
            String safe = mermaidId(n.id);
            String label = escape(n.label);
            switch (n.type) {
                case SOURCE   -> sb.append("  ").append(safe).append("([\"").append(label).append("\"])\n");
                case TOOL     -> sb.append("  ").append(safe).append("[/\"").append(label).append("\"/]\n");
                case ARTIFACT -> sb.append("  ").append(safe).append("[\"").append(label).append("\"]\n");
                case MODE     -> sb.append("  ").append(safe).append("{{\"").append(label).append("\"}}\n");
            }
        }
        sb.append("\n");
        // Edges.
        for (BuildGraphModel.Edge e : g.edges) {
            if (!g.nodes.containsKey(e.from) || !g.nodes.containsKey(e.to)) continue;
            sb.append("  ").append(mermaidId(e.from)).append(" --> ")
                    .append(mermaidId(e.to)).append("\n");
        }
        sb.append("\n");
        // Styling per type.
        sb.append("  classDef source fill:#E8F0FE,stroke:#3592C4,color:#1A1A1A;\n");
        sb.append("  classDef tool fill:#FFF3E0,stroke:#E08E3C,color:#1A1A1A;\n");
        sb.append("  classDef artifact fill:#F3F3F3,stroke:#6C707E,color:#1A1A1A;\n");
        sb.append("  classDef mode fill:#E6F4EA,stroke:#59A869,color:#1A1A1A;\n");
        appendClassAssignments(sb, g);
        return sb.toString();
    }

    private static void appendClassAssignments(StringBuilder sb, BuildGraphModel g) {
        for (BuildGraphModel.NodeType t : BuildGraphModel.NodeType.values()) {
            List<String> ids = new ArrayList<>();
            for (BuildGraphModel.Node n : g.nodes.values()) {
                if (n.type == t) ids.add(mermaidId(n.id));
            }
            if (ids.isEmpty()) continue;
            sb.append("  class ").append(String.join(",", ids)).append(' ')
                    .append(t.name().toLowerCase()).append(";\n");
        }
    }

    /**
     * Generates plain-language tips describing the pipeline and flagging issues.
     * Pass the real source-file names so the "unused source" check only considers
     * genuine inputs (not Makefile-derived intermediates that share an extension).
     */
    public static List<String> tips(BuildGraphModel g, List<String> realSources) {
        return tips(g, realSources, null);
    }

    /** Tips including test-suite awareness when a parsed {@link TestSuiteModel} is given. */
    public static List<String> tips(BuildGraphModel g, List<String> realSources,
                                    TestSuiteModel suite) {
        List<String> tips = new ArrayList<>();

        int modes = g.modePipelines.size();
        var tools = g.tools();

        if (modes > 0) {
            tips.add("This module defines " + modes + " mode" + (modes == 1 ? "" : "s")
                    + " in modes.xml — these are the pipelines you run with "
                    + "`apertium -d . <mode>`.");
        }

        // Describe the toolchain in use.
        boolean hfst = tools.stream().anyMatch(t -> t.startsWith("hfst"));
        boolean lttoolbox = tools.contains("lt-comp") || tools.contains("lt-proc");
        boolean cg = tools.contains("cg-comp");
        if (hfst && lttoolbox) {
            tips.add("It uses both FST toolkits: HFST (hfst-lexc / hfst-twolc) for the "
                    + "morphology and lttoolbox (lt-comp) for the dictionary side.");
        } else if (hfst) {
            tips.add("Morphology is built with HFST (hfst-lexc compiles the lexicon, "
                    + "hfst-twolc the two-level rules).");
        } else if (lttoolbox) {
            tips.add("Morphology is built with lttoolbox (lt-comp compiles the .dix).");
        }
        if (cg) {
            tips.add("Disambiguation uses Constraint Grammar (cg-comp compiles the .rlx).");
        }

        // Note spellrelax if present among the sources.
        if (realSources != null && realSources.stream().anyMatch(s -> s.endsWith(".spellrelax"))) {
            tips.add("A .spellrelax file is present — these HFST rules let the analyser "
                    + "accept orthographic and typographic variants of the input.");
        }

        // Analysis vs generation split.
        boolean hasMorph = g.modePipelines.keySet().stream()
                .anyMatch(m -> m.contains("morph") || m.contains("anmorph"));
        boolean hasGen = g.modePipelines.keySet().stream()
                .anyMatch(m -> m.contains("gener") || m.contains("gen"));
        if (hasMorph && hasGen) {
            tips.add("There are both analysis modes (surface form → tags) and "
                    + "generation modes (tags → surface form).");
        }

        // Flag a *real* source file that nothing in the build consumes. Only the
        // names the caller declared as sources are considered, so generated
        // intermediates that share an extension don't trip this.
        if (realSources != null) {
            for (String src : realSources) {
                String id = "src:" + src;
                boolean known = g.nodes.containsKey(id);
                boolean consumed = g.edges.stream().anyMatch(e -> e.from.equals(id));
                if (known && !consumed) {
                    tips.add("Heads-up: `" + src + "` doesn't appear to feed any build "
                            + "rule — it may be unused, or built by a rule Morphingbird "
                            + "couldn't parse.");
                }
            }
        }

        // Test suite awareness.
        if (suite != null && !suite.tests.isEmpty()) {
            int n = suite.tests.size();
            tips.add("The module has " + n + " regtest" + (n == 1 ? "" : "s")
                    + " in `test/tests.json` (run them with `apertium-regtest test`); "
                    + "each feeds an input file through one of the modes above.");
        }

        if (tips.isEmpty()) {
            tips.add("No modes.xml or Makefile build rules were found to describe the "
                    + "pipeline.");
        }
        return tips;
    }

    /** Backwards-compatible overload without the source list (skips unused-source check). */
    public static List<String> tips(BuildGraphModel g) {
        return tips(g, null);
    }

    // --- helpers ---

    /**
     * Generates hints about a {@code .prob} tagger model relative to the
     * project's morphology tagset. Surfaces an untrained placeholder, and for a
     * trained model, the mismatch between what the tagger knows and what the
     * morphology actually emits (tags the tagger can't disambiguate, and tags it
     * knows that the morphology no longer has).
     */
    public static List<String> probHints(ProbModel prob, java.util.Set<String> morphologyTags) {
        List<String> hints = new ArrayList<>();
        if (prob == null) return hints;

        hints.add(prob.note);
        if (prob.looksUntrained || prob.tags.isEmpty()) {
            return hints;   // nothing more meaningful to compare
        }
        if (morphologyTags == null || morphologyTags.isEmpty()) {
            return hints;
        }

        java.util.Set<String> probTags = prob.canonicalTags();
        List<String> morphOnly = new ArrayList<>();
        for (String t : morphologyTags) if (!probTags.contains(t)) morphOnly.add(t);
        List<String> taggerOnly = new ArrayList<>();
        for (String t : probTags) if (!morphologyTags.contains(t)) taggerOnly.add(t);

        int shared = probTags.size() - taggerOnly.size();
        hints.add("Tagger/morphology tagset overlap: " + shared + " shared, "
                + morphOnly.size() + " only in the morphology, "
                + taggerOnly.size() + " only in the tagger.");

        if (!morphOnly.isEmpty()) {
            java.util.Collections.sort(morphOnly);
            hints.add("The tagger was not trained on " + morphOnly.size()
                    + " tag(s) your morphology emits, so words bearing them won't be "
                    + "disambiguated: " + preview(morphOnly)
                    + " — consider retraining with apertium-tagger.");
        }
        if (!taggerOnly.isEmpty()) {
            java.util.Collections.sort(taggerOnly);
            hints.add("The tagger knows " + taggerOnly.size()
                    + " tag(s) your morphology no longer emits (likely removed since "
                    + "training): " + preview(taggerOnly) + ".");
        }
        return hints;
    }

    private static String preview(List<String> items) {
        int n = Math.min(items.size(), 12);
        String head = String.join(", ", items.subList(0, n));
        return items.size() > n ? head + ", …" : head;
    }

    /** Mermaid node ids must be alphanumeric-ish; map arbitrary ids to safe ones. */
    private static String mermaidId(String id) {
        StringBuilder sb = new StringBuilder("n");
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            sb.append(Character.isLetterOrDigit(c) ? c : '_');
        }
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("\"", "&quot;");
    }
}
