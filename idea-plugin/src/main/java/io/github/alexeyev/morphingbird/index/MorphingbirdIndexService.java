package io.github.alexeyev.morphingbird.index;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import io.github.alexeyev.morphingbird.core.Cg3Model;
import io.github.alexeyev.morphingbird.core.DixModel;
import io.github.alexeyev.morphingbird.core.LexcParser;
import io.github.alexeyev.morphingbird.core.SymbolIndex;
import io.github.alexeyev.morphingbird.core.TwolModel;
import io.github.alexeyev.morphingbird.core.LexdModel;
import io.github.alexeyev.morphingbird.core.UdxModel;

import java.nio.charset.StandardCharsets;

/**
 * A project-level service that builds and caches the cross-file
 * {@link SymbolIndex} for all apertium source files in the project.
 *
 * <p>Following the plan's deliberate v1 decision (and Refalcon's measured
 * approach): no stub index yet — the whole project's apertium files are walked
 * once and the result cached, invalidated on any PSI change via
 * {@link PsiModificationTracker}. At the scale of a single language repo this is
 * effectively instant (the core builds the kir graph in ~250 ms); a
 * {@code StubIndex} is the documented next step for many-thousand-file scale.</p>
 */
@Service(Service.Level.PROJECT)
public final class MorphingbirdIndexService {

    private static final Logger LOG = Logger.getInstance(MorphingbirdIndexService.class);

    private final Project project;
    private final CachedValue<SymbolIndex> cache;

    public MorphingbirdIndexService(Project project) {
        this.project = project;
        this.cache = CachedValuesManager.getManager(project).createCachedValue(
                new CachedValueProvider<>() {
                    @Override
                    public Result<SymbolIndex> compute() {
                        SymbolIndex idx = buildIndex();
                        // Rebuild whenever any PSI changes anywhere in the project.
                        return Result.create(idx,
                                PsiModificationTracker.MODIFICATION_COUNT);
                    }
                }, false);
    }

    public static MorphingbirdIndexService getInstance(Project project) {
        return project.getService(MorphingbirdIndexService.class);
    }

    /** The current (cached) cross-file symbol index. */
    public SymbolIndex getIndex() {
        return cache.getValue();
    }

    private SymbolIndex buildIndex() {
        SymbolIndex idx = new SymbolIndex();
        ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);

        fileIndex.iterateContent(vf -> {
            if (vf.isDirectory()) return true;
            try {
                String name = vf.getName();
                String id = vf.getPath();
                if (name.endsWith(".lexc")) {
                    idx.addLexc(id, LexcParser.parse(text(vf)));
                } else if (name.endsWith(".twol") || name.endsWith(".twoc")) {
                    idx.addTwol(id, TwolModel.parse(text(vf)));
                } else if (name.endsWith(".rlx")) {
                    idx.addCg3(id, Cg3Model.parse(text(vf)));
                } else if (name.endsWith(".dix") || name.endsWith(".lsx")
                        || name.endsWith(".metadix")) {
                    idx.addDix(id, DixModel.parse(text(vf)));
                } else if (name.endsWith(".lexd")) {
                    idx.addLexd(id, LexdModel.parse(text(vf)));
                } else if (name.endsWith(".udx")) {
                    idx.addUdx(id, UdxModel.parse(text(vf)));
                }
            } catch (Exception e) {
                LOG.warn("Failed to index " + vf.getPath(), e);
            }
            return true;
        });
        return idx;
    }

    private static String text(VirtualFile vf) {
        try {
            return new String(vf.contentsToByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }
}
