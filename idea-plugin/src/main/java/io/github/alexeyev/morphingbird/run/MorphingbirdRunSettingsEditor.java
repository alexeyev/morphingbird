package io.github.alexeyev.morphingbird.run;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * The run-configuration UI: a data-directory chooser (which also populates the
 * mode picker from that directory's {@code modes.xml}), a mode combo box, an
 * input text area, and an optional explicit toolchain path. The mode picker is
 * the key affordance the plan calls for — driven by the real modes.xml.
 */
public final class MorphingbirdRunSettingsEditor
        extends SettingsEditor<MorphingbirdRunConfiguration> {

    private final JPanel panel;
    private final TextFieldWithBrowseButton dataDir = new TextFieldWithBrowseButton();
    private final JComboBox<String> mode = new JComboBox<>();
    private final JBTextArea input = new JBTextArea(5, 40);
    private final TextFieldWithBrowseButton binary = new TextFieldWithBrowseButton();

    public MorphingbirdRunSettingsEditor(Project project) {
        dataDir.addBrowseFolderListener(project,
                FileChooserDescriptorFactory.createSingleFolderDescriptor()
                        .withTitle("Apertium Data Directory")
                        .withDescription("Folder containing modes.xml and compiled artifacts"));
        // When the data dir changes, refresh the mode list from its modes.xml.
        dataDir.getTextField().getDocument().addDocumentListener(
                new com.intellij.ui.DocumentAdapter() {
                    @Override
                    protected void textChanged(@NotNull javax.swing.event.DocumentEvent e) {
                        refreshModes();
                    }
                });
        binary.addBrowseFolderListener(project,
                FileChooserDescriptorFactory.createSingleFileDescriptor()
                        .withTitle("Apertium Binary")
                        .withDescription("Leave empty to auto-detect on PATH"));

        input.setLineWrap(true);
        input.setWrapStyleWord(true);

        panel = FormBuilder.createFormBuilder()
                .addLabeledComponent("Language data directory:", dataDir)
                .addLabeledComponent("Mode:", mode)
                .addLabeledComponent("Input text:", new com.intellij.ui.components.JBScrollPane(input))
                .addLabeledComponent("Apertium binary (optional):", binary)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    private void refreshModes() {
        String dir = dataDir.getText();
        Object current = mode.getSelectedItem();
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        if (dir != null && !dir.isBlank()) {
            VirtualFile vf = LocalFileSystem.getInstance()
                    .findFileByPath(dir + "/modes.xml");
            if (vf != null) {
                try {
                    String xml = new String(vf.contentsToByteArray(),
                            StandardCharsets.UTF_8);
                    List<ModesXml.Mode> modes = ModesXml.parse(xml);
                    for (ModesXml.Mode mm : modes) model.addElement(mm.name);
                } catch (Exception ignored) {
                }
            }
        }
        mode.setModel(model);
        if (current != null) mode.setSelectedItem(current);
    }

    @Override
    protected void resetEditorFrom(@NotNull MorphingbirdRunConfiguration s) {
        MorphingbirdRunOptions o = s.options();
        dataDir.setText(nz(o.getDataDir()));
        refreshModes();
        if (o.getMode() != null && !o.getMode().isBlank()) {
            mode.setSelectedItem(o.getMode());
        }
        input.setText(nz(o.getInput()));
        binary.setText(nz(o.getApertiumBinary()));
    }

    @Override
    protected void applyEditorTo(@NotNull MorphingbirdRunConfiguration s) {
        MorphingbirdRunOptions o = s.options();
        o.setDataDir(dataDir.getText());
        Object m = mode.getSelectedItem();
        o.setMode(m == null ? "" : m.toString());
        o.setInput(input.getText());
        o.setApertiumBinary(binary.getText());
    }

    @Override
    protected @NotNull JComponent createEditor() {
        return panel;
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
