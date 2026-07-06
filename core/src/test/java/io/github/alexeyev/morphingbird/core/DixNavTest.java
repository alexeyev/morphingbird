package io.github.alexeyev.morphingbird.core;
import java.nio.file.*; import java.util.*;
public class DixNavTest {
  public static void main(String[] a) throws Exception {
    String D="/home/claude/kir-buildtest/";
    SymbolIndex idx = new SymbolIndex();
    idx.addLexc(D+"lexc", LexcParser.parse(rd(D+"apertium-kir.kir.lexc")));
    idx.addDix(D+"lsx", DixModel.parse(rd(D+"apertium-kir.kir-seg-prefix.lsx")));

    System.out.println("=== Simulating: click <s/<sdef n=\"np\"> in .lsx -> Kind.TAG, key '<np>' ===");
    // This is exactly what DixReferenceContributor produces: Kind.TAG with key "<np>"
    var decls = idx.tagDeclarations("<np>");
    System.out.println("resolve targets for <np>:");
    SymbolIndex.Loc lexcTarget = null;
    for (var loc : decls) {
      System.out.println("   " + loc);
      if (loc.file.contains("lexc")) lexcTarget = loc;
    }
    System.out.println("\n-> Clicking the lsx tag jumps to the lexc declaration: " 
        + (lexcTarget != null ? "YES (" + lexcTarget + ")" : "NO"));

    // And clicking <par n="inputs"> -> Kind.NAMED, key "inputs" -> pardef def
    System.out.println("\n=== Simulating: click <par n=\"inputs\"> -> Kind.NAMED, key 'inputs' ===");
    var pardef = idx.namedDefinition("inputs");
    System.out.println("-> resolves to pardef def: " + (pardef != null ? pardef : "NONE"));

    boolean ok = lexcTarget != null && pardef != null;
    System.out.println("\n" + (ok ? "DIX NAVIGATION OK" : "PROBLEM"));
  }
  static String rd(String p) throws Exception { return new String(Files.readAllBytes(Path.of(p))); }
}
