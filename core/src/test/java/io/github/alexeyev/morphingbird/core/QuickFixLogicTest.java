package io.github.alexeyev.morphingbird.core;
import java.nio.file.*; import java.util.*;
public class QuickFixLogicTest {
  public static void main(String[] a) throws Exception {
    // Simulate: a project where a tag is used in CG3 but not declared in lexc.
    // Build a small lexc with a Multichar_Symbols block + a CG3 using an extra tag.
    String lexc = "Multichar_Symbols\n%<n%>\n%<nom%>\n\nLEXICON Root\ncat:cat N ;\n\nLEXICON N\n%<n%>%<nom%>: # ;\n";
    String cg3  = "LIST Noun = n ;\nSELECT (acc) ;\n";  // 'acc' is used but not declared

    SymbolIndex idx = new SymbolIndex();
    LexcModel lm = LexcParser.parse(lexc);
    idx.addLexc("test.lexc", lm);
    idx.addCg3("test.rlx", Cg3Model.parse(cg3));

    System.out.println("=== Simulate quick-fix for undeclared tag ===");
    // Find the UNDECLARED_TAG diagnostic
    var diags = idx.validate();
    for (var d : diags) {
      if (d.kind == SymbolIndex.Diagnostic.Kind.UNDECLARED_TAG) {
        System.out.println("Diagnostic: " + d.message);
        System.out.println("  kind=" + d.kind + ", symbol=" + d.symbol);
      }
    }
    // Simulate what AddTagDeclarationFix does:
    String target = idx.primaryMulticharFile();
    int offset = idx.multicharInsertOffset(target);
    System.out.println("\nFix target file: " + target);
    System.out.println("Insert offset: " + offset);
    // Apply the insertion the fix would make for '<acc>'
    String inner = "acc";
    String insertion = "\n%<" + inner + "%>";
    String result = lexc.substring(0, offset) + insertion + lexc.substring(offset);
    System.out.println("\n--- Multichar_Symbols block after fix ---");
    System.out.println(result.substring(0, result.indexOf("\n\nLEXICON")));

    // Verify: re-parse the fixed lexc, <acc> now declared, warning gone
    SymbolIndex idx2 = new SymbolIndex();
    idx2.addLexc("test.lexc", LexcParser.parse(result));
    idx2.addCg3("test.rlx", Cg3Model.parse(cg3));
    long accWarn = idx2.validate().stream().filter(d->d.kind==SymbolIndex.Diagnostic.Kind.UNDECLARED_TAG && "<acc>".equals(d.symbol)).count();
    System.out.println("\n<acc> declared after fix: " + (!idx2.tagDeclarations("<acc>").isEmpty()));
    System.out.println("<acc> warning remaining: " + accWarn);
    System.out.println(accWarn==0 && !idx2.tagDeclarations("<acc>").isEmpty() ? "\nQUICK-FIX LOGIC OK" : "\nPROBLEM");
  }
}
