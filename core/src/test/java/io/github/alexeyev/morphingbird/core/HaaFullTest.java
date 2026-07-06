package io.github.alexeyev.morphingbird.core;
import java.nio.file.*; import java.util.*;
public class HaaFullTest {
  public static void main(String[] a) throws Exception {
    String D="/tmp/apertium-haa/"; SymbolIndex idx=new SymbolIndex(); Map<String,String> fn=new HashMap<>();
    int ld=0,tw=0,cg=0,dx=0,ud=0;
    for (Path p : (Iterable<Path>)Files.list(Path.of(D))::iterator) {
      String n=p.getFileName().toString(); fn.put(p.toString(),n);
      try {
        if(n.endsWith(".lexd")){idx.addLexd(p.toString(),LexdModel.parse(rd(p)));ld++;}
        else if(n.endsWith(".twol")){idx.addTwol(p.toString(),TwolModel.parse(rd(p)));tw++;}
        else if(n.endsWith(".rlx")){idx.addCg3(p.toString(),Cg3Model.parse(rd(p)));cg++;}
        else if(n.endsWith(".dix")){idx.addDix(p.toString(),DixModel.parse(rd(p)));dx++;}
        else if(n.endsWith(".udx")){idx.addUdx(p.toString(),UdxModel.parse(rd(p)));ud++;}
      } catch(Exception e){ System.out.println("  FAIL "+n+": "+e); }
    }
    System.out.println("=== Hän FULL graph (lexd now included) ===");
    System.out.println("Parsed: "+ld+" lexd, "+tw+" twol, "+cg+" cg3, "+dx+" dix, "+ud+" udx");
    System.out.println("Stats: "+idx.stats());
    var diags=idx.validate();
    long err=diags.stream().filter(d->d.severity==SymbolIndex.Diagnostic.Severity.ERROR).count();
    System.out.println("\nValidation: "+diags.size()+" diags, "+err+" errors, "+(diags.size()-err)+" warnings");
    System.out.println("(was 71 warnings WITHOUT lexd — how many now?)");
    diags.forEach(d->System.out.println("   "+d.severity+": "+d.message));
    System.out.println();
    // Cross-format check: <np> declared in lexd, used in udx?
    for (String tag : new String[]{"<np>","<v>","<n>","<num>"}) {
      var decl=idx.tagDeclarations(tag); var use=idx.tagUsages(tag);
      Set<String> df=new TreeSet<>(), uf=new TreeSet<>();
      for(var l:decl)df.add(fn.get(l.file)); for(var l:use)uf.add(fn.get(l.file));
      System.out.println(tag+": declared in "+df+", used in "+uf);
    }
    boolean ok = err==0 && ld==1 && !idx.tagDeclarations("<np>").isEmpty();
    System.out.println("\n"+(ok?"HÄN CROSS-FORMAT OK":"PROBLEM"));
  }
  static String rd(Path p) throws Exception { return new String(Files.readAllBytes(p)); }
}
