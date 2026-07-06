package io.github.alexeyev.morphingbird.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reconstructs the Apertium build / runtime pipeline for a language module, so
 * the IDE can show how the pieces fit together. Two sources are combined:
 *
 * <ul>
 *   <li><b>{@code modes.xml}</b> — the <em>runtime</em> pipelines. Each
 *       {@code <mode>} is an ordered chain of {@code <program>} steps, and each
 *       step names the compiled artifact it consumes ({@code kaz.automorf.bin},
 *       {@code .deps/kaz.lexc.hfst}). This is the data flow a user actually
 *       runs with {@code apertium -d . <mode>}.</li>
 *   <li><b>{@code Makefile.am}</b> — the <em>build</em> dependencies. Rules of
 *       the form {@code target: prereqs} with a tool on the recipe line
 *       ({@code hfst-lexc}, {@code hfst-twolc}, {@code lt-comp}, {@code cg-comp})
 *       show how source files become those artifacts.</li>
 * </ul>
 *
 * <p>The result is a typed node/edge graph: {@link NodeType#SOURCE} files →
 * {@link NodeType#TOOL} compilers → {@link NodeType#ARTIFACT} binaries →
 * {@link NodeType#MODE} pipelines. It is data only (no rendering, no IntelliJ
 * dependency), so it is unit-testable against real repositories and can be
 * emitted as Mermaid/DOT or summarised as plain-language tips by callers.</p>
 */
public final class BuildGraphModel {

    public enum NodeType { SOURCE, TOOL, ARTIFACT, MODE }

    public static final class Node {
        public final String id;       // stable id (usually the file/mode name)
        public final String label;    // display label
        public final NodeType type;
        public Node(String id, String label, NodeType type) {
            this.id = id; this.label = label; this.type = type;
        }
        @Override public boolean equals(Object o) {
            return o instanceof Node && ((Node) o).id.equals(id);
        }
        @Override public int hashCode() { return id.hashCode(); }
    }

    public static final class Edge {
        public final String from;
        public final String to;
        public final String label;    // optional (e.g. the tool, or "")
        public Edge(String from, String to, String label) {
            this.from = from; this.to = to; this.label = label;
        }
    }

    public final Map<String, Node> nodes = new LinkedHashMap<>();
    public final List<Edge> edges = new ArrayList<>();
    /** Mode name -> ordered list of program step labels, for tips/summaries. */
    public final Map<String, List<String>> modePipelines = new LinkedHashMap<>();

    private void addNode(String id, String label, NodeType type) {
        nodes.putIfAbsent(id, new Node(id, label, type));
    }
    private void addEdge(String from, String to, String label) {
        edges.add(new Edge(from, to, label));
    }

    /**
     * Builds the graph from the two declaration files. Either argument may be
     * empty/blank; whatever is available is used.
     *
     * @param modesXml   contents of {@code modes.xml}
     * @param makefile   contents of {@code Makefile.am} (or {@code Makefile})
     * @param sourceFiles bare names of the source files present in the module
     *                    (e.g. {@code apertium-kaz.kaz.lexc}); used to anchor the
     *                    build side onto real files.
     */
    public static BuildGraphModel build(String modesXml, String makefile,
                                        List<String> sourceFiles) {
        BuildGraphModel g = new BuildGraphModel();
        if (sourceFiles != null) {
            for (String s : sourceFiles) {
                // Use the same "src:" key scheme the Makefile parser uses, so a
                // pre-seeded source and a Makefile-referenced source are the
                // SAME node (otherwise "unused source" checks misfire).
                g.addNode("src:" + s, s, NodeType.SOURCE);
            }
        }
        if (makefile != null && !makefile.isBlank()) {
            g.parseMakefile(makefile);
        }
        if (modesXml != null && !modesXml.isBlank()) {
            g.parseModes(modesXml);
        }
        return g;
    }

    // ---- modes.xml -------------------------------------------------------

    private static final Pattern MODE =
            Pattern.compile("<mode\\b[^>]*\\bname=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern PROGRAM =
            Pattern.compile("<program\\b[^>]*\\bname=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern FILE_IN_PROGRAM =
            Pattern.compile("<file\\b[^>]*\\bname=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);

    private void parseModes(String xml) {
        Matcher mm = MODE.matcher(xml);
        // Collect mode spans by walking each <mode>...</mode>.
        int searchFrom = 0;
        while (mm.find(searchFrom)) {
            String modeName = mm.group(1);
            int modeStart = mm.end();
            int modeEnd = indexOfClose(xml, "</mode>", modeStart);
            String body = xml.substring(modeStart, modeEnd < 0 ? xml.length() : modeEnd);

            String modeId = "mode:" + modeName;
            addNode(modeId, modeName, NodeType.MODE);

            // Each program step in order; link artifacts it consumes -> mode.
            List<String> steps = new ArrayList<>();
            Matcher pm = PROGRAM.matcher(body);
            while (pm.find()) {
                String prog = pm.group(1).trim();
                steps.add(prog);
                // Files referenced inside this program element. We approximate
                // the program's span as up to the next <program or end.
                int pStart = pm.end();
                int nextProg = body.indexOf("<program", pStart);
                String pBody = body.substring(pStart, nextProg < 0 ? body.length() : nextProg);
                Matcher fm = FILE_IN_PROGRAM.matcher(pBody);
                while (fm.find()) {
                    String artifact = normalizeArtifact(fm.group(1));
                    addNode("art:" + artifact, artifact, NodeType.ARTIFACT);
                    addEdge("art:" + artifact, modeId, "");
                }
            }
            modePipelines.put(modeName, steps);

            searchFrom = modeEnd < 0 ? xml.length() : modeEnd;
            if (searchFrom >= xml.length()) break;
        }
    }

    // ---- Makefile.am -----------------------------------------------------

    // A make rule "target: prereq1 prereq2". We capture the target and prereqs.
    private static final Pattern RULE =
            Pattern.compile("^([^\\s:#][^:#\\n]*?):\\s*([^\\n=]*)$", Pattern.MULTILINE);
    // Tools we recognise on recipe lines.
    private static final String[] TOOLS = {
            "hfst-lexc", "hfst-twolc", "lt-comp", "cg-comp",
            "hfst-compose-intersect", "hfst-invert", "lt-proc", "hfst-proc",
            "hfst-strings2fst", "hfst-fst2strings"
    };

    private void parseMakefile(String mk) {
        // Resolve the common automake variables (LANG1, BASENAME, ...) so targets
        // and prereqs map onto real-looking filenames instead of $(VAR) noise.
        Map<String, String> vars = collectVars(mk);

        Matcher rm = RULE.matcher(mk);
        while (rm.find()) {
            String rawTarget = expandVars(rm.group(1).trim(), vars);
            String rawPrereqs = expandVars(rm.group(2).trim(), vars);
            if (rawTarget.isEmpty()) continue;
            // skip phony/internal targets without a useful artifact shape
            if (rawTarget.startsWith(".") && !rawTarget.contains("/")) continue;

            String target = normalizeArtifact(rawTarget);
            // Identify the recipe (the indented lines after the rule) to find a tool.
            String tool = findToolForRule(mk, rm.end());

            // Only model rules that produce a build artifact (have an extension
            // we care about) and have at least one prereq.
            if (!looksLikeArtifact(target) && !looksLikeSource(target)) continue;

            NodeType tType = looksLikeSource(target) ? NodeType.SOURCE : NodeType.ARTIFACT;
            addNode(nodeKey(tType, target), target, tType);

            for (String prereq : rawPrereqs.split("\\s+")) {
                String p = normalizeArtifact(prereq.trim());
                if (p.isEmpty() || p.equals("\\")) continue;
                if (!looksLikeArtifact(p) && !looksLikeSource(p)) continue;
                NodeType pType = looksLikeSource(p) ? NodeType.SOURCE : NodeType.ARTIFACT;
                addNode(nodeKey(pType, p), p, pType);

                if (tool != null) {
                    String toolId = "tool:" + tool;
                    addNode(toolId, tool, NodeType.TOOL);
                    addEdge(nodeKey(pType, p), toolId, "");
                    addEdge(toolId, nodeKey(tType, target), "");
                } else {
                    addEdge(nodeKey(pType, p), nodeKey(tType, target), "");
                }
            }
        }
    }

    private static String nodeKey(NodeType t, String name) {
        return (t == NodeType.SOURCE ? "src:" : "art:") + name;
    }

    /** Looks at the indented recipe lines after a rule for a known tool name. */
    private static String findToolForRule(String mk, int from) {
        int lineStart = from;
        // Move to the next line.
        int nl = mk.indexOf('\n', from);
        if (nl < 0) return null;
        lineStart = nl + 1;
        // Recipe lines begin with a tab; scan a few.
        for (int i = 0; i < 6 && lineStart < mk.length(); i++) {
            int lineEnd = mk.indexOf('\n', lineStart);
            if (lineEnd < 0) lineEnd = mk.length();
            String line = mk.substring(lineStart, lineEnd);
            if (!line.startsWith("\t") && !line.startsWith("    ")) break; // recipe ended
            for (String tool : TOOLS) {
                if (line.contains(tool)) return tool;
            }
            lineStart = lineEnd + 1;
        }
        return null;
    }

    // ---- helpers ---------------------------------------------------------

    private static final Pattern VAR_DEF =
            Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)\\s*[:?]?=\\s*(.+)$", Pattern.MULTILINE);
    private static final Pattern VAR_USE =
            Pattern.compile("\\$\\(([A-Za-z_][A-Za-z0-9_]*)\\)");

    /** Collects {@code VAR = value} definitions from a Makefile. */
    private static Map<String, String> collectVars(String mk) {
        Map<String, String> vars = new LinkedHashMap<>();
        Matcher m = VAR_DEF.matcher(mk);
        while (m.find()) {
            // Ignore lines that are actually rules (target: prereq) — those have
            // no '=' so VAR_DEF won't match them anyway. Strip trailing comments.
            String val = m.group(2);
            int hash = val.indexOf('#');
            if (hash >= 0) val = val.substring(0, hash);
            vars.put(m.group(1), val.trim());
        }
        return vars;
    }

    /** Recursively expands {@code $(VAR)} references using the collected map. */
    private static String expandVars(String s, Map<String, String> vars) {
        String cur = s;
        for (int depth = 0; depth < 8; depth++) {
            Matcher m = VAR_USE.matcher(cur);
            if (!m.find()) break;
            StringBuilder sb = new StringBuilder();
            m.reset();
            boolean any = false;
            while (m.find()) {
                String name = m.group(1);
                String rep = vars.get(name);
                if (rep == null) rep = m.group(0);   // leave unknown vars intact
                else any = true;
                m.appendReplacement(sb, Matcher.quoteReplacement(rep));
            }
            m.appendTail(sb);
            cur = sb.toString();
            if (!any) break;   // nothing expanded this round; stop
        }
        return cur;
    }

    /** Strips a leading {@code .deps/} and surrounding whitespace from an artifact name. */
    private static String normalizeArtifact(String s) {
        String t = s.trim();
        if (t.startsWith(".deps/")) t = t.substring(".deps/".length());
        return t;
    }

    private static boolean looksLikeArtifact(String s) {
        return s.endsWith(".hfst") || s.endsWith(".bin") || s.endsWith(".att")
                || s.endsWith(".rlx.bin") || s.endsWith(".zhfst") || s.endsWith(".prob");
    }

    private static boolean looksLikeSource(String s) {
        return s.endsWith(".lexc") || s.endsWith(".lexd") || s.endsWith(".twol")
                || s.endsWith(".twoc") || s.endsWith(".rlx") || s.endsWith(".dix")
                || s.endsWith(".lsx") || s.endsWith(".metadix") || s.endsWith(".udx")
                || s.endsWith(".spellrelax");
    }

    private static int indexOfClose(String s, String close, int from) {
        return s.indexOf(close, from);
    }

    /** All nodes of a given type, in insertion order. */
    public List<Node> nodesOfType(NodeType type) {
        List<Node> out = new ArrayList<>();
        for (Node n : nodes.values()) if (n.type == type) out.add(n);
        return out;
    }

    /** Distinct tool names used anywhere in the build. */
    public Set<String> tools() {
        Set<String> out = new LinkedHashSet<>();
        for (Node n : nodes.values()) if (n.type == NodeType.TOOL) out.add(n.label);
        return out;
    }
}
