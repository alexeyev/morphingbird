package io.github.alexeyev.morphingbird.common;

import com.intellij.openapi.util.IconLoader;

import javax.swing.Icon;

/**
 * Morphingbird plugin icons. Each file type has its own glyph (lexc = morphotactic
 * graph, twol = two-level tapes, CG3 = disambiguation, dix = XML dictionary,
 * udx = tag→UD mapping). Light/dark variants are resolved automatically by the
 * platform from the {@code _dark} suffix.
 */
public final class MorphingbirdIcons {
    private MorphingbirdIcons() {}

    /** The Morphingbird brand mark (settings, run config type). */
    public static final Icon MORPHINGBIRD =
            IconLoader.getIcon("/icons/morphingbird.svg", MorphingbirdIcons.class);

    /** .lexc — morphotactic continuation graph. */
    public static final Icon LEXC =
            IconLoader.getIcon("/icons/morphingbird-lexc.svg", MorphingbirdIcons.class);
    /** .twol — two-level rules. */
    public static final Icon TWOL =
            IconLoader.getIcon("/icons/morphingbird-twol.svg", MorphingbirdIcons.class);
    /** .rlx — CG3 disambiguation. */
    public static final Icon CG3 =
            IconLoader.getIcon("/icons/morphingbird-cg3.svg", MorphingbirdIcons.class);
    /** .dix/.lsx — lttoolbox XML dictionary. */
    public static final Icon DIX =
            IconLoader.getIcon("/icons/morphingbird-dix.svg", MorphingbirdIcons.class);
    /** .udx — Apertium→UD tag mapping. */
    public static final Icon UDX =
            IconLoader.getIcon("/icons/morphingbird-udx.svg", MorphingbirdIcons.class);
    /** A run mode (modes.xml). */
    public static final Icon MODE =
            IconLoader.getIcon("/icons/morphingbird-mode.svg", MorphingbirdIcons.class);

    // --- semantic aliases used by structure views / completion ---
    /** Generic file glyph (uses the brand mark). */
    public static final Icon FILE = MORPHINGBIRD;
    /** A LEXICON node (structure view, completion). */
    public static final Icon LEXICON = LEXC;
    /** A tag symbol (completion, structure). */
    public static final Icon TAG =
            IconLoader.getIcon("/icons/morphingbird-udx.svg", MorphingbirdIcons.class);
    /** A rule / set (twol rules, CG3 lists). */
    public static final Icon RULE = TWOL;
}
