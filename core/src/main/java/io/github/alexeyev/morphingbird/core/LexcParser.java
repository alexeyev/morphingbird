package io.github.alexeyev.morphingbird.core;

import java.util.List;

/**
 * A lenient lexc parser: it turns a token stream into a {@link LexcModel}
 * without ever failing on malformed input (per the plan's "never red" rule —
 * structure is always produced; diagnostics are a separate concern).
 *
 * <p>Grammar, informally:</p>
 * <pre>
 *   file        := (multicharBlock | lexiconBlock | junk)*
 *   multicharBlock := 'Multichar_Symbols' symbolDecl*
 *   lexiconBlock   := 'LEXICON' IDENT entry*
 *   entry          := form (':' form)? IDENT? ';'
 * </pre>
 * where {@code form} is a run of WORD/IDENTIFIER/TAG/ARCHIPHONEME tokens.
 */
public final class LexcParser {

    private final List<LexcToken> toks;
    private int i;

    private LexcParser(List<LexcToken> toks) {
        this.toks = toks;
        this.i = 0;
    }

    public static LexcModel parse(String source) {
        return new LexcParser(LexcScanner.tokenize(source)).parseFile();
    }

    public static LexcModel parseTokens(List<LexcToken> toks) {
        return new LexcParser(toks).parseFile();
    }

    private LexcToken peek() {
        return toks.get(Math.min(i, toks.size() - 1));
    }

    private LexcToken advance() {
        LexcToken t = peek();
        if (i < toks.size() - 1) i++;
        return t;
    }

    private boolean atEof() {
        return peek().kind == LexcToken.Kind.EOF;
    }

    /** End offset of the most recently consumed token (0 if none). */
    private int prevEnd() {
        int idx = i - 1;
        if (idx < 0) idx = 0;
        if (idx >= toks.size()) idx = toks.size() - 1;
        return toks.get(idx).end;
    }

    private void skipTrivia() {
        while (!atEof()) {
            LexcToken.Kind k = peek().kind;
            if (k == LexcToken.Kind.WHITESPACE || k == LexcToken.Kind.COMMENT) {
                advance();
            } else {
                break;
            }
        }
    }

    private LexcModel parseFile() {
        LexcModel m = new LexcModel();
        while (!atEof()) {
            skipTrivia();
            if (atEof()) break;
            LexcToken t = peek();
            switch (t.kind) {
                case KW_MULTICHAR:
                    advance();
                    parseMulticharBlock(m);
                    break;
                case KW_LEXICON:
                    advance();
                    parseLexiconBlock(m);
                    break;
                default:
                    advance();   // skip anything we don't recognise (lenient)
            }
        }
        return m;
    }

    /** After the {@code Multichar_Symbols} keyword: collect TAG/ARCHIPHONEME decls. */
    private void parseMulticharBlock(LexcModel m) {
        // Default insertion point = just after the Multichar_Symbols keyword
        // (the keyword token was just consumed by the caller).
        if (m.multicharInsertOffset < 0) {
            m.multicharInsertOffset = prevEnd();
        }
        while (!atEof()) {
            skipTrivia();
            LexcToken t = peek();
            if (t.kind == LexcToken.Kind.KW_LEXICON
                    || t.kind == LexcToken.Kind.KW_MULTICHAR
                    || t.kind == LexcToken.Kind.KW_DEFINITIONS) {
                return;  // next section
            }
            if (t.kind == LexcToken.Kind.TAG) {
                m.declaredSymbols.add(new LexcModel.SymbolDecl(
                        t.canonical, true, t.start, t.end));
                m.multicharInsertOffset = t.end;   // insert after last decl
                advance();
            } else if (t.kind == LexcToken.Kind.ARCHIPHONEME) {
                m.declaredSymbols.add(new LexcModel.SymbolDecl(
                        t.canonical, false, t.start, t.end));
                m.multicharInsertOffset = t.end;
                advance();
            } else if (t.kind == LexcToken.Kind.EOF) {
                return;
            } else {
                advance();  // other punctuation in the block, skip
            }
        }
    }

    /** After the {@code LEXICON} keyword: read the name, then entries. */
    private void parseLexiconBlock(LexcModel m) {
        skipTrivia();
        LexcToken nameTok = peek();
        if (nameTok.kind != LexcToken.Kind.IDENTIFIER) {
            // Malformed header; bail to file level.
            return;
        }
        advance();
        LexcModel.Lexicon lex = new LexcModel.Lexicon(
                nameTok.text, nameTok.start, nameTok.end);
        m.lexicons.add(lex);

        // Entries until the next section keyword or EOF.
        while (!atEof()) {
            skipTrivia();
            LexcToken t = peek();
            if (t.kind == LexcToken.Kind.KW_LEXICON
                    || t.kind == LexcToken.Kind.KW_MULTICHAR
                    || t.kind == LexcToken.Kind.KW_DEFINITIONS
                    || t.kind == LexcToken.Kind.EOF) {
                return;
            }
            parseEntry(m, lex);
        }
    }

    /**
     * Parses one entry up to and including its {@code ;}. Collects the
     * continuation class (last IDENTIFIER before {@code ;}) and any
     * tags/archiphonemes seen.
     */
    private void parseEntry(LexcModel m, LexcModel.Lexicon lex) {
        LexcModel.Entry e = new LexcModel.Entry();
        int start = peek().start;
        e.lineStart = start;

        LexcToken lastIdent = null;     // candidate continuation class
        boolean sawColon = false;

        while (!atEof()) {
            LexcToken t = peek();
            if (t.kind == LexcToken.Kind.SEMICOLON) {
                e.lineEnd = t.end;
                advance();
                break;
            }
            if (t.kind == LexcToken.Kind.WHITESPACE
                    || t.kind == LexcToken.Kind.COMMENT
                    || t.kind == LexcToken.Kind.STRING) {
                advance();
                continue;
            }
            // A section keyword without a ';' — malformed entry; stop before it.
            if (t.kind == LexcToken.Kind.KW_LEXICON
                    || t.kind == LexcToken.Kind.KW_MULTICHAR
                    || t.kind == LexcToken.Kind.KW_DEFINITIONS) {
                e.lineEnd = t.start;
                break;
            }
            switch (t.kind) {
                case COLON:
                    sawColon = true;
                    lastIdent = null;   // reset: continuation is after the lower side
                    break;
                case IDENTIFIER:
                    // '#' is lexc's special end-of-word continuation, not a
                    // user LEXICON; record it as a continuation but mark it as
                    // the built-in terminator so resolution never flags it.
                    lastIdent = t;
                    break;
                case TAG:
                    e.tags.add(new LexcModel.SymbolRef(t.canonical, t.start, t.end));
                    break;
                case ARCHIPHONEME:
                    e.archiphonemes.add(
                            new LexcModel.SymbolRef(t.canonical, t.start, t.end));
                    break;
                default:
                    // WORD/OTHER: part of the form, ignore for symbol graph
            }
            advance();
        }

        // The continuation class is the last bare IDENTIFIER before ';'.
        if (lastIdent != null) {
            e.continuation = lastIdent.text;
            e.continuationStart = lastIdent.start;
            e.continuationEnd = lastIdent.end;
            m.continuationRefs.add(new LexcModel.SymbolRef(
                    lastIdent.text, lastIdent.start, lastIdent.end));
        }
        if (e.lineEnd == 0) e.lineEnd = peek().start;
        lex.entries.add(e);
    }
}
