package io.github.alexeyev.morphingbird.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured model of a single parsed lexc file. Produced by {@link LexcParser}
 * and consumed by the cross-file {@link SymbolIndex}. Everything carries source
 * offsets so the IntelliJ layer can navigate and annotate.
 */
public final class LexcModel {

    /** A {@code LEXICON Name} declaration plus the entries under it. */
    public static final class Lexicon {
        public final String name;
        public final int nameStart;
        public final int nameEnd;
        public final List<Entry> entries = new ArrayList<>();

        public Lexicon(String name, int nameStart, int nameEnd) {
            this.name = name;
            this.nameStart = nameStart;
            this.nameEnd = nameEnd;
        }
    }

    /**
     * One entry line, e.g. {@code ибарат:ибарат N-INFL ;}. We record the
     * continuation-class reference (the identifier just before {@code ;}) and
     * the tags/archiphonemes that appear anywhere on the line.
     */
    public static final class Entry {
        /** Continuation-class name (may be {@code null} for {@code # ;} ends). */
        public String continuation;
        public int continuationStart;
        public int continuationEnd;
        /** Canonical tags used on this entry, e.g. {@code <gen>}. */
        public final List<SymbolRef> tags = new ArrayList<>();
        /** Canonical archiphonemes used, e.g. {@code {N}}. */
        public final List<SymbolRef> archiphonemes = new ArrayList<>();
        public int lineStart;
        public int lineEnd;
    }

    /** A reference to a tag or archiphoneme at a source location. */
    public static final class SymbolRef {
        public final String canonical;  // e.g. "<nom>" or "{A}"
        public final int start;
        public final int end;

        public SymbolRef(String canonical, int start, int end) {
            this.canonical = canonical;
            this.start = start;
            this.end = end;
        }
    }

    /** A multichar symbol declaration inside the {@code Multichar_Symbols} block. */
    public static final class SymbolDecl {
        public final String canonical;  // "<nom>" or "{A}"
        public final boolean isTag;     // true => tag, false => archiphoneme
        public final int start;
        public final int end;

        public SymbolDecl(String canonical, boolean isTag, int start, int end) {
            this.canonical = canonical;
            this.isTag = isTag;
            this.start = start;
            this.end = end;
        }
    }

    public final List<Lexicon> lexicons = new ArrayList<>();
    public final List<SymbolDecl> declaredSymbols = new ArrayList<>();
    /** Continuation-class references collected across all entries (for "find unresolved"). */
    public final List<SymbolRef> continuationRefs = new ArrayList<>();

    /**
     * Best offset at which to insert a new symbol into the
     * {@code Multichar_Symbols} block (end of the last existing declaration, or
     * just after the {@code Multichar_Symbols} keyword when the block is empty).
     * -1 when the file has no {@code Multichar_Symbols} block at all.
     */
    public int multicharInsertOffset = -1;

    /** Convenience: find a lexicon by name, or null. */
    public Lexicon lexicon(String name) {
        for (Lexicon l : lexicons) if (l.name.equals(name)) return l;
        return null;
    }

    /**
     * Whether a continuation-class name is a lexc built-in rather than a
     * user-defined LEXICON. {@code #} is the end-of-word terminator. These must
     * never be reported as "unresolved continuation".
     */
    public static boolean isBuiltinContinuation(String name) {
        return "#".equals(name);
    }

    /** A continuation reference is resolved if it is built-in or names a LEXICON. */
    public boolean isResolvedContinuation(String name) {
        return isBuiltinContinuation(name) || lexicon(name) != null;
    }
}
