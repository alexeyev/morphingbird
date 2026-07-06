package io.github.alexeyev.morphingbird.core;
import java.nio.file.*; import java.util.*;
public class ModernRepoTest {
  public static void main(String[] a) throws Exception {
    for (String repo : new String[]{"/tmp/apertium-zab","/tmp/apertium-skr"}) {
      System.out.println("=== " + repo.replaceAll(".*/","") + " ===");
      SymbolIndex idx = new SymbolIndex();
      int ld=0,lc=0,tw=0,cg=0,dx=0,ud=0,sr=0;
      for (Path p : (Iterable<Path>)Files.list(Path.of(repo))::iterator) {
        String n = p.getFileName().toString();
        try {
          if(n.endsWith(".lexd")){idx.addLexd(p.toString(),LexdModel.parse(rd(p)));ld++;}
          else if(n.endsWith(".lexc")){idx.addLexc(p.toString(),LexcParser.parse(rd(p)));lc++;}
          else if(n.endsWith(".twol")){idx.addTwol(p.toString(),TwolModel.parse(rd(p)));tw++;}
          else if(n.endsWith(".rlx")){idx.addCg3(p.toString(),Cg3Model.parse(rd(p)));cg++;}
          else if(n.endsWith(".dix")||n.endsWith(".lsx")){idx.addDix(p.toString(),DixModel.parse(rd(p)));dx++;}
          else if(n.endsWith(".udx")){idx.addUdx(p.toString(),UdxModel.parse(rd(p)));ud++;}
          else if(n.endsWith(".spellrelax")){sr++;}  // NOT handled
        } catch(Exception e){ System.out.println("  CRASH on "+n+": "+e); }
      }
      System.out.println("  parsed: "+ld+" lexd, "+lc+" lexc, "+tw+" twol, "+cg+" cg3, "+dx+" dix, "+ud+" udx");
      System.out.println("  UNHANDLED: "+sr+" .spellrelax file(s)");
      System.out.println("  index stats: "+idx.stats());
      var d = idx.validate();
      long err = d.stream().filter(x->x.severity==SymbolIndex.Diagnostic.Severity.ERROR).count();
      System.out.println("  validation: "+d.size()+" diags, "+err+" errors");
      System.out.println();
    }
    System.out.println("MODERN REPO OK");
  }
  static String rd(Path p) throws Exception { return new String(Files.readAllBytes(p)); }
}
