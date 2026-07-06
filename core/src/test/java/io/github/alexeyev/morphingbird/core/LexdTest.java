package io.github.alexeyev.morphingbird.core;
import java.nio.file.*; import java.util.*; import java.util.stream.*;
public class LexdTest {
  public static void main(String[] a) throws Exception {
    String src = new String(Files.readAllBytes(Path.of("/tmp/apertium-haa/apertium-haa.haa.lexd")));
    LexdModel m = LexdModel.parse(src);
    System.out.println("=== LexdModel on Hän ===");
    System.out.println("tag refs: " + m.tagRefs.size() + " (" + m.tagRefs.stream().map(r->r.canonical).distinct().count() + " distinct)");
    System.out.println("archiphoneme refs: " + m.archiphonemeRefs.size() + " (" + m.archiphonemeRefs.stream().map(r->r.canonical).distinct().count() + " distinct: " + m.archiphonemeRefs.stream().map(r->r.canonical).distinct().sorted().collect(Collectors.toList()) + ")");
    System.out.println("definitions: " + m.definitions.size() + " (" + m.definitions.stream().filter(d->d.isPattern).count() + " patterns, " + m.definitions.stream().filter(d->!d.isPattern).count() + " lexicons)");
    System.out.println("name refs: " + m.nameRefs.size());
    System.out.println();
    // CRITICAL 1: comment-block tags must NOT be captured. The comments list <n>,<v>,<adj>...
    // but actual <n> uses exist too. Check a tag that ONLY appears in comments vs real.
    // <mod> appears in comment "# <mod>" AND in pattern "ModalWord [<mod>:]". Real use exists.
    // Verify offsets are all exact:
    boolean exact = m.tagRefs.stream().allMatch(r -> src.substring(r.start,r.end).equals(r.canonical));
    System.out.println("all tag offsets exact: " + exact);
    boolean archExact = m.archiphonemeRefs.stream().allMatch(r -> src.substring(r.start,r.end).equals(r.canonical));
    System.out.println("all archiphoneme offsets exact: " + archExact);
    // CRITICAL 2: no tag should start with '#' context — verify none of the captured tags
    // are inside a comment by checking the char before line start. Spot check:
    long inComment = m.tagRefs.stream().filter(r -> {
      int ls = src.lastIndexOf('\n', r.start);
      String lineToTag = src.substring(ls+1, r.start);
      return lineToTag.contains("#");
    }).count();
    System.out.println("tags captured inside a comment (must be 0): " + inComment);
    System.out.println();
    System.out.println("Sample tags: " + m.tagRefs.stream().map(r->r.canonical).distinct().limit(12).collect(Collectors.toList()));
    System.out.println("Lexicon names: " + m.definitions.stream().filter(d->!d.isPattern).map(d->d.name).limit(8).collect(Collectors.toList()));

    boolean ok = exact && archExact && inComment==0 && m.tagRefs.size()>50 && m.definitions.size()>=20;
    System.out.println("\n" + (ok ? "LEXD MODEL OK" : "LEXD MODEL PROBLEM"));
  }
}
