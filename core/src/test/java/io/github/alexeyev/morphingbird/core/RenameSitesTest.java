package io.github.alexeyev.morphingbird.core;
import java.nio.file.*; import java.util.*;
public class RenameSitesTest {
  public static void main(String[] a) throws Exception {
    String DIR="/home/claude/kir-buildtest/";
    SymbolIndex idx = new SymbolIndex();
    idx.addLexc(DIR+"apertium-kir.kir.lexc", LexcParser.parse(new String(Files.readAllBytes(Path.of(DIR+"apertium-kir.kir.lexc")))));
    // Pick a moderately-used lexicon and check rename sites = 1 decl + N uses
    String name = "CLIT-NONPRED";
    var sites = idx.lexiconRenameSites(name);
    int total = sites.values().stream().mapToInt(List::size).sum();
    int uses = idx.lexiconUsages(name).size();
    boolean hasDef = idx.lexiconDefinition(name) != null;
    System.out.println("lexicon: " + name);
    System.out.println("  declaration present: " + hasDef);
    System.out.println("  usages: " + uses);
    System.out.println("  total rename sites: " + total + " (expect " + (uses + (hasDef?1:0)) + ")");
    // Verify descending order within file (so edits stay valid)
    boolean ordered = true;
    for (var locs : sites.values())
      for (int i=1;i<locs.size();i++) if (locs.get(i-1).start < locs.get(i).start) ordered=false;
    System.out.println("  edits in descending offset order: " + ordered);
    // Verify every site's text actually equals the name (no off-by-one in offsets)
    String src = new String(Files.readAllBytes(Path.of(DIR+"apertium-kir.kir.lexc")));
    boolean allMatch = true; int checked=0;
    for (var e : sites.entrySet())
      for (var loc : e.getValue()) {
        String snippet = src.substring(loc.start, loc.end);
        if (!snippet.equals(name)) { allMatch=false; System.out.println("    MISMATCH at "+loc.start+": '"+snippet+"'"); }
        checked++;
      }
    System.out.println("  all " + checked + " sites' text == name: " + allMatch);
    System.out.println(hasDef && total==uses+1 && ordered && allMatch ? "RENAME SITES OK" : "RENAME SITES PROBLEM");
  }
}
