package io.github.alexeyev.morphingbird.lttoolbox;

import com.intellij.javaee.ResourceRegistrar;
import com.intellij.javaee.StandardResourceProvider;

/**
 * Registers lttoolbox's schema identifiers so {@code .dix}/{@code .lsx} files
 * that reference them resolve without per-project setup.
 *
 * <p>Deliberate decision (honesty over convenience): we do <em>not</em> ship a
 * hand-transcribed copy of {@code dix.dtd}. A subtly-wrong bundled DTD would
 * produce false validation errors on valid dictionaries — exactly the
 * "never red on valid code" failure the plan forbids. Instead we register the
 * canonical schema URLs as known resources; when a project keeps the DTD locally
 * (the usual case, since apertium repos vendor it) or the file's DOCTYPE points
 * at it, XML validation resolves correctly. Absent any DTD, IntelliJ's XML
 * support still provides well-formedness checking, structure, and
 * tag/attribute completion from the document itself.</p>
 *
 * <p>If a verified copy of the official DTD is later vendored into plugin
 * resources, bind it here with the second argument of
 * {@link ResourceRegistrar#addStdResource(String, String, Class)}.</p>
 */
public final class LttoolboxResourceProvider implements StandardResourceProvider {

    /** Canonical locations of lttoolbox's dictionary schemas. */
    public static final String DIX_DTD =
            "https://raw.githubusercontent.com/apertium/lttoolbox/main/lttoolbox/dix.dtd";
    public static final String DIX_RNG =
            "https://raw.githubusercontent.com/apertium/lttoolbox/main/lttoolbox/dix.rng";

    @Override
    public void registerResources(ResourceRegistrar registrar) {
        // Register the identifiers as known so the IDE doesn't flag them as
        // unresolved; resolution falls back to a local/DOCTYPE-referenced copy.
        // (No local mapping is bound until a verified DTD is vendored.)
        registrar.addIgnoredResource(DIX_DTD);
        registrar.addIgnoredResource(DIX_RNG);
    }
}
