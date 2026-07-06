package io.github.alexeyev.morphingbird.core;
import java.util.*;
public class CompilerRunnerTest {
  public static void main(String[] a) {
    // 1. A VALID tiny lexc -> expect 0 errors
    String valid = "Multichar_Symbols\n%<n%>\nLEXICON Root\ncat:cat # ;\n";
    var r1 = CompilerRunner.compile(CompilerRunner.Kind.LEXC, "ok.lexc", valid, null);
    System.out.println("valid lexc: toolAvailable=" + r1.toolAvailable + " ("+r1.toolName+") diags=" + r1.diagnostics.size());
    for (var d : r1.diagnostics) System.out.println("   " + d);

    // 2. A BROKEN lexc: continuation to a missing lexicon + malformed -> expect error(s)
    String broken = "Multichar_Symbols\n%<n%>\nLEXICON Root\ncat:cat NOEXIST ;\n";
    var r2 = CompilerRunner.compile(CompilerRunner.Kind.LEXC, "bad.lexc", broken, null);
    System.out.println("\nbroken lexc: diags=" + r2.diagnostics.size());
    for (var d : r2.diagnostics) System.out.println("   " + d);

    // 3. A broken twol (bad syntax) -> expect error with a line
    String badTwol = "Alphabet\n a b ;\n\nRules\n\n\"bad rule\"\na:b <=> _ ;\nthis is not valid twol syntax @@@\n";
    var r3 = CompilerRunner.compile(CompilerRunner.Kind.TWOL, "bad.twol", badTwol, null);
    System.out.println("\nbroken twol: toolAvailable=" + r3.toolAvailable + " diags=" + r3.diagnostics.size());
    for (var d : r3.diagnostics) System.out.println("   " + d);

    System.out.println("\n" + (r1.toolAvailable && r2.toolAvailable ? "TOOLCHAIN REACHED" : "TOOLCHAIN MISSING"));
  }
}
