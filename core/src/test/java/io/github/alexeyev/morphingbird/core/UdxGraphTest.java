package io.github.alexeyev.morphingbird.core;
import java.nio.file.*; import java.util.*;
public class UdxGraphTest {
  public static void main(String[] a) throws Exception {
    String D="/tmp/apertium-kaz/";
    SymbolIndex idx = new SymbolIndex();
    Map<String,String> fn = new HashMap<>();
    for (Path p : (Iterable<Path>)Files.list(Path.of(D))::iterator) {
      String n=p.getFileName().toString(); fn.put(p.toString(),n);
      if(n.endsWith(".lexc")) idx.addLexc(p.toString(), LexcParser.parse(rd(p)));
      else if(n.endsWith(".twol")) idx.addTwol(p.toString(), TwolModel.parse(rd(p)));
      else if(n.endsWith(".rlx")) idx.addCg3(p.toString(), Cg3Model.parse(rd(p)));
      else if(n.endsWith(".dix")) idx.addDix(p.toString(), DixModel.parse(rd(p)));
      else if(n.endsWith(".udx")) idx.addUdx(p.toString(), UdxModel.parse(rd(p)));
    }
    System.out.println("=== .udx joined the graph? ===");
    // <nom> is in udx (Case=Nom), lexc (declaration), and CG3. Find usages should span all.
    for (String tag : new String[]{"<nom>","<gen>","<dem>"}) {
      var uses = idx.tagUsages(tag);
      Set<String> files = new TreeSet<>();
      for (var loc : uses) files.add(fn.get(loc.file));
      var decl = idx.tagDeclarations(tag);
      System.out.println(tag + ": declared " + decl.size() + "x, used " + uses.size() + "x across " + files);
      boolean inUdx = files.stream().anyMatch(f->f.endsWith(".udx"));
      System.out.println("   -> appears in .udx: " + inUdx + " | jumps to lexc decl: " + decl.stream().anyMatch(l->fn.get(l.file).endsWith(".lexc")));
    }
    // Simulate: click a tag in the .udx -> resolves to lexc declaration
    System.out.println("\n=== Navigation FROM .udx ===");
    var nomDecl = idx.tagDeclarations("<nom>");
    String lexcDecl = nomDecl.stream().filter(l->fn.get(l.file).endsWith(".lexc")).map(Object::toString).findFirst().orElse("NONE");
    System.out.println("Clicking 'nom' in .udx -> lexc declaration at " + lexcDecl);
    boolean ok = idx.tagUsages("<nom>").stream().anyMatch(l->fn.get(l.file).endsWith(".udx")) && !nomDecl.isEmpty();
    System.out.println("\n" + (ok ? "UDX GRAPH OK" : "PROBLEM"));
  }
  static String rd(Path p) throws Exception { return new String(Files.readAllBytes(p)); }
}
