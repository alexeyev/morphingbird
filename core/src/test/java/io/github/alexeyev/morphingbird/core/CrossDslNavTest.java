package io.github.alexeyev.morphingbird.core;
import java.nio.file.*; import java.util.*;
public class CrossDslNavTest {
  public static void main(String[] a) throws Exception {
    String DIR="/home/claude/kir-buildtest/";
    SymbolIndex idx = new SymbolIndex();
    idx.addLexc(DIR+"l", LexcParser.parse(rd(DIR+"apertium-kir.kir.lexc")));
    idx.addTwol(DIR+"t", TwolModel.parse(rd(DIR+"apertium-kir.kir.twol")));
    idx.addCg3(DIR+"c", Cg3Model.parse(rd(DIR+"apertium-kir.kir.rlx")));

    System.out.println("=== Simulating the goto-handler resolution paths ===\n");

    // 1. CG3 TAG "nom" -> normalize to <nom> -> tagDeclarations (lexc)
    String cg3tag = "nom";
    String canon = "<" + cg3tag + ">";
    var tagDecl = idx.tagDeclarations(canon);
    System.out.println("CG3 tag '" + cg3tag + "' -> lexc declaration: " 
        + (tagDecl.isEmpty() ? "NONE (broken!)" : tagDecl.get(0)));

    // 2. twol ARCHIPHONEME %{G%} -> {G} -> archiphonemeDeclarations (lexc)
    String arch = "{G}";
    var archDecl = idx.archiphonemeDeclarations(arch);
    System.out.println("twol archiphoneme '%{G%}' -> lexc declaration: "
        + (archDecl.isEmpty() ? "NONE (broken!)" : archDecl.get(0)));

    // 3. CG3 SETNAME "Vow"/"Nouns" -> namedDefinition
    for (String setName : new String[]{"Nouns","FiniteVerb","Cns"}) {
      var def = idx.namedDefinition(setName);
      System.out.println("named def '" + setName + "' -> " 
          + (def == null ? "NONE" : def));
    }

    // 4. completion sources non-empty?
    System.out.println("\n=== Completion sources ===");
    System.out.println("allTags(): " + idx.allTags().size() + " tags");
    System.out.println("namedDefinitionNames(): " + idx.namedDefinitionNames().size() + " names");
    System.out.println("declaredArchiphonemes(): " + idx.declaredArchiphonemes().size());

    boolean ok = !tagDecl.isEmpty() && !archDecl.isEmpty() 
        && idx.namedDefinition("Nouns")!=null
        && idx.allTags().size()>50 && idx.namedDefinitionNames().size()>100;
    System.out.println("\n" + (ok ? "CROSS-DSL NAV + COMPLETION DATA OK" : "PROBLEM"));
  }
  static String rd(String p) throws Exception { return new String(Files.readAllBytes(Path.of(p))); }
}
