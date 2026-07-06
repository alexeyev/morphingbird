package io.github.alexeyev.morphingbird.core;
import java.nio.file.*; import java.util.*;
public class ProbTest {
  public static void main(String[] a) throws Exception {
    String[][] cases = {
      {"kaz","/tmp/apertium-kaz/kaz.prob","trained-large"},
      {"zab","/tmp/apertium-zab/zab.prob","trained-tiny"},
      {"skr","/tmp/apertium-skr/skr.prob","untrained-default"},
      {"haa","/tmp/apertium-haa/haa.prob","untrained-default"}
    };
    for (String[] c : cases) {
      byte[] data = Files.readAllBytes(Path.of(c[1]));
      ProbModel m = ProbModel.parse(data);
      System.out.println("=== " + c[0] + ".prob (" + c[2] + ") ===");
      System.out.println("  size: " + m.byteSize + " bytes");
      System.out.println("  looksUntrained: " + m.looksUntrained);
      System.out.println("  tags decoded: " + m.tags.size());
      if (m.tags.size()>0) System.out.println("  sample tags: " + m.tags.stream().limit(15).toList());
      System.out.println("  note: " + m.note);
      System.out.println();
    }
    // Assertions
    ProbModel kaz = ProbModel.parse(Files.readAllBytes(Path.of("/tmp/apertium-kaz/kaz.prob")));
    ProbModel skr = ProbModel.parse(Files.readAllBytes(Path.of("/tmp/apertium-skr/skr.prob")));
    boolean ok = !kaz.looksUntrained && kaz.tags.size()>100 && kaz.tags.contains("nom")
      && skr.looksUntrained && skr.note.contains("apertium-init");
    System.out.println(ok ? "PROB MODEL OK" : "PROBLEM");
  }
}
