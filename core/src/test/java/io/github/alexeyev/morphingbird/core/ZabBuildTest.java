package io.github.alexeyev.morphingbird.core;
import java.nio.file.*; import java.util.*;
public class ZabBuildTest {
  public static void main(String[] a) throws Exception {
    String repo="/tmp/apertium-zab";
    String modes=rd(repo+"/modes.xml"), mk=rd(repo+"/Makefile.am");
    List<String> srcs=new ArrayList<>();
    for (Path p : (Iterable<Path>)Files.list(Path.of(repo))::iterator) {
      String n=p.getFileName().toString();
      if(n.matches(".*\\.(lexc|lexd|twol|twoc|rlx|dix|lsx|metadix|udx|spellrelax)$")) srcs.add(n);
    }
    BuildGraphModel g = BuildGraphModel.build(modes, mk, srcs);
    System.out.println("=== zab build graph ===");
    System.out.println("sources recognized: " + srcs.size());
    System.out.println("  spellrelax in sources: " + srcs.stream().filter(s->s.endsWith(".spellrelax")).count());
    System.out.println("tips:");
    for (String t : BuildGraphRenderer.tips(g, srcs)) System.out.println("  • " + t);
  }
  static String rd(String p){ try { return new String(Files.readAllBytes(Path.of(p))); } catch(Exception e){ return ""; } }
}
