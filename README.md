# Morphingbird

**Apertium-like structured projects on finite-state morphology support for IntelliJ-based IDEs.**

![CI](https://github.com/alexeyev/morphingbird/actions/workflows/ci.yml/badge.svg)


Language support targeting [Apertium](https://www.apertium.org/)-style morphological-transducer
projects in IntelliJ-based IDEs. It understands ten cooperating source formats —
**lexc**, **lexd**, **twol**, **twoc**, **CG3** (`.rlx`), lttoolbox
**`.dix`/`.lsx`/`.metadix`** XML, the Apertium→UD **`.udx`** mapping, and
**`.spellrelax`** — and builds a **cross-file symbol graph** so you can navigate
"what comes from where" and "what depends on what" across files and across formats
(hopefully).

| This repository is a contributor-built effort and is **not** an official Apertium or JetBrains product. |
|-|

---

## What it does

- **Syntax highlighting** for lexc, twol, and CG3, with distinct colors for the
  load-bearing symbols (tags, archiphonemes, continuation classes, rule names).
- **Structure view** for lexc: one node per `LEXICON` with its entry count — the
  primary way to navigate an 18k-line lexicon.
- **Cross-file, cross-DSL navigation** (works *from* lexc, twol, CG3, and
  lttoolbox `.dix`/`.lsx`):
  - a continuation-class reference → its `LEXICON` (possibly in another file);
  - a tag `<nom>` → its declaration in `Multichar_Symbols`, from lexc, from a CG3
    `nom`, or from a lttoolbox `<s n="nom"/>` / `<sdef n="nom"/>`;
  - an archiphoneme `{G}` → the **twol** rule that resolves it (the lexc↔twol jump
    nothing else provides), and from twol back to its lexc declaration;
  - twol/CG3 Set/LIST names → their definitions; lttoolbox `<par>` → `<pardef>`.
- **Go to Symbol** over every `LEXICON` in the project.
- **Find Usages** and **reference highlighting** in lexc, twol, and CG3, via real
  PSI references resolved through the symbol index — so Find Usages on a tag lists
  its lexc *and* CG3 *and* lttoolbox sites together.
- **Rename** a `LEXICON` across the whole project (declaration + every continuation
  reference) in one undoable command.
- **Customizable syntax colors** for every language — Settings → Editor → Color
  Scheme has an *Apertium lexc / twol / CG3 / lexd / UD mapping / spellrelax* page,
  each with a live, fully-representative preview so every token color can be tuned.
- **Index-driven completion** in lexc (continuation classes, tags, archiphonemes),
  twol (archiphonemes, sets), and CG3 (tags, LIST/SET names) — tags are offered with
  their plain-language gloss shown inline (e.g. `nom`  Nominative case).
- **Hover documentation**: hovering a tag explains it (`<gen>` → "Genitive case"),
  shows how many times it is declared and used, and warns if it is never declared;
  archiphonemes show whether a twol rule resolves them; LEXICON / set names show
  where they are defined. Structured agreement tags are decoded too — `s_1sg` →
  "Subject agreement: 1st person singular".
- **Quick-fixes**: an "undeclared tag" warning offers a one-click *Declare tag
  `<nom>` in Multichar_Symbols* fix that inserts the tag (correctly `%`-escaped) into
  the principal lexicon's symbol block, then reveals the edit.
- **Structure view** for lexc (lexicons), twol (sets, definitions, rules), and CG3
  (LIST/SET definitions, sections).
- **Two-tier validation**:
  - **Tier 1 (instant, compiler-free):** cross-file consistency the compiler does
    *not* surface — unresolved continuation classes, tags used but never declared,
    archiphonemes never resolved by any twol rule, unreachable lexicons. Each
    diagnostic carries a structured kind, so the IDE can attach a targeted quick-fix.
  - **Tier 2 (background, ground-truth):** compiles the current file with the real
    toolchain (`hfst-lexc` / `hfst-twolc` / `cg-comp`) on an isolated temp copy and
    surfaces the compiler's own diagnostics inline.
- **"Apertium mode" run configuration** with a `modes.xml`-driven mode picker, input
  fed on stdin, and clickable `file:line:col` compiler errors in the run console.
- **lttoolbox `.dix`/`.lsx`/`.metadix`** treated as schema-driven XML, so IntelliJ's
  bundled XML support provides validation, completion, and structure.
- **Apertium→Universal Dependencies `.udx` mappings** parsed and joined to the shared
  tag graph: the Apertium tags a `.udx` maps (e.g. `nom` → `Case=Nom`) navigate to
  their lexc declarations, Find Usages spans them, and the validator flags a `.udx`
  that maps a tag the morphology never declares.
- **Tagger model (`.prob`) analysis** — without running `apertium-tagger`. Right-click
  a `.prob` file and choose *Analyze Tagger Model* to open a visual HTML report: a
  trained/untrained status banner, summary stat cards, the model's tagset ranked by how
  much it relies on each tag (with plain-language glosses and proportional bars), and a
  cross-check against the project's morphology that flags tags the tagger can't
  disambiguate (in the morphology but missing from the model) and tags the model still
  carries that the morphology no longer emits — a concrete "retrain" signal. The same
  analysis also appears as a section in the "Show Build Pipeline" document.
- **"Run Apertium Regression Tests" action** (Tools menu): runs `apertium-regtest`
  for the module and streams the results into a console, with `file:line:col`
  diagnostics (across all source formats) made clickable. Available whenever the
  project has a `test/tests.json`. Shells the real tool, so results match the command
  line exactly (the module must be compiled first, as regtest needs the artifacts).
- **"Show Build Pipeline" action** (Tools menu): reconstructs the module's build and
  runtime pipeline from `modes.xml` and the Makefile, then opens a generated Markdown
  document with a Mermaid flowchart (source files → compiler tools → built artifacts →
  modes) plus plain-language tips — what toolkits are in use, the analysis/generation
  split, and a heads-up for any source file that no build rule seems to consume.
- **lexd `.lexd` lexicons** (Apertium's pattern/lexicon format, used by Athabaskan and
  other languages): tags, archiphonemes, and LEXICON/PATTERN names parsed and joined to
  the shared graph, with syntax highlighting that distinguishes tags, archiphonemes,
  and morphophonological sieves.

---

## Architecture

The design separates a pure, testable core from the IDE integration.

### `core/` — the symbol-graph engine (zero dependencies)

Plain Java, no IntelliJ imports, independently unit-testable:

- `LexcScanner` / `LexcToken` — a handwritten escape-aware lexc scanner. The `%`
  escape is the single hardest detail in the language (`%<n%>` is a tag, `%{A%}` an
  archiphoneme, `%>` a literal boundary, `% ` a literal space, `%!` a literal bang).
  The scanner computes both the raw text and the canonical escape-stripped symbol.
- `LexcParser` / `LexcModel` — a lenient parser producing lexicons, entries,
  continuation references, and `Multichar_Symbols` declarations. Never throws.
- `TwolModel` — extracts the twol `Alphabet` archiphoneme resolutions (the lexc↔twol
  bridge), named sets, and rule names.
- `Cg3Model` — extracts CG3 `LIST`/`SET` definitions and tag references, normalising
  bare CG3 tags (`nom`) to the canonical `<nom>` so they unify across DSLs.
- `SymbolIndex` — the centerpiece: ingests the per-file models and answers all the
  navigation, dependency, completion, rename, and Tier-1 validation queries. A tag is
  one logical node whether written as lexc `%<np%>`, CG3 `np`, or lttoolbox
  `<s n="np"/>`.
- `CompilerRunner` — Tier-2: runs the real toolchain on an isolated temp copy and
  parses diagnostics. Also IntelliJ-free, so it is testable against an installed
  toolchain.

### `idea-plugin/` — the IntelliJ integration

Thin adapters over the core:

- **Languages** (`lexc`, `twol`, `cg3` packages): `Language`, `FileType`, `Lexer`
  (wrapping the core scanners), `SyntaxHighlighter`, `ParserDefinition` (flat PSI —
  structure and intelligence come from the index, not a heavy PSI tree), commenters,
  brace matcher, structure view, color settings, completion.
- **`index.MorphingbirdIndexService`** — a project service that builds and caches the
  core `SymbolIndex`, invalidated on any PSI change. (No stub index in v1 — a cached
  whole-project map is effectively instant at single-repo scale; a `StubIndex` is the
  documented scaling step.)
- **`refs`** — `PsiReferenceContributor` (the idiomatic way to enable Find Usages,
  Rename, and highlighting at once), `FindUsagesProvider`, and the rename action.
- **`nav`** — go-to-declaration and Go-to-Symbol, index-backed.
- **`validation`** — the Tier-1 annotator (index diagnostics) and the Tier-2
  `ExternalAnnotator` (background compiler).
- **`run`** — the run configuration, type/factory, settings editor, `modes.xml`
  reader, toolchain locator, console filter, gutter run-marker, and config producer.
- **`lttoolbox`** — registers the dix schema identifiers for the XML half.

---

## Building

A multi-module Gradle build (wrapper included; the IntelliJ Platform Gradle
plugin is declared in `idea-plugin/build.gradle.kts`). Run from the repo root:

```bash
./gradlew :core:build                  # build + test the dependency-free core
./gradlew :morphingbird:buildPlugin    # → idea-plugin/build/distributions/morphingbird-<version>.zip
./gradlew :morphingbird:runIde         # launches a sandbox IDE with the plugin
./gradlew :morphingbird:verifyPlugin   # JetBrains Plugin Verifier (downloads IDEs; CI runs this)
```

(The plugin subproject lives in the `idea-plugin/` directory but its Gradle
project name is `morphingbird`, so the distribution and its content root are
named after the plugin.)

`core` is a real project dependency of the plugin — the distribution zip
contains `core-<version>.jar` alongside the plugin jar.

### Running the core tests

`core` has a JUnit 5 suite (`CoreTest`, 9 self-contained tests against a committed
fixture) that is the actual CI gate — this is what `./gradlew :core:build` runs:

```bash
cd core
./gradlew test                                          # via Gradle (what CI runs)

# or, without Gradle:
javac -d build $(find src/main/java -name '*.java')
javac -cp "build:junit-platform-console-standalone.jar" -d build \
    src/test/java/io/github/alexeyev/morphingbird/core/CoreTest.java \
    src/test/java/io/github/alexeyev/morphingbird/core/TestFixtures.java
java -jar junit-platform-console-standalone.jar execute -cp build \
    -c io.github.alexeyev.morphingbird.core.CoreTest
```

`core/src/test/` also holds a set of plain-`main()` exploration scripts
(`ScannerAssertTest`, `HaaFullTest`, `ZabBuildTest`, `CompilerRunnerTest`, and others)
used during development to check the engine against real Apertium repos
(`apertium-kir`, `-kaz`, `-haa`, `-yua`, `-zab`, `-skr`) and an installed HFST
toolchain. They have no `@Test` annotations, so `./gradlew test` does not run them —
they are ad hoc, run manually with `javac` + `java -cp` against a local toolchain and
corpora, not part of the automated gate.

---

## Verification status (read this honestly)

Different parts of this project carry **different levels of assurance**, and it is
worth being precise about which is which.

**Compiled and behaviourally tested (strongest):**

- The entire `core/` engine is compiled and run against the **real `apertium-kir`
  sources**. Verified facts include: the scanner round-trips the full 626 KB lexc
  byte-for-byte; the parser resolves **all 15,763** continuation references with zero
  false "unresolved"; the index builds the whole-project graph and its Tier-1
  validation produces **0 false errors** (the 8 warnings were each inspected and
  confirmed to be genuine latent issues, including tags the authors themselves marked
  `! FIXME`); rename-site offsets land **exactly** on the symbol text at all sites;
  the cross-DSL jumps resolve correctly (a CG3 `nom` and a lttoolbox `<s n="np"/>`
  both reach the lexc declaration); and the **cross-toolkit graph** unifies tags
  across the two FST toolkits (tag `<np>` is declared in both the lexc
  `Multichar_Symbols` and the lttoolbox `.lsx`, and the `.lsx` symbol resolves to the
  lexc site). The `DixModel` extractor is checked against the real `.dix`/`.lsx`,
  including a false-positive guard (a `<section>` element is not mistaken for an
  `<s>` tag).
- `CompilerRunner` is tested against the **real installed hfst toolchain**: it parses
  actual `hfst-lexc` diagnostics (`file:line.col`) and `hfst-twolc` syntax errors to
  the correct positions. (It also confirmed that `hfst-lexc` does *not* flag a missing
  continuation class — which is exactly why the Tier-1 index check is not redundant.)

**Compiled against the real platform (strong):**

- The **entire plugin (96 source files) compiles cleanly against the real IntelliJ
  IDEA Community 2024.3 platform jars** — zero errors; the only `-Xlint:all`
  findings were benign missing-`serialVersionUID` warnings, deliberately left
  (the platform's own `Language`/`PsiFile` classes omit them too). This caught real
  bugs that inspection missed (e.g. a method colliding with a `SyntaxHighlighterBase`
  method; an access-modifier mismatch on an overridden method).

  > Note on versions: local offline type-checking was originally performed
  > against the 2024.3 platform jars. Since then, the **full Gradle build has
  > been run for real**: the wrapper (Gradle 8.13) bootstrapped, the IntelliJ
  > Platform Gradle Plugin 2.10.4 resolved, the **unified IntelliJ IDEA 2025.3
  > distribution downloaded and resolved via `intellijIdea("2025.3")`**, the
  > plugin compiled against it under Java 21, searchable options were built by
  > an actual headless IDE boot, and `:morphingbird:buildPlugin` produced the
  > distributable `morphingbird-<version>.zip` (with the Gradle-patched
  > `plugin.xml` carrying `<version>` and `<idea-version since-build="243"/>`,
  > verified by inspection). `since-build` stays at `243` so the single
  > artifact installs on 2024.3 through the latest unified releases. The full
  > JetBrains Plugin Verifier (`verifyPlugin`) downloads additional IDE
  > distributions and runs in CI, where the disk for it exists.
- The `LexcLexer` adapter is exercised **through the real IntelliJ `Lexer` interface**
  and produces correct token types with gapless buffer coverage (the contract the
  platform enforces).
- **Every extension point** declared in `plugin.xml` was verified to exist in the
  platform's own extension-point declarations, and the specific non-obvious API calls
  (e.g. `ExecutorAction.getActions(int)`, the `RunLineMarkerContributor.Info`
  constructor, `ResourceRegistrar.addIgnoredResource`) were confirmed by signature.
- The plugin packages into a structurally valid jar (correct `META-INF/plugin.xml`
  layout, classes, icons).

**Not yet runtime-tested (the genuine gap):**

- The plugin has **not** been launched in a running IDE (`runIde`) or run through the
  full JetBrains Plugin Verifier — both need a desktop IDE session. So while every
  *type signature and extension-point name* is verified against the real 2024.3 API,
  the **runtime behaviour** of the higher-level features (navigation actually jumping,
  annotations actually drawing, the run configuration actually launching a process) is
  asserted by construction, not yet observed in a live IDE. That is the remaining
  validation step a maintainer should perform with `./gradlew runIde`.

**Deliberate scope decisions:**

- No bundled copy of lttoolbox's `dix.dtd` is shipped. A subtly-wrong transcription
  would produce false validation errors on valid dictionaries; instead the schema
  identifiers are registered and resolution falls back to a project-local DTD or the
  file's DOCTYPE. (Vendor a verified DTD later to bind it directly.)
- Bilingual/transfer surfaces (bidix, `.t1x`/`.rtx`/`.lrx`) are out of scope for v1;
  monolingual `.dix`/`.lsx` is in scope.
- The PSI is intentionally flat; cross-file intelligence lives in the index. A
  `StubIndex` is the documented next step for many-thousand-file scale.

---

## License

Licensed under the [MIT License](LICENSE). 

The plugin invokes the (GPL-licensed) Apertium/HFST/CG-3 toolchain strictly as external processes found on your PATH — nothing from those projects is bundled or linked.
