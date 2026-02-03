/*
 * Copyright (c) 2026 SUSE LLC
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 */
package com.suse.maven.obs.common;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.ElementSelectors;

import com.suse.maven.obs.TestUtils;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

class SafeXmlTest {

    private Path maliciousXml;

    @BeforeEach
    void setup() {
        // This malicious xml file tries to read /etc/os-release
        maliciousXml = TestUtils.getResourcePath("xxe-attack.xml");
    }

    @Test
    void documentBuilderBlocksXXE() throws Exception {
        DocumentBuilder builder = SafeXml.newDocumentBuilder();

        try (InputStream inputStream = Files.newInputStream(maliciousXml)) {
            assertThrows(SAXParseException.class, () -> builder.parse(inputStream));
        }
    }

    @Test
    void streamReaderBlocksXXE() throws Exception {
        try (InputStream inputStream = Files.newInputStream(maliciousXml)) {
            XMLStreamReader reader = SafeXml.newStreamReader(inputStream);

            // While we stream the file we should get an exception when the external entity is processed
            assertThrows(XMLStreamException.class, () -> {
                while (reader.hasNext()) {
                    reader.next();
                }
            });
        }
    }

    @Test
    void transformerRejectsXXE() throws Exception {
        try (Reader reader = Files.newBufferedReader(maliciousXml, StandardCharsets.UTF_8)) {
            Transformer transformer = SafeXml.newTransformer();

            Source source = new StreamSource(reader);
            Result result = new StreamResult(new StringWriter());

            assertThrows(TransformerException.class, () -> transformer.transform(source, result));
        }
    }
    
    @Test
    void securitySettingsShouldNotBreakXmlValidProcessing() throws Exception {
        Path safeData = TestUtils.getResourcePath("safe-data.xml");
        String safeXmlData;
        try (Reader reader = Files.newBufferedReader(safeData, StandardCharsets.UTF_8)) {
            safeXmlData = IOUtils.toString(reader);
        }

        // Test DocumentBuilder
        try (Reader reader = new StringReader(safeXmlData)) {
            DocumentBuilder builder = SafeXml.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(reader));
            assertEquals("commons-io", doc.getElementsByTagName("name").item(0).getTextContent());
        }

        // Test StreamReader
        try (InputStream inputStream = new ByteArrayInputStream(safeXmlData.getBytes(StandardCharsets.UTF_8)) ) {
            XMLStreamReader reader = SafeXml.newStreamReader(inputStream);
            boolean foundName = false;
            while(reader.hasNext()) {
                if (reader.next() == XMLStreamConstants.START_ELEMENT && "name".equals(reader.getLocalName())) {
                    assertEquals("commons-io", reader.getElementText());
                    foundName = true;
                }
            }
            assertTrue(foundName);
        }

        // Test Transformer
        Transformer transformer = SafeXml.newTransformer();
        assertNotNull(transformer);
        
        Source source = new StreamSource(new StringReader(safeXmlData));
        StreamResult result = new StreamResult(new StringWriter());
        assertDoesNotThrow(() -> transformer.transform(source, result));

        String actualXml = result.getWriter().toString();
        Diff diff = DiffBuilder.compare(safeXmlData)
            .withTest(actualXml)
            .ignoreWhitespace()
            .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndAllAttributes))
            .checkForIdentical()
            .build();

        assertFalse(diff.hasDifferences(), "Transformed xml is not correct: " + diff.fullDescription()
            + "\nExpected XML:\n" + safeXmlData
            + "\nActual XML:\n" + actualXml);
    }
}
