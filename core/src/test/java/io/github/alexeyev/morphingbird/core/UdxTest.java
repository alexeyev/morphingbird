package io.github.alexeyev.morphingbird.core;
import java.nio.file.*; import java.util.*; import java.util.stream.*;
public class UdxTest {
  public static void main(String[] a) throws Exception {
    for (String f : new String[]{"/tmp/apertium-yua/apertium-yua.yua.udx","/tmp/apertium-kaz/apertium-kaz.kaz.udx"}) {
      String src = new String(Files.readAllBytes(Path.of(f)));
      UdxModel m = UdxModel.parse(src);
      System.out.println("=== " + f.replaceAll(".*/","") + " ===");
      System.out.println("  mappings: " + m.mappings.size());
      System.out.println("  tag refs: " + m.tagRefs.size());
      System.out.println("  distinct tags: " + m.tagRefs.stream().map(r->r.canonical).distinct().count());
      // Verify offsets land exactly on the tag text
      boolean exact = m.tagRefs.stream().allMatch(r -> src.substring(r.start,r.end).equals(r.canonical.substring(1,r.canonical.length()-1)));
      System.out.println("  all offsets exact: " + exact);
      // Show a few sample mappings
      System.out.println("  samples:");
      m.mappings.stream().filter(mp->!mp.apertiumTags.isEmpty()).limit(5).forEach(mp->
        System.out.println("    " + mp.apertiumTags + " -> POS=" + (mp.udPos.isEmpty()?"-":mp.udPos) + " feats=" + (mp.udFeats.isEmpty()?"-":mp.udFeats)));
      // CRITICAL: no UD features/POS leaked as tags
      var bad = m.tagRefs.stream().map(r->r.canonical).filter(t->t.contains("=")||t.matches("<[A-Z]+>")).collect(Collectors.toList());
      System.out.println("  leaked UD POS/feats as tags (should be empty): " + bad);
      System.out.println();
    }
    System.out.println("UDX MODEL DONE");
  }
}
