package io.github.alexeyev.morphingbird.core;
import java.nio.file.*; import java.util.*; import java.util.stream.*;
public class BuildGraphTest {
  public static void main(String[] a) throws Exception {
    for (String repo : new String[]{"/tmp/apertium-kaz","/tmp/apertium-haa"}) {
      String modes = readOpt(repo+"/modes.xml");
      String mk = readOpt(repo+"/Makefile.am");
      List<String> srcs = new ArrayList<>();
      for (Path p : (Iterable<Path>)Files.list(Path.of(repo))::iterator) {
        String n = p.getFileName().toString();
        if (n.matches(".*\\.(lexc|lexd|twol|twoc|rlx|dix|lsx|metadix)$")) srcs.add(n);
      }
      BuildGraphModel g = BuildGraphModel.build(modes, mk, srcs);
      System.out.println("=== " + repo.replaceAll(".*/","") + " ===");
      System.out.println("  nodes: " + g.nodes.size() + " (src=" + g.nodesOfType(BuildGraphModel.NodeType.SOURCE).size()
        + " tool=" + g.nodesOfType(BuildGraphModel.NodeType.TOOL).size()
        + " artifact=" + g.nodesOfType(BuildGraphModel.NodeType.ARTIFACT).size()
        + " mode=" + g.nodesOfType(BuildGraphModel.NodeType.MODE).size() + ")");
      System.out.println("  edges: " + g.edges.size());
      System.out.println("  tools detected: " + g.tools());
      System.out.println("  modes: " + g.modePipelines.size() + " -> " + g.modePipelines.keySet().stream().limit(6).collect(Collectors.toList()));
      // Show a sample pipeline
      var first = g.modePipelines.entrySet().iterator().next();
      System.out.println("  sample pipeline '" + first.getKey() + "': " + first.getValue());
      // Show some edges
      System.out.println("  sample edges:");
      g.edges.stream().limit(6).forEach(e -> System.out.println("     " + g.nodes.get(e.from).label + " -> " + g.nodes.get(e.to).label));
      System.out.println();
    }
    System.out.println("BUILD GRAPH DONE");
  }
  static String readOpt(String p) { try { return new String(Files.readAllBytes(Path.of(p))); } catch(Exception e){ return ""; } }
}
