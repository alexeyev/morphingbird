package io.github.alexeyev.morphingbird.core;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A lightweight, dependency-free reader for Apertium {@code .prob} tagger model
 * files — enough to give useful hints <em>without</em> running
 * {@code apertium-tagger}.
 *
 * <p>A trained {@code .prob} is a binary serialization (Apertium's
 * {@code Compression} multibyte format) of an HMM tagger: a tagset, a set of
 * ambiguity classes (the tag-combinations the tagger learned to disambiguate),
 * and the transition/emission matrices. The strings are stored as
 * length-prefixed wide-character sequences, which appear in the byte stream as a
 * {@code 0x01 <len> (0x01 <char>)*} pattern. We decode just those strings — the
 * tagset and ambiguity-class vocabulary — and leave the probability matrices
 * alone (they are floating-point data with no navigational value).</p>
 *
 * <p>What this enables:</p>
 * <ul>
 *   <li><b>Untrained-placeholder detection.</b> {@code apertium-init} ships a
 *       fixed default {@code .prob}; many repos commit it unchanged. It has a
 *       known fingerprint and decodes to no real tagset, so Morphingbird can warn that
 *       the tagger has not actually been trained.</li>
 *   <li><b>Tagset recovery.</b> For a trained model, the distinct tags it knows
 *       — which the IDE can cross-check against the lexc/lexd
 *       {@code Multichar_Symbols} to flag a tagger trained on a different/older
 *       tagset than the current morphology.</li>
 *   <li><b>Model size.</b> The number of ambiguity classes, a rough proxy for
 *       how much the tagger distinguishes.</li>
 * </ul>
 *
 * <p>This is heuristic by nature (a reverse-engineered read of a binary format),
 * so it reports only what it can decode with confidence and never fabricates.</p>
 */
public final class ProbModel {

    /** MD5 of the default {@code .prob} shipped by {@code apertium-init} (untrained). */
    private static final String DEFAULT_PROB_MD5 = "c5e8275d57e234e4824a5251c9c8e4be";

    public final boolean looksUntrained;
    public final int byteSize;
    /** Distinct lowercase tags decoded from the model (empty if none/untrained). */
    public final Set<String> tags;
    /** All decoded tokens in order (tags + ambiguity-class members + any words). */
    public final List<String> tokens;
    /** A short human-readable note about the model's state. */
    public final String note;
    /** How often each tag appears across the decoded ambiguity classes. */
    public final java.util.Map<String, Integer> tagFrequency;

    private ProbModel(boolean untrained, int size, Set<String> tags,
                      List<String> tokens, String note,
                      java.util.Map<String, Integer> tagFrequency) {
        this.looksUntrained = untrained;
        this.byteSize = size;
        this.tags = tags;
        this.tokens = tokens;
        this.note = note;
        this.tagFrequency = tagFrequency;
    }

    /** Parses a {@code .prob} from its raw bytes. */
    public static ProbModel parse(byte[] data) {
        int size = data == null ? 0 : data.length;
        if (data == null || data.length == 0) {
            return new ProbModel(true, 0, Set.of(), List.of(),
                    "Empty .prob file.", java.util.Map.of());
        }

        // 1. Known untrained default? (fixed fingerprint, or decodes to nothing.)
        boolean defaultFingerprint = DEFAULT_PROB_MD5.equals(md5(data));

        // 2. Decode the embedded strings.
        List<String> tokens = decodeStrings(data);
        Set<String> tags = new LinkedHashSet<>();
        java.util.Map<String, Integer> freq = new java.util.LinkedHashMap<>();
        for (String t : tokens) {
            if (isTag(t)) {
                tags.add(t);
                freq.merge(t, 1, Integer::sum);
            }
        }

        boolean untrained = defaultFingerprint || tags.isEmpty();
        String note;
        if (defaultFingerprint) {
            note = "This looks like the untrained default .prob shipped by "
                    + "apertium-init — train it with apertium-tagger before relying "
                    + "on disambiguation.";
        } else if (tags.isEmpty()) {
            note = "No tagset could be decoded from this .prob; it may be empty, "
                    + "untrained, or in a format Morphingbird does not read.";
        } else {
            note = "Trained tagger model: knows " + tags.size() + " tags.";
        }
        return new ProbModel(untrained, size, tags, tokens, note, freq);
    }

    /**
     * Decodes the length-prefixed wide-char strings from the multibyte stream.
     * The pattern is {@code 0x01 <len> (0x01 <char>)*len}; anything else is a
     * probability/count value we skip.
     */
    private static List<String> decodeStrings(byte[] d) {
        List<String> out = new ArrayList<>();
        int i = 0;
        while (i + 1 < d.length) {
            if ((d[i] & 0xff) == 0x01) {
                int len = d[i + 1] & 0xff;
                int j = i + 2;
                StringBuilder sb = new StringBuilder();
                boolean ok = len > 0;
                for (int k = 0; k < len; k++) {
                    if (j + 1 < d.length && (d[j] & 0xff) == 0x01) {
                        int ch = d[j + 1] & 0xff;
                        // Accept printable ASCII and high-byte UTF-8 lead bytes are
                        // not handled here; tags are ASCII, which is what we want.
                        if (ch >= 32 && ch < 127) { sb.append((char) ch); j += 2; }
                        else { ok = false; break; }
                    } else { ok = false; break; }
                }
                if (ok && sb.length() == len) {
                    out.add(sb.toString());
                    i = j;
                    continue;
                }
            }
            i++;
        }
        return out;
    }

    /** Apertium tags are lowercase letters / digits / underscore, short (e.g. {@code ger_perf}). */
    private static boolean isTag(String s) {
        if (s.isEmpty() || s.length() > 16) return false;
        boolean hasLetter = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'a' && c <= 'z') hasLetter = true;
            else if (!((c >= '0' && c <= '9') || c == '_')) return false;
        }
        return hasLetter;
    }

    private static String md5(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] h = md.digest(data);
            StringBuilder sb = new StringBuilder(h.length * 2);
            for (byte b : h) sb.append(Character.forDigit((b >> 4) & 0xf, 16))
                    .append(Character.forDigit(b & 0xf, 16));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /** Canonical {@code <tag>} forms of the decoded tagset, for graph cross-checks. */
    public Set<String> canonicalTags() {
        Set<String> out = new LinkedHashSet<>();
        for (String t : tags) out.add("<" + t + ">");
        return out;
    }

    /** Tags sorted by how often they appear in ambiguity classes (most-used first). */
    public List<java.util.Map.Entry<String, Integer>> tagsByFrequency() {
        List<java.util.Map.Entry<String, Integer>> list =
                new ArrayList<>(tagFrequency.entrySet());
        list.sort((a, b) -> {
            int c = Integer.compare(b.getValue(), a.getValue());
            return c != 0 ? c : a.getKey().compareTo(b.getKey());
        });
        return list;
    }

    /** The single most-used tag, or null. */
    public String topTag() {
        var byFreq = tagsByFrequency();
        return byFreq.isEmpty() ? null : byFreq.get(0).getKey();
    }

    /**
     * Result of comparing the tagger's tagset against the project's morphology
     * tagset: which tags are shared, which only the morphology emits (the tagger
     * can't disambiguate them), and which only the tagger carries (likely removed
     * from the morphology since training).
     */
    public static final class CrossCheck {
        public final List<String> shared;
        public final List<String> morphologyOnly;
        public final List<String> taggerOnly;
        public CrossCheck(List<String> shared, List<String> morphologyOnly,
                          List<String> taggerOnly) {
            this.shared = shared; this.morphologyOnly = morphologyOnly;
            this.taggerOnly = taggerOnly;
        }
        public boolean isClean() { return morphologyOnly.isEmpty() && taggerOnly.isEmpty(); }
    }

    /** Compares this model's tagset against a set of canonical morphology tags. */
    public CrossCheck crossCheck(Set<String> morphologyTags) {
        List<String> shared = new ArrayList<>();
        List<String> morphOnly = new ArrayList<>();
        List<String> taggerOnly = new ArrayList<>();
        Set<String> probTags = canonicalTags();
        if (morphologyTags == null) morphologyTags = Set.of();
        for (String t : probTags) {
            if (morphologyTags.contains(t)) shared.add(t); else taggerOnly.add(t);
        }
        for (String t : morphologyTags) {
            if (!probTags.contains(t)) morphOnly.add(t);
        }
        java.util.Collections.sort(shared);
        java.util.Collections.sort(morphOnly);
        java.util.Collections.sort(taggerOnly);
        return new CrossCheck(shared, morphOnly, taggerOnly);
    }
}
