package io.github.alexeyev.morphingbird.core;
import java.util.*;
public class TipsPrecisionTest {
  public static void main(String[] a) {
    // Case 1: a Makefile that builds from foo.lexc but NOT from orphan.twol
    String mk = "LANG1=xyz\nBASENAME=apertium-$(LANG1)\n"
      + ".deps/$(LANG1).lexc.hfst: $(BASENAME).$(LANG1).lexc\n\thfst-lexc $< -o $@\n";
    String modes = "<modes><mode name=\"xyz-morph\"><pipeline>"
      + "<program name=\"hfst-proc\"><file name=\".deps/xyz.lexc.hfst\"/></program>"
      + "</pipeline></mode></modes>";
    List<String> srcs = Arrays.asList("apertium-xyz.xyz.lexc", "apertium-xyz.orphan.twol");
    BuildGraphModel g = BuildGraphModel.build(modes, mk, srcs);
    var tips = BuildGraphRenderer.tips(g, srcs);
    System.out.println("=== Tips (one real orphan expected) ===");
    tips.forEach(t -> System.out.println("  • " + t));
    long orphanWarnings = tips.stream().filter(t->t.contains("orphan.twol")).count();
    long lexcWarnings = tips.stream().filter(t->t.contains("xyz.lexc")&&t.contains("Heads-up")).count();
    System.out.println("\norphan.twol flagged (should be 1): " + orphanWarnings);
    System.out.println("xyz.lexc falsely flagged (should be 0): " + lexcWarnings);

    // Case 2: Hän — all sources consumed, expect ZERO heads-up
    java.nio.file.Path repo = java.nio.file.Path.of("/tmp/apertium-haa");
    try {
      String hmodes = new String(java.nio.file.Files.readAllBytes(repo.resolve("modes.xml")));
      String hmk = new String(java.nio.file.Files.readAllBytes(repo.resolve("Makefile.am")));
      List<String> hsrcs = Arrays.asList("apertium-haa.haa.lexd","apertium-haa.haa.twol","apertium-haa.haa.rlx","apertium-haa.post-haa.dix");
      BuildGraphModel hg = BuildGraphModel.build(hmodes, hmk, hsrcs);
      long heads = BuildGraphRenderer.tips(hg, hsrcs).stream().filter(t->t.contains("Heads-up")).count();
      System.out.println("\nHän heads-up warnings (should be 0 — all consumed): " + heads);
      boolean ok = orphanWarnings==1 && lexcWarnings==0 && heads==0;
      System.out.println("\n" + (ok ? "TIPS PRECISION OK" : "PROBLEM"));
    } catch(Exception e){ System.out.println("haa read failed: "+e); }
  }
}
