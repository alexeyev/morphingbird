package io.github.alexeyev.morphingbird.run;

import com.intellij.openapi.diagnostic.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal reader for apertium {@code modes.xml}, extracting the declared mode
 * names (and whether each is installed) so the run configuration can present a
 * mode picker. modes.xml is small and schema-simple, so a DOM parse is fine.
 */
public final class ModesXml {

    private static final Logger LOG = Logger.getInstance(ModesXml.class);

    /** A single {@code <mode>} entry. */
    public static final class Mode {
        public final String name;
        public final boolean install;
        public Mode(String name, boolean install) {
            this.name = name; this.install = install;
        }
        @Override public String toString() { return name; }
    }

    /** Parses modes.xml content; returns the modes (possibly empty, never null). */
    public static List<Mode> parse(String xml) {
        List<Mode> modes = new ArrayList<>();
        if (xml == null || xml.isBlank()) return modes;
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(false);
            // Be defensive about external entities.
            f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            DocumentBuilder b = f.newDocumentBuilder();
            Document doc = b.parse(new ByteArrayInputStream(
                    xml.getBytes(StandardCharsets.UTF_8)));
            NodeList nodes = doc.getElementsByTagName("mode");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element e = (Element) nodes.item(i);
                String name = e.getAttribute("name");
                if (name == null || name.isEmpty()) continue;
                boolean install = "yes".equalsIgnoreCase(e.getAttribute("install"));
                modes.add(new Mode(name, install));
            }
        } catch (Exception ex) {
            LOG.warn("Failed to parse modes.xml", ex);
        }
        return modes;
    }
}
