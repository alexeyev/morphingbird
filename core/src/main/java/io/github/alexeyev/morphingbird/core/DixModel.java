package io.github.alexeyev.morphingbird.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A lenient extractor for lttoolbox dictionary XML ({@code .dix} / {@code .lsx}
 * / {@code .metadix}). For the cross-file symbol graph we need:
 * <ul>
 *   <li><b>{@code <sdef n="np"/>}</b> — symbol (tag) <em>definitions</em>;</li>
 *   <li><b>{@code <s n="np"/>}</b> — symbol (tag) <em>uses</em>. These share the
 *       morphological tag vocabulary with the HFST side: {@code n="np"} is the
 *       same logical tag as lexc {@code %<np%>} / CG3 {@code np}, so the index
 *       unifies them as {@code <np>};</li>
 *   <li><b>{@code <pardef n="..">}</b> paradigm definitions and
 *       <b>{@code <par n="..">}</b> uses — lttoolbox-internal cross-references.</li>
 * </ul>
 *
 * <p>We use regex rather than a DOM parse on purpose: it is offset-accurate (we
 * need exact source ranges for navigation), resilient to partial/being-edited
 * files, and avoids pulling a parser into the IntelliJ-free core. The patterns
 * are anchored to the lttoolbox schema's attribute forms.</p>
 */
public final class DixModel {

    /** A {@code <sdef>} / {@code <pardef>} definition with the name's source range. */
    public static final class Def {
        public final String name;
        public final int start;   // start of the name attribute *value*
        public final int end;
        public Def(String name, int start, int end) {
            this.name = name; this.start = start; this.end = end;
        }
    }

    /** A reference (a {@code <s>} tag use or a {@code <par>} use). */
    public static final class Ref {
        public final String name;
        public final int start;
        public final int end;
        public Ref(String name, int start, int end) {
            this.name = name; this.start = start; this.end = end;
        }
    }

    public final List<Def> sdefs = new ArrayList<>();   // tag definitions
    public final List<Ref> sUses = new ArrayList<>();   // tag uses (<s n="..">)
    public final List<Def> pardefs = new ArrayList<>(); // paradigm definitions
    public final List<Ref> parUses = new ArrayList<>(); // paradigm uses (<par n="..">)

    // Match an attribute value and report the value's offsets. We match the
    // element name then the n="..." attribute. Group 1 is the value.
    private static final Pattern SDEF =
            Pattern.compile("<sdef\\b[^>]*?\\bn=\"([^\"]*)\"");
    private static final Pattern S_USE =
            Pattern.compile("<s\\b[^>]*?\\bn=\"([^\"]*)\"");
    private static final Pattern PARDEF =
            Pattern.compile("<pardef\\b[^>]*?\\bn=\"([^\"]*)\"");
    private static final Pattern PAR_USE =
            Pattern.compile("<par\\b[^>]*?\\bn=\"([^\"]*)\"");

    public static DixModel parse(String src) {
        DixModel m = new DixModel();
        collectDefs(SDEF, src, m.sdefs);
        collectRefs(S_USE, src, m.sUses);
        collectDefs(PARDEF, src, m.pardefs);
        collectRefs(PAR_USE, src, m.parUses);
        return m;
    }

    private static void collectDefs(Pattern p, String src, List<Def> out) {
        Matcher mt = p.matcher(src);
        while (mt.find()) {
            out.add(new Def(mt.group(1), mt.start(1), mt.end(1)));
        }
    }

    private static void collectRefs(Pattern p, String src, List<Ref> out) {
        Matcher mt = p.matcher(src);
        while (mt.find()) {
            out.add(new Ref(mt.group(1), mt.start(1), mt.end(1)));
        }
    }

    /** Canonical tag form for a lttoolbox symbol name, e.g. {@code np} -> {@code <np>}. */
    public static String tagCanonical(String name) {
        return "<" + name + ">";
    }
}
