package io.github.alexeyev.morphingbird.core;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ParserSmokeTest {
    public static void main(String[] args) throws Exception {
        String path = args.length > 0 ? args[0]
                : "/home/claude/kir-buildtest/apertium-kir.kir.lexc";
        String src = new String(Files.readAllBytes(Path.of(path)));

        long t0 = System.nanoTime();
        LexcModel m = LexcParser.parse(src);
        long ms = (System.nanoTime() - t0) / 1_000_000;

        System.out.println("parsed in " + ms + " ms");
        System.out.println("lexicons: " + m.lexicons.size());
        System.out.println("declared symbols: " + m.declaredSymbols.size()
                + " (tags=" + m.declaredSymbols.stream().filter(s -> s.isTag).count()
                + ", archiphonemes="
                + m.declaredSymbols.stream().filter(s -> !s.isTag).count() + ")");
        int entries = m.lexicons.stream().mapToInt(l -> l.entries.size()).sum();
        System.out.println("total entries: " + entries);
        System.out.println("continuation refs: " + m.continuationRefs.size());

        System.out.println("\n-- first 8 lexicons --");
        m.lexicons.stream().limit(8).forEach(l ->
                System.out.println("  LEXICON " + l.name + "  (" + l.entries.size() + " entries)"));

        System.out.println("\n-- sample entries from 'Nouns' --");
        LexcModel.Lexicon nouns = m.lexicon("Nouns");
        if (nouns != null) {
            nouns.entries.stream().limit(5).forEach(e ->
                    System.out.println("  cont=" + e.continuation
                            + " tags=" + e.tags.size()
                            + " arch=" + e.archiphonemes.size()));
        }

        // How many continuation refs point at a non-existent LEXICON?
        long unresolved = m.continuationRefs.stream()
                .filter(r -> !m.isResolvedContinuation(r.canonical))
                .count();
        System.out.println("\ncontinuation refs unresolved (excluding built-ins): " + unresolved);
        // Show a few distinct ones
        m.continuationRefs.stream()
                .map(r -> r.canonical)
                .distinct()
                .filter(name -> !m.isResolvedContinuation(name))
                .limit(15)
                .forEach(name -> System.out.println("   ? " + name));
    }
}
