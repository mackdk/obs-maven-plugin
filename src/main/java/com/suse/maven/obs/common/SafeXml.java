package com.suse.maven.obs.common;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;

import org.jetbrains.annotations.NotNull;

/**
 * A utility class for creating secure XML parsers and transformers.
 * <p>
 * This class ensures that all XML processors are configured to prevent
 * <b>XXE (XML External Entity)</b> attacks and other common XML security vulnerabilities.
 * It strictly disables DTDs (Document Type Definitions), external entity resolution,
 * and external stylesheet access.
 */
public class SafeXml {

    private SafeXml() {
        // Prevent instantiation
    }

    /**
     * Creates a new, secure {@link DocumentBuilder} with namespace awareness enabled.
     * @return a configured, secure {@link DocumentBuilder}.
     * @throws ParserConfigurationException if the security features cannot be applied.
     * @see #newDocumentBuilder(boolean)
     */
    public static DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        return newDocumentBuilder(true);
    }

    /**
     * Creates a new, secure {@link DocumentBuilder}.
     * <p>
     * This builder is configured to:
     * <ul>
     * <li>Disallow DOCTYPE declarations entirely.</li>
     * <li>Disable external general and parameter entities.</li>
     * <li>Disable loading of external DTDs.</li>
     * <li>Disable XInclude processing.</li>
     * <li>Disable entity reference expansion.</li>
     * </ul>
     * This configuration effectively blocks XXE attacks.
     * @param namespaceAware {@code true} for supportting XML namespaces, {@code false} otherwise.
     * @return a configured, secure DocumentBuilder.
     * @throws ParserConfigurationException if the security features cannot be applied.
     */
    public static DocumentBuilder newDocumentBuilder(boolean namespaceAware) throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        // Disable DOCTYPE declarations and external references
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        factory.setNamespaceAware(namespaceAware);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        return factory.newDocumentBuilder();
    }

    /**
     * Creates a new, secure {@link Transformer} with default settings (indentation, UTF-8).
     * @return a configured, secure Transformer.
     * @throws TransformerConfigurationException if the security features cannot be applied.
     * @see #newTransformer(boolean, Charset)
     */
    public static Transformer newTransformer() throws TransformerConfigurationException {
        return newTransformer(true, StandardCharsets.UTF_8);
    }

    /**
     * Creates a new, secure {@link Transformer} with custom output properties.
     * <p>
     * This transformer is configured to prevent the loading of external DTDs and Stylesheets
     * during transformation, securing it against SSRF (Server Side Request Forgery) attacks via XSLT.
     * @param indent {@code true} to enable pretty-printing, {@code false} for compact output.
     * @param charset the character set to use for the output encoding (e.g., UTF-8).
     * @return a configured, secure Transformer.
     * @throws TransformerConfigurationException if the security features cannot be applied.
     */
    public static Transformer newTransformer(boolean indent, Charset charset) throws TransformerConfigurationException {
        TransformerFactory factory = TransformerFactory.newInstance();

        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

        Transformer transformer = factory.newTransformer();

        transformer.setOutputProperty(OutputKeys.INDENT, indent ? "yes" : "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, charset.name());

        return transformer;
    }

    /**
     * Creates a new, secure StAX {@link XMLStreamReader} for efficient stream-based parsing.
     * <p>
     * This reader is configured to strictly disable DTD support and external entity resolution.
     * @param inputStream the input stream to read the XML from. Must not be null.
     * @return a configured, secure XMLStreamReader.
     * @throws XMLStreamException if the reader cannot be created.
     */
    public static XMLStreamReader newStreamReader(@NotNull InputStream inputStream) throws XMLStreamException {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();

        // Disable DTD support and external entities
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

        return xmlInputFactory.createXMLStreamReader(inputStream);
    }
}
