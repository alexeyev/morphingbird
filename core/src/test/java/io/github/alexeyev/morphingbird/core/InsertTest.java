package io.github.alexeyev.morphingbird.core;
import java.nio.file.*;
public class InsertTest {
  public static void main(String[] a) throws Exception {
    String f = "/home/claude/kir-buildtest/apertium-kir.kir.lexc";
    String src = new String(Files.readAllBytes(Path.of(f)));
    LexcModel m = LexcParser.parse(src);
    System.out.println("Multichar insert offset: " + m.multicharInsertOffset);
    // Show context around it
    int o = m.multicharInsertOffset;
    System.out.println("Context: ..." + src.substring(Math.max(0,o-30), Math.min(src.length(),o+10)).replace("\n","\\n") + "...");
    System.out.println("declared symbols: " + m.declaredSymbols.size());
    // The offset should be right after the last declared symbol
    LexcModel.SymbolDecl last = m.declaredSymbols.get(m.declaredSymbols.size()-1);
    System.out.println("last symbol '" + last.canonical + "' ends at " + last.end + " (offset matches: " + (o==last.end) + ")");
    // Via index
    SymbolIndex idx = new SymbolIndex();
    idx.addLexc(f, m);
    System.out.println("index.multicharInsertOffset: " + idx.multicharInsertOffset(f));
    System.out.println("index.primaryMulticharFile: " + (idx.primaryMulticharFile()!=null ? "found" : "null"));
    System.out.println(o==last.end && idx.multicharInsertOffset(f)==o ? "\nINSERT OFFSET OK" : "\nPROBLEM");
  }
}
