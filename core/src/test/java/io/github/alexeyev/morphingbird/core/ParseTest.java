package io.github.alexeyev.morphingbird.core;
import java.util.*;
public class ParseTest {
  public static void main(String[] a) {
    // Real hfst-lexc syntax error -> should parse to line 5 (or 2)
    String b2 = "Multichar_Symbols\n%<n%>\nLEXICON Root\ncat:cat # ;\nLEXICON\n";
    var r2 = CompilerRunner.compile(CompilerRunner.Kind.LEXC, "bad2.lexc", b2, null);
    System.out.println("bad2 diags=" + r2.diagnostics.size());
    for (var d : r2.diagnostics) System.out.println("   " + d);

    String b3 = "LEXICON Root\n: : : ;;;\n";
    var r3 = CompilerRunner.compile(CompilerRunner.Kind.LEXC, "bad3.lexc", b3, null);
    System.out.println("bad3 diags=" + r3.diagnostics.size());
    for (var d : r3.diagnostics) System.out.println("   " + d);

    boolean ok = r2.diagnostics.stream().anyMatch(d->d.line==5)
              && r3.diagnostics.stream().anyMatch(d->d.line==2);
    System.out.println(ok ? "PARSE OK (lines 5 and 2 detected)" : "PARSE PROBLEM");
  }
}
