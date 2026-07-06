package io.github.alexeyev.morphingbird.run;

import com.intellij.execution.configurations.RunConfigurationOptions;
import com.intellij.openapi.components.StoredProperty;

/**
 * Persisted options for an {@link MorphingbirdRunConfiguration}: which language-data
 * directory, which mode, the input text, and an optional explicit toolchain
 * binary (empty = auto-detect on PATH and standard locations).
 */
public final class MorphingbirdRunOptions extends RunConfigurationOptions {

    private final StoredProperty<String> dataDir =
            string("").provideDelegate(this, "dataDir");
    private final StoredProperty<String> mode =
            string("").provideDelegate(this, "mode");
    private final StoredProperty<String> input =
            string("").provideDelegate(this, "input");
    private final StoredProperty<String> apertiumBinary =
            string("").provideDelegate(this, "apertiumBinary");

    public String getDataDir() { return dataDir.getValue(this); }
    public void setDataDir(String v) { dataDir.setValue(this, v); }

    public String getMode() { return mode.getValue(this); }
    public void setMode(String v) { mode.setValue(this, v); }

    public String getInput() { return input.getValue(this); }
    public void setInput(String v) { input.setValue(this, v); }

    public String getApertiumBinary() { return apertiumBinary.getValue(this); }
    public void setApertiumBinary(String v) { apertiumBinary.setValue(this, v); }
}
