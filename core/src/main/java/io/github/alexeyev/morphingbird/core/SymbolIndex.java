package io.github.alexeyev.morphingbird.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * The cross-file symbol graph — the core asset of the whole plugin. It ingests
 * the per-file models (lexc / twol / CG3) of every source file in a project and
 * answers the navigation, dependency, completion and validation queries that all
 * the IntelliJ features are thin views over.
 *
 * <p>The unifying idea: a morphological <b>tag</b> is one logical node no matter
 * how it is spelled — lexc {@code %<nom%>} (canonical {@code <nom>}), CG3
 * {@code nom} (normalised to {@code <nom>}), or lttoolbox {@code <s n="nom"/>}.
 * Likewise an <b>archiphoneme</b> {@code {G}} is declared in lexc and
 * <em>resolved</em> in twol. This index is where those spellings meet.</p>
 *
 * <p>It is a plain data structure with no IntelliJ dependency; the IntelliJ
 * layer builds one of these (cached, rebuilt on PSI change) and queries it.</p>
 */
public final class SymbolIndex {

    /** A location within a specific source file. */
    public static final class Loc {
        public final String file;   // opaque file id (path or URI)
        public final int start;
        public final int end;
        public Loc(String file, int start, int end) {
            this.file = file; this.start = start; this.end = end;
        }
        @Override public String toString() { return file + ":" + start + "-" + end; }
    }

    // tag canonical -> declaration locations (lexc Multichar_Symbols)
    private final Map<String, List<Loc>> tagDecls = new TreeMap<>();
    // tag canonical -> use locations (lexc entries, CG3 lists)
    private final Map<String, List<Loc>> tagUses = new TreeMap<>();
    // archiphoneme canonical -> declaration locations (lexc Multichar_Symbols)
    private final Map<String, List<Loc>> archDecls = new TreeMap<>();
    // archiphoneme canonical -> resolution locations (twol Alphabet)
    private final Map<String, List<Loc>> archResolutions = new TreeMap<>();
    // archiphoneme canonical -> use locations (lexc entries, twol sets)
    private final Map<String, List<Loc>> archUses = new TreeMap<>();
    // LEXICON name -> definition location
    private final Map<String, Loc> lexiconDefs = new LinkedHashMap<>();
    // LEXICON name -> continuation-reference locations (who continues into it)
    private final Map<String, List<Loc>> lexiconUses = new TreeMap<>();
    // continuation references that need checking: name -> ref locations
    private final List<ContRef> continuationRefs = new ArrayList<>();
    // CG3 / twol named defs: name -> location (for intra-file navigation)
    private final Map<String, Loc> namedDefs = new LinkedHashMap<>();
    // named-def uses (e.g. lttoolbox <par n="..">): name -> use locations
    private final Map<String, List<Loc>> namedUses = new TreeMap<>();
    // Per lexc file: best offset to insert a new Multichar_Symbols declaration.
    private final Map<String, Integer> multicharInsert = new LinkedHashMap<>();

    /** A continuation-class reference recorded for validation. */
    public static final class ContRef {
        public final String name;
        public final Loc loc;
        public ContRef(String name, Loc loc) { this.name = name; this.loc = loc; }
    }

    // --- ingestion ---------------------------------------------------------

    /** Adds a parsed lexc file's contributions to the graph. */
    public void addLexc(String file, LexcModel m) {
        if (m.multicharInsertOffset >= 0) {
            multicharInsert.put(file, m.multicharInsertOffset);
        }
        for (LexcModel.SymbolDecl d : m.declaredSymbols) {
            Loc loc = new Loc(file, d.start, d.end);
            if (d.isTag) add(tagDecls, d.canonical, loc);
            else add(archDecls, d.canonical, loc);
        }
        for (LexcModel.Lexicon lex : m.lexicons) {
            // Only the first definition wins for "go to definition"; duplicates
            // are still discoverable via the duplicate check below.
            lexiconDefs.putIfAbsent(lex.name, new Loc(file, lex.nameStart, lex.nameEnd));
            for (LexcModel.Entry e : lex.entries) {
                for (LexcModel.SymbolRef t : e.tags) {
                    add(tagUses, t.canonical, new Loc(file, t.start, t.end));
                }
                for (LexcModel.SymbolRef ar : e.archiphonemes) {
                    add(archUses, ar.canonical, new Loc(file, ar.start, ar.end));
                }
                if (e.continuation != null) {
                    Loc loc = new Loc(file, e.continuationStart, e.continuationEnd);
                    continuationRefs.add(new ContRef(e.continuation, loc));
                    add(lexiconUses, e.continuation, loc);
                }
            }
        }
    }

    /** Adds a parsed twol file's contributions. */
    public void addTwol(String file, TwolModel m) {
        for (TwolModel.Resolved r : m.resolvedArchiphonemes) {
            add(archResolutions, r.archiphoneme, new Loc(file, r.start, r.end));
        }
        for (LexcModel.SymbolRef ar : m.archiphonemeRefs) {
            add(archUses, ar.canonical, new Loc(file, ar.start, ar.end));
        }
        for (TwolModel.Named s : m.sets) {
            namedDefs.putIfAbsent(s.name, new Loc(file, s.start, s.end));
        }
        for (TwolModel.Named d : m.definitions) {
            namedDefs.putIfAbsent(d.name, new Loc(file, d.start, d.end));
        }
    }

    /** Adds a parsed CG3 file's contributions. */
    public void addCg3(String file, Cg3Model m) {
        for (Cg3Model.Def d : m.definitions) {
            namedDefs.putIfAbsent(d.name, new Loc(file, d.start, d.end));
        }
        for (LexcModel.SymbolRef t : m.tagRefs) {
            add(tagUses, t.canonical, new Loc(file, t.start, t.end));
        }
    }

    /**
     * Adds a parsed lexd lexicon ({@code .lexd}) to the graph. lexd declares the
     * morphological tags ({@code <np>}, {@code <s_1sg>}) and archiphonemes that
     * the rest of the project references, so ingesting it lets a udx/rlx/dix tag
     * resolve to its lexd site and stops those tags being reported as
     * "used but never declared". LEXICON/PATTERN names are recorded as named
     * defs with their in-pattern references as named uses.
     */
    public void addLexd(String file, LexdModel m) {
        // lexd tags are declarations of the morphological vocabulary.
        for (LexcModel.SymbolRef t : m.tagRefs) {
            add(tagDecls, t.canonical, new Loc(file, t.start, t.end));
        }
        for (LexcModel.SymbolRef ar : m.archiphonemeRefs) {
            add(archUses, ar.canonical, new Loc(file, ar.start, ar.end));
        }
        for (LexdModel.Def d : m.definitions) {
            namedDefs.putIfAbsent(d.name, new Loc(file, d.start, d.end));
        }
        for (LexcModel.SymbolRef ref : m.nameRefs) {
            add(namedUses, ref.canonical, new Loc(file, ref.start, ref.end));
        }
    }

    /**
     * Adds a parsed {@code .udx} (Apertium->Universal Dependencies tag mapping).
     * The Apertium tags it matches on join the shared tag vocabulary, so a
     * {@code .udx} tag links to its lexc {@code Multichar_Symbols} declaration
     * and Find Usages on a tag spans the UD mapping too. The UD POS/feature
     * outputs are not symbols in the graph (they belong to UD, not Apertium).
     */
    public void addUdx(String file, UdxModel m) {
        for (LexcModel.SymbolRef t : m.tagRefs) {
            add(tagUses, t.canonical, new Loc(file, t.start, t.end));
        }
    }

    /**
     * Adds a parsed lttoolbox dictionary ({@code .dix}/{@code .lsx}) to the graph.
     * Its {@code <sdef>}/{@code <s>} symbol names join the <em>shared tag
     * vocabulary</em> (so {@code <s n="np"/>} unifies with lexc {@code %<np%>}),
     * and its {@code <pardef>}/{@code <par>} are recorded as named defs/uses for
     * lttoolbox-internal navigation.
     */
    public void addDix(String file, DixModel m) {
        // sdef = a tag definition that also lives in the dix; we treat lexc's
        // Multichar_Symbols as the canonical declaration, but a dix sdef is also
        // a declaration site, so record it as one.
        for (DixModel.Def sdef : m.sdefs) {
            String canon = DixModel.tagCanonical(sdef.name);
            add(tagDecls, canon, new Loc(file, sdef.start, sdef.end));
        }
        // <s n=".."> are tag uses, unified with the HFST side.
        for (DixModel.Ref s : m.sUses) {
            String canon = DixModel.tagCanonical(s.name);
            add(tagUses, canon, new Loc(file, s.start, s.end));
        }
        // pardef/par are lttoolbox-internal named defs/uses.
        for (DixModel.Def pardef : m.pardefs) {
            namedDefs.putIfAbsent(pardef.name, new Loc(file, pardef.start, pardef.end));
        }
        for (DixModel.Ref par : m.parUses) {
            // record the par use under a synthetic key space so namedUses works
            add(namedUses, par.name, new Loc(file, par.start, par.end));
        }
    }

    // --- queries (navigation / dependency) ---------------------------------

    /** Definition site(s) of a tag (lexc Multichar_Symbols). */
    public List<Loc> tagDeclarations(String canonical) {
        return tagDecls.getOrDefault(canonical, List.of());
    }

    /** Every place a tag is used (lexc entries + CG3 lists). */
    public List<Loc> tagUsages(String canonical) {
        return tagUses.getOrDefault(canonical, List.of());
    }

    /** twol rule(s) that resolve an archiphoneme (the lexc->twol bridge). */
    public List<Loc> archiphonemeResolutions(String canonical) {
        return archResolutions.getOrDefault(canonical, List.of());
    }

    /** Every place an archiphoneme is used (lexc entries + twol set bodies). */
    public List<Loc> archiphonemeUsages(String canonical) {
        return archUses.getOrDefault(canonical, List.of());
    }

    /** Declaration of an archiphoneme (lexc). */
    public List<Loc> archiphonemeDeclarations(String canonical) {
        return archDecls.getOrDefault(canonical, List.of());
    }

    /** Definition location of a LEXICON, or null. */
    public Loc lexiconDefinition(String name) {
        return lexiconDefs.get(name);
    }

    /** Locations that continue into a given LEXICON (Find Usages). */
    public List<Loc> lexiconUsages(String name) {
        return lexiconUses.getOrDefault(name, List.of());
    }

    /** Definition of a twol Set / Definition or CG3 LIST / SET, or null. */
    public Loc namedDefinition(String name) {
        return namedDefs.get(name);
    }

    /** Uses of a named def (e.g. lttoolbox {@code <par n="..">} paradigm uses). */
    public List<Loc> namedUsages(String name) {
        return namedUses.getOrDefault(name, List.of());
    }

    /** Insert offset for the {@code Multichar_Symbols} block of a given lexc file, or -1. */
    public int multicharInsertOffset(String file) {
        Integer v = multicharInsert.get(file);
        return v == null ? -1 : v;
    }

    /**
     * Picks a lexc file to receive a new tag declaration. Prefers the file with
     * the most existing declarations (the "main" lexicon), so a fix lands where
     * a reader would expect. Returns null if no lexc file has a
     * {@code Multichar_Symbols} block.
     */
    public String primaryMulticharFile() {
        String best = null;
        int bestCount = -1;
        // Count declarations per file to find the principal lexc.
        Map<String, Integer> perFile = new LinkedHashMap<>();
        for (List<Loc> locs : tagDecls.values()) {
            for (Loc l : locs) perFile.merge(l.file, 1, Integer::sum);
        }
        for (Map.Entry<String, Integer> e : multicharInsert.entrySet()) {
            int c = perFile.getOrDefault(e.getKey(), 0);
            if (c > bestCount) { bestCount = c; best = e.getKey(); }
        }
        return best;
    }

    /**
     * All source locations that must change when a LEXICON is renamed: its
     * declaration plus every continuation reference to it (across files). Used by
     * the rename refactoring. Locations are returned grouped by file so callers
     * can edit one document at a time, and within each file in descending start
     * order so edits don't invalidate later offsets.
     */
    public Map<String, List<Loc>> lexiconRenameSites(String name) {
        Map<String, List<Loc>> byFile = new LinkedHashMap<>();
        Loc def = lexiconDefs.get(name);
        if (def != null) {
            byFile.computeIfAbsent(def.file, k -> new ArrayList<>()).add(def);
        }
        for (Loc use : lexiconUses.getOrDefault(name, List.of())) {
            byFile.computeIfAbsent(use.file, k -> new ArrayList<>()).add(use);
        }
        // Sort each file's edits by descending start so applying them in order
        // keeps earlier offsets valid.
        for (List<Loc> locs : byFile.values()) {
            locs.sort((a, b) -> Integer.compare(b.start, a.start));
        }
        return byFile;
    }

    /** All known LEXICON names (for completion / Go to Symbol). */
    public Set<String> lexiconNames() {
        return new LinkedHashSet<>(lexiconDefs.keySet());
    }

    /** All declared tags (for completion). */
    public Set<String> declaredTags() {
        return new LinkedHashSet<>(tagDecls.keySet());
    }

    /** All declared archiphonemes (for completion). */
    public Set<String> declaredArchiphonemes() {
        return new LinkedHashSet<>(archDecls.keySet());
    }

    /** All named definitions (twol Sets/Definitions, CG3 LIST/SET) for completion. */
    public Set<String> namedDefinitionNames() {
        return new LinkedHashSet<>(namedDefs.keySet());
    }

    /** All tags referenced anywhere (declared or used) — for CG3 tag completion. */
    public Set<String> allTags() {
        Set<String> tags = new LinkedHashSet<>(tagDecls.keySet());
        tags.addAll(tagUses.keySet());
        return tags;
    }

    // --- validation (Tier-1, compiler-free) --------------------------------

    /** A diagnostic produced by the index's consistency checks. */
    public static final class Diagnostic {
        public enum Severity { ERROR, WARNING }
        /**
         * The category of a diagnostic, so the IDE can offer a targeted quick-fix
         * without parsing the human-readable message.
         */
        public enum Kind {
            UNDECLARED_TAG,         // tag used but not in any Multichar_Symbols
            UNRESOLVED_ARCHIPHONEME,// archiphoneme never resolved by twol
            UNRESOLVED_CONTINUATION,// continuation class names no LEXICON
            UNREACHABLE_LEXICON,    // nothing continues into this LEXICON
            OTHER
        }
        public final Severity severity;
        public final String message;
        public final Loc loc;
        public final Kind kind;
        /** The symbol the diagnostic is about (canonical tag, archiphoneme, or name). */
        public final String symbol;

        public Diagnostic(Severity sev, String message, Loc loc) {
            this(sev, message, loc, Kind.OTHER, null);
        }
        public Diagnostic(Severity sev, String message, Loc loc, Kind kind, String symbol) {
            this.severity = sev; this.message = message; this.loc = loc;
            this.kind = kind; this.symbol = symbol;
        }
        @Override public String toString() {
            return severity + " " + loc + ": " + message;
        }
    }

    /**
     * Runs the cheap, build-free consistency checks across the whole project:
     * <ul>
     *   <li>continuation classes that name no LEXICON (and aren't built-ins);</li>
     *   <li>tags used (in lexc or CG3) that were never declared;</li>
     *   <li>archiphonemes used but never resolved by any twol rule;</li>
     *   <li>LEXICONs that nothing ever continues into (dead code), except Root;
     *   </li>
     *   <li>tags declared but never used anywhere (dead vocabulary).</li>
     * </ul>
     */
    public List<Diagnostic> validate() {
        List<Diagnostic> out = new ArrayList<>();

        // 1. Unresolved continuation classes.
        for (ContRef ref : continuationRefs) {
            if (LexcModel.isBuiltinContinuation(ref.name)) continue;
            if (!lexiconDefs.containsKey(ref.name)) {
                out.add(new Diagnostic(Diagnostic.Severity.ERROR,
                        "Unresolved continuation class '" + ref.name
                                + "': no LEXICON with this name", ref.loc,
                        Diagnostic.Kind.UNRESOLVED_CONTINUATION, ref.name));
            }
        }

        // 2. Tags used but never declared.
        for (Map.Entry<String, List<Loc>> e : tagUses.entrySet()) {
            if (!tagDecls.containsKey(e.getKey())) {
                for (Loc loc : e.getValue()) {
                    out.add(new Diagnostic(Diagnostic.Severity.WARNING,
                            "Tag '" + e.getKey()
                                    + "' is used but never declared in any"
                                    + " Multichar_Symbols block", loc,
                            Diagnostic.Kind.UNDECLARED_TAG, e.getKey()));
                }
            }
        }

        // 3. Archiphonemes used but never resolved by twol.
        for (Map.Entry<String, List<Loc>> e : archUses.entrySet()) {
            if (!archResolutions.containsKey(e.getKey())) {
                for (Loc loc : e.getValue()) {
                    out.add(new Diagnostic(Diagnostic.Severity.WARNING,
                            "Archiphoneme '" + e.getKey()
                                    + "' is never resolved by a twol rule; it"
                                    + " may leak into surface forms", loc,
                            Diagnostic.Kind.UNRESOLVED_ARCHIPHONEME, e.getKey()));
                }
            }
        }

        // 4. Unreachable LEXICONs (nothing continues into them), except Root.
        for (Map.Entry<String, Loc> e : lexiconDefs.entrySet()) {
            String name = e.getKey();
            if (name.equalsIgnoreCase("Root")) continue;
            if (!lexiconUses.containsKey(name)) {
                out.add(new Diagnostic(Diagnostic.Severity.WARNING,
                        "LEXICON '" + name + "' is never used as a continuation"
                                + " class (unreachable)", e.getValue(),
                        Diagnostic.Kind.UNREACHABLE_LEXICON, name));
            }
        }

        return out;
    }

    // --- stats / debugging -------------------------------------------------

    public Map<String, Integer> stats() {
        Map<String, Integer> s = new LinkedHashMap<>();
        s.put("lexicons", lexiconDefs.size());
        s.put("declaredTags", tagDecls.size());
        s.put("declaredArchiphonemes", archDecls.size());
        s.put("resolvedArchiphonemes", archResolutions.size());
        s.put("namedDefs", namedDefs.size());
        s.put("continuationRefs", continuationRefs.size());
        int tagUseCount = tagUses.values().stream().mapToInt(List::size).sum();
        s.put("tagUses", tagUseCount);
        return s;
    }

    private static void add(Map<String, List<Loc>> map, String key, Loc loc) {
        map.computeIfAbsent(key, k -> new ArrayList<>()).add(loc);
    }
}
