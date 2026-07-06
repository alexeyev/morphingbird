package io.github.alexeyev.morphingbird.core;
import java.nio.file.*;
public class DixTest {
  public static void main(String[] a) throws Exception {
    String lsx = new String(Files.readAllBytes(Path.of("/home/claude/kir-buildtest/apertium-kir.kir-seg-prefix.lsx")));
    String dix = new String(Files.readAllBytes(Path.of("/home/claude/kir-buildtest/apertium-kir.post-kir.dix")));

    System.out.println("=== .lsx ===");
    DixModel ml = DixModel.parse(lsx);
    System.out.println("sdefs: " + ml.sdefs.size() + " " + names(ml.sdefs));
    System.out.println("s uses: " + ml.sUses.size());
    System.out.println("pardefs: " + ml.pardefs.size() + " " + names(ml.pardefs));
    System.out.println("par uses: " + ml.parUses.size());
    // Verify offsets land exactly on the name
    for (var d : ml.sdefs) {
      String got = lsx.substring(d.start, d.end);
      if (!got.equals(d.name)) System.out.println("  OFFSET BUG: '"+got+"' != '"+d.name+"'");
    }
    System.out.println("  all sdef offsets exact: " + ml.sdefs.stream().allMatch(d->lsx.substring(d.start,d.end).equals(d.name)));

    System.out.println("\n=== .dix ===");
    DixModel md = DixModel.parse(dix);
    System.out.println("sdefs: " + md.sdefs.size());
    System.out.println("s uses: " + md.sUses.size());
    System.out.println("pardefs: " + md.pardefs.size() + " " + names(md.pardefs));
    System.out.println("par uses: " + md.parUses.size());

    // Check no false matches from <section> being caught by <s
    System.out.println("\n=== False-positive check: does <s match <section? ===");
    String test = "<section id=\"main\"><s n=\"np\"/><sdef n=\"x\"/></section>";
    DixModel mt = DixModel.parse(test);
    System.out.println("s uses in test (should be 1, just the real <s>): " + mt.sUses.size());
    for (var r : mt.sUses) System.out.println("   <s n=\"" + r.name + "\">");
    System.out.println("sdefs in test (should be 1): " + mt.sdefs.size());

    boolean ok = ml.sdefs.size()==2 && md.pardefs.size()>=1 && mt.sUses.size()==1 && mt.sdefs.size()==1
      && ml.sdefs.stream().allMatch(d->lsx.substring(d.start,d.end).equals(d.name));
    System.out.println("\n" + (ok ? "DIX MODEL OK" : "DIX MODEL PROBLEM"));
  }
  static String names(java.util.List<DixModel.Def> ds){
    StringBuilder s=new StringBuilder("[");
    for(var d:ds) s.append(d.name).append(" ");
    return s.append("]").toString();
  }
}
