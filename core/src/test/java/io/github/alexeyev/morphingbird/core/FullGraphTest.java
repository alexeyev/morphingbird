package io.github.alexeyev.morphingbird.core;
import java.nio.file.*; import java.util.*;
public class FullGraphTest {
  public static void main(String[] a) throws Exception {
    String D="/home/claude/kir-buildtest/";
    SymbolIndex idx = new SymbolIndex();
    idx.addLexc(D+"lexc", LexcParser.parse(rd(D+"apertium-kir.kir.lexc")));
    idx.addTwol(D+"twol", TwolModel.parse(rd(D+"apertium-kir.kir.twol")));
    idx.addCg3(D+"cg3", Cg3Model.parse(rd(D+"apertium-kir.kir.rlx")));
    idx.addDix(D+"lsx", DixModel.parse(rd(D+"apertium-kir.kir-seg-prefix.lsx")));
    idx.addDix(D+"dix", DixModel.parse(rd(D+"apertium-kir.post-kir.dix")));

    System.out.println("=== The 'two toolkits, one graph' claim ===\n");
    // The .lsx declares <sdef n="np"/>. lexc declares %<np%>. They must unify as <np>.
    var npDecls = idx.tagDeclarations("<np>");
    System.out.println("Tag <np> declaration sites (lexc Multichar + lsx sdef):");
    for (var loc : npDecls) System.out.println("   " + loc);
    boolean lexcDecl = npDecls.stream().anyMatch(l -> l.file.contains("lexc"));
    boolean lsxDecl  = npDecls.stream().anyMatch(l -> l.file.contains("lsx"));
    System.out.println("   -> declared in lexc: " + lexcDecl + ", declared in lsx: " + lsxDecl);

    // <np> usages should now span lexc entries, CG3 lists, AND lsx
    var npUses = idx.tagUsages("<np>");
    Set<String> files = new TreeSet<>();
    for (var loc : npUses) files.add(loc.file.replaceAll(".*/",""));
    System.out.println("\nTag <np> used at " + npUses.size() + " sites across files: " + files);

    // pardef navigation inside lttoolbox
    System.out.println("\nlttoolbox pardef 'inputs' def: " + idx.namedDefinition("inputs"));
    System.out.println("lttoolbox pardef 'inputs' uses: " + idx.namedUsages("inputs").size());

    boolean ok = lexcDecl && lsxDecl && idx.namedDefinition("inputs") != null;
    System.out.println("\n" + (ok ? "CROSS-TOOLKIT GRAPH OK — lttoolbox joined the HFST graph" : "PROBLEM"));
  }
  static String rd(String p) throws Exception { return new String(Files.readAllBytes(Path.of(p))); }
}
