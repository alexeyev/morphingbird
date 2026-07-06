package io.github.alexeyev.morphingbird.common;

import java.util.HashMap;
import java.util.Map;

/**
 * Human-readable glosses for the common Apertium tagset, so the editor can
 * explain a cryptic tag like {@code <gen>} on hover or in completion. Based on
 * the conventional Apertium tag inventory (see the Apertium wiki "Tagset"
 * page). This is a best-effort reference table, not exhaustive — unknown tags
 * simply have no gloss, and language-specific tags (e.g. {@code <s_1sg>}) are
 * handled by a small pattern fallback.
 */
public final class ApertiumTagset {
    private ApertiumTagset() {}

    private static final Map<String, String> GLOSS = new HashMap<>();

    static {
        // Parts of speech
        put("n", "Noun");
        put("v", "Verb");
        put("vblex", "Lexical verb");
        put("vbser", "Verb 'to be' (ser)");
        put("vbhaver", "Verb 'to have' (haber)");
        put("vbmod", "Modal verb");
        put("vaux", "Auxiliary verb");
        put("adj", "Adjective");
        put("adv", "Adverb");
        put("preadv", "Pre-adverb");
        put("predet", "Pre-determiner");
        put("det", "Determiner");
        put("prn", "Pronoun");
        put("pr", "Preposition");
        put("post", "Postposition");
        put("num", "Numeral");
        put("np", "Proper noun");
        put("ij", "Interjection");
        put("cnjcoo", "Coordinating conjunction");
        put("cnjsub", "Subordinating conjunction");
        put("cnjadv", "Adverbial conjunction");
        put("mod", "Modal word");
        put("part", "Particle");
        put("rel", "Relative");
        put("guio", "Hyphen / dash");
        put("sent", "Sentence-ending punctuation");
        put("cm", "Comma");
        put("lpar", "Left parenthesis");
        put("rpar", "Right parenthesis");
        put("lquot", "Left quotation mark");
        put("rquot", "Right quotation mark");
        put("apos", "Apostrophe");

        // Nominal features — case
        put("nom", "Nominative case");
        put("acc", "Accusative case");
        put("gen", "Genitive case");
        put("dat", "Dative case");
        put("abl", "Ablative case");
        put("loc", "Locative case");
        put("ins", "Instrumental case");
        put("voc", "Vocative case");
        put("erg", "Ergative case");
        put("abs", "Absolutive case");
        put("com", "Comitative case");
        put("equ", "Equative case");
        put("all", "Allative case");

        // Number / gender / person
        put("sg", "Singular");
        put("pl", "Plural");
        put("du", "Dual");
        put("sp", "Singular or plural");
        put("m", "Masculine");
        put("f", "Feminine");
        put("mf", "Masculine or feminine");
        put("nt", "Neuter");
        put("p1", "First person");
        put("p2", "Second person");
        put("p3", "Third person");

        // Verbal features — tense / aspect / mood
        put("pres", "Present tense");
        put("past", "Past tense");
        put("fut", "Future tense");
        put("pii", "Past imperfect");
        put("pis", "Past simple");
        put("pri", "Present indicative");
        put("prs", "Present subjunctive");
        put("ifi", "Preterite (indefinido)");
        put("imp", "Imperative");
        put("inf", "Infinitive");
        put("ger", "Gerund");
        put("pp", "Past participle");
        put("prs", "Present subjunctive");
        put("cni", "Conditional");
        put("fti", "Future indicative");
        put("impf", "Imperfective aspect");
        put("perf", "Perfective aspect");
        put("incp", "Inceptive aspect");
        put("opt", "Optative mood");
        put("neg", "Negative");
        put("refl", "Reflexive");

        // Determiner / pronoun subtypes
        put("def", "Definite");
        put("ind", "Indefinite");
        put("dem", "Demonstrative");
        put("qnt", "Quantifier");
        put("pers", "Personal");
        put("itg", "Interrogative");
        put("pos", "Possessive");
        put("ord", "Ordinal");
        put("card", "Cardinal");

        // Proper-noun subtypes
        put("ant", "Anthroponym (personal name)");
        put("cog", "Cognomen (family name)");
        put("top", "Toponym (place name)");
        put("al", "Other proper noun");

        // Verb valency
        put("tv", "Transitive verb");
        put("iv", "Intransitive verb");

        // Misc
        put("attr", "Attributive");
        put("subst", "Substantive");
        put("comp", "Comparative");
        put("sup", "Superlative");
        put("abbr", "Abbreviation");
    }

    private static void put(String tag, String gloss) { GLOSS.put(tag, gloss); }

    /**
     * Returns a human-readable gloss for a tag, or null if unknown. Accepts the
     * tag with or without angle brackets ({@code <nom>} or {@code nom}). Falls
     * back to decoding a few structured language-specific patterns.
     */
    public static String gloss(String tag) {
        String t = tag;
        if (t.startsWith("<") && t.endsWith(">")) t = t.substring(1, t.length() - 1);
        String g = GLOSS.get(t);
        if (g != null) return g;
        return structured(t);
    }

    /**
     * Decodes some structured Athabaskan/agglutinative tag conventions seen in
     * the wild, e.g. {@code s_1sg} (subject, 1st person singular), {@code o_3pl}
     * (object, 3rd person plural).
     */
    private static String structured(String t) {
        if (t.matches("[so]_[123](sg|pl|du)")) {
            String role = t.charAt(0) == 's' ? "Subject" : "Object";
            char person = t.charAt(2);
            String num = t.substring(3);
            String n = switch (num) {
                case "sg" -> "singular";
                case "pl" -> "plural";
                case "du" -> "dual";
                default -> num;
            };
            return role + " agreement: " + person + ordinal(person) + " person " + n;
        }
        return null;
    }

    private static String ordinal(char person) {
        return switch (person) {
            case '1' -> "st";
            case '2' -> "nd";
            case '3' -> "rd";
            default -> "th";
        };
    }

    /** Whether a gloss (built-in or structured) exists for this tag. */
    public static boolean isKnown(String tag) {
        return gloss(tag) != null;
    }
}
