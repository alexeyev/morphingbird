package io.github.alexeyev.morphingbird.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses an Apertium <b>apertium-regtest</b> {@code tests.json} — the standard
 * test configuration in modern modules. The file maps a test name to the input
 * file it feeds and the mode it runs:
 *
 * <pre>
 * {
 *   "tenses":     { "input": "tenses-input.txt",     "mode": "zab-morph" },
 *   "tenses-gen": { "input": "tenses-gen-input.txt", "mode": "zab-gener" }
 * }
 * </pre>
 *
 * <p>Linking each test to a mode lets Morphingbird connect the test suite to the
 * pipeline graph (each test exercises one mode) and surface the tests in the
 * build overview. A deliberately small, dependency-free JSON reader is used (the
 * schema is flat and well-known), keeping the core free of a JSON library and
 * unit-testable against real {@code tests.json} files.</p>
 */
public final class TestSuiteModel {

    /** One regtest entry. */
    public static final class Test {
        public final String name;
        public final String inputFile;
        public final String mode;
        public Test(String name, String inputFile, String mode) {
            this.name = name; this.inputFile = inputFile; this.mode = mode;
        }
    }

    public final List<Test> tests = new ArrayList<>();

    // Matches a top-level "key": { ... } object entry.
    private static final Pattern ENTRY =
            Pattern.compile("\"([^\"]+)\"\\s*:\\s*\\{([^}]*)\\}", Pattern.DOTALL);
    private static final Pattern FIELD =
            Pattern.compile("\"(input|mode)\"\\s*:\\s*\"([^\"]*)\"");

    public static TestSuiteModel parse(String json) {
        TestSuiteModel m = new TestSuiteModel();
        if (json == null) return m;
        Matcher em = ENTRY.matcher(json);
        while (em.find()) {
            String name = em.group(1);
            String body = em.group(2);
            String input = null, mode = null;
            Matcher fm = FIELD.matcher(body);
            while (fm.find()) {
                if (fm.group(1).equals("input")) input = fm.group(2);
                else mode = fm.group(2);
            }
            // Only accept entries that actually look like a test (have a mode or
            // input); this skips any unrelated nested objects.
            if (input != null || mode != null) {
                m.tests.add(new Test(name, input, mode));
            }
        }
        return m;
    }

    /** Tests grouped by the mode they exercise. */
    public Map<String, List<String>> byMode() {
        Map<String, List<String>> out = new LinkedHashMap<>();
        for (Test t : tests) {
            if (t.mode == null) continue;
            out.computeIfAbsent(t.mode, k -> new ArrayList<>()).add(t.name);
        }
        return out;
    }
}
