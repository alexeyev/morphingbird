package io.github.alexeyev.morphingbird.run;

import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Makes compiler/processor diagnostics in the Run console clickable. Recognises
 * the common {@code file:line:col} and {@code file:line} forms emitted by
 * {@code hfst-lexc}, {@code hfst-twolc}, {@code lt-comp} and {@code cg-comp},
 * turning them into links that open the file at the right position — the same
 * "navigable diagnostics" idea Refalcon implements for {@code rlc}.
 */
public final class MorphingbirdConsoleFilter implements Filter {

    // path : line ( : col )?   — path is non-greedy up to the first ':line'
    private static final Pattern LOC = Pattern.compile(
            "([^\\s:]+\\.(?:lexc|lexd|twol|twoc|rlx|dix|lsx|metadix|udx|spellrelax)):(\\d+)(?::(\\d+))?");

    private final Project project;

    public MorphingbirdConsoleFilter(Project project) {
        this.project = project;
    }

    @Override
    public @Nullable Result applyFilter(@NotNull String line, int entireLength) {
        Matcher m = LOC.matcher(line);
        if (!m.find()) return null;

        String path = m.group(1);
        int lineNo = parseInt(m.group(2), 1) - 1;        // 0-based for descriptor
        int colNo = parseInt(m.group(3), 1) - 1;

        VirtualFile vf = resolve(path);
        if (vf == null) return null;

        int lineStartInConsole = entireLength - line.length();
        int linkStart = lineStartInConsole + m.start(1);
        int linkEnd = lineStartInConsole + m.end(2);
        if (m.group(3) != null) linkEnd = lineStartInConsole + m.end(3);

        OpenFileHyperlinkInfo link =
                new OpenFileHyperlinkInfo(project, vf, Math.max(0, lineNo), Math.max(0, colNo));
        return new Result(linkStart, linkEnd, link);
    }

    private VirtualFile resolve(String path) {
        File f = new File(path);
        if (f.isAbsolute()) {
            return LocalFileSystem.getInstance().findFileByPath(path);
        }
        // Relative: try the project base dir.
        String base = project.getBasePath();
        if (base != null) {
            VirtualFile vf = LocalFileSystem.getInstance()
                    .findFileByPath(base + "/" + path);
            if (vf != null) return vf;
        }
        return LocalFileSystem.getInstance().findFileByPath(path);
    }

    private static int parseInt(String s, int dflt) {
        if (s == null) return dflt;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return dflt; }
    }
}
