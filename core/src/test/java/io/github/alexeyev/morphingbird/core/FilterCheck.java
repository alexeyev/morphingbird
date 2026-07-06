package io.github.alexeyev.morphingbird.core;
import java.nio.file.*; import java.util.*; import java.util.stream.*;
public class FilterCheck {
  public static void main(String[] a) throws Exception {
    String src = new String(Files.readAllBytes(Path.of("/tmp/apertium-skr/apertium-skr.skr.lexd")));
    LexdModel m = LexdModel.parse(src);
    System.out.println("filters captured: " + m.filters.size());
    m.filters.stream().limit(8).forEach(f->System.out.println("  [" + f.body + "]  (offset " + f.start + ")"));
    // verify offsets exact
    boolean exact = m.filters.stream().allMatch(f->src.substring(f.start,f.end).equals("["+f.body+"]"));
    System.out.println("filter offsets exact: " + exact);
    // name refs should now mostly resolve
    Set<String> defs = m.definitions.stream().map(d->d.name).collect(Collectors.toSet());
    Set<String> refs = m.nameRefs.stream().map(r->r.canonical).collect(Collectors.toSet());
    long resolved = refs.stream().filter(defs::contains).count();
    System.out.println("name refs resolving to a def: " + resolved + "/" + refs.size());
    System.out.println("unresolved: " + refs.stream().filter(n->!defs.contains(n)).collect(Collectors.toList()));
  }
}
