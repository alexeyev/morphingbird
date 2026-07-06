package io.github.alexeyev.morphingbird.core;
import java.nio.file.*; import java.util.*; import java.util.stream.*;
public class LexdNavTest {
  public static void main(String[] a) throws Exception {
    String src = new String(Files.readAllBytes(Path.of("/tmp/apertium-skr/apertium-skr.skr.lexd")));
    LexdModel m = LexdModel.parse(src);
    System.out.println("=== What LexdModel captures for navigation (skr) ===");
    System.out.println("definitions: " + m.definitions.size());
    System.out.println("  patterns: " + m.definitions.stream().filter(d->d.isPattern).map(d->d.name).collect(Collectors.toList()));
    System.out.println("  lexicons (first 10): " + m.definitions.stream().filter(d->!d.isPattern).map(d->d.name).limit(10).collect(Collectors.toList()));
    System.out.println("name refs: " + m.nameRefs.size());
    System.out.println("  sample refs: " + m.nameRefs.stream().map(r->r.canonical).distinct().limit(12).collect(Collectors.toList()));
    System.out.println();
    // KEY QUESTION: does a name-ref resolve to its definition?
    // 'NounRoot' is referenced in PATTERN NOUN but is it DEFINED? (it's a LEXICON elsewhere)
    Set<String> defNames = m.definitions.stream().map(d->d.name).collect(Collectors.toSet());
    Set<String> refNames = m.nameRefs.stream().map(r->r.canonical).collect(Collectors.toSet());
    System.out.println("refs that resolve to a def: " + refNames.stream().filter(defNames::contains).count() + "/" + refNames.size());
    System.out.println("refs WITHOUT a matching def (first 10): " + refNames.stream().filter(n->!defNames.contains(n)).limit(10).collect(Collectors.toList()));
    System.out.println();
    System.out.println("GAP: lexicon tag-filters like NounRoot[I,m], NounRoot[m,-unmarked,-I]");
    long filters = src.lines().filter(l->l.matches(".*\\w+\\[[a-zA-Z,_-]+\\].*")).count();
    System.out.println("  lines with [tag-filter]: " + filters + " (NOT modeled)");
  }
}
