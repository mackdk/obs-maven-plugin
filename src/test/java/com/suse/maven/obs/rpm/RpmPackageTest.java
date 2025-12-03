/*
 * Copyright (c) 2025 SUSE LLC
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 */
package com.suse.maven.obs.rpm;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.suse.maven.obs.TestUtils;

class RpmPackageTest {

    private Path testPackagePath;

    private Path sourcePackagePath;

    @BeforeEach
    void setup() {
        testPackagePath = TestUtils.getResourcePath("simplexml.rpm");
        sourcePackagePath = TestUtils.getResourcePath("simplexml.src.rpm");
    }

    @Test
    void canListCorrectFilesWithinRPM() throws Exception {
        RpmPackage rpmPackage = new RpmPackage(testPackagePath);

        List<String> extracted = rpmPackage.list().stream()
            .map(file -> file.getPath().toString())
            .collect(Collectors.toList());

        assertEquals(Arrays.asList(
            "/usr/share/doc/packages/simplexml",
            "/usr/share/doc/packages/simplexml/README.md",
            "/usr/share/java/simpleframework",
            "/usr/share/java/simpleframework/simplexml.jar",
            "/usr/share/licenses/simplexml",
            "/usr/share/licenses/simplexml/LICENSE.txt",
            "/usr/share/maven-metadata/simplexml.xml",
            "/usr/share/maven-poms/simpleframework",
            "/usr/share/maven-poms/simpleframework/simplexml.pom"
        ), extracted);
    }

    @Test
    void canListCorrectFilesFiltered() throws Exception {
        RpmPackage rpmPackage = new RpmPackage(testPackagePath);

        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:**/simpleframework/*.*");
        List<String> extracted = rpmPackage.filter(file -> file.isRegularFile() && pathMatcher.matches(file.getPath()))
            .list().stream()
            .map(file -> file.getPath().toString())
            .collect(Collectors.toList());

        assertEquals(Arrays.asList(
            "/usr/share/java/simpleframework/simplexml.jar",
            "/usr/share/maven-poms/simpleframework/simplexml.pom"
        ), extracted);
    }

    @Test
    void canExtractFileFromRPM() throws IOException {
        RpmPackage rpmPackage = new RpmPackage(testPackagePath);

        List<String> fileContents = new ArrayList<>();
        rpmPackage.filter(file -> file.isRegularFile() && file.getPath().endsWith("simplexml.pom"))
            .forEach((file, inputStream) -> {
                try {
                    fileContents.add(IOUtils.toString(inputStream, StandardCharsets.UTF_8));
                } catch (IOException ex) {
                    fail("Unable to read input stream for file " + file, ex);
                }
            });

        // Check that only one file was extracted and it has the expected content
        try (InputStream pomResource = RpmPackageTest.class.getResourceAsStream("simplexml.pom")) {
            assertNotNull(pomResource);
            assertEquals(1, fileContents.size());
            assertEquals(IOUtils.toString(pomResource, StandardCharsets.UTF_8), fileContents.get(0));
        }
    }

    @Test
    void throwsExceptionOnSourcePackage() {
        RpmPackage sourcePackage = new RpmPackage(sourcePackagePath);

        IOException ex = assertThrows(
            IOException.class,
            () -> sourcePackage.forEach((file, inputStream) -> fail("Implementation should reject source packages"))
        );

        assertEquals("Invalid RPM file: Source RPMs are not supported", ex.getMessage());
    }

    @Test
    void canReadPartialData() throws IOException {
        RpmPackage rpmPackage = new RpmPackage(testPackagePath);

        Map<String, byte[]> fileContents = new HashMap<>();
        rpmPackage.filter(RpmFile::isRegularFile)
            .forEach((file, inputStream) -> {
                byte[] first5Bytes = new byte[5];
                assertEquals(5, inputStream.read(first5Bytes, 0, 5));
                fileContents.put(file.getPath().getFileName().toString(), first5Bytes);
            });

        assertEquals(5, fileContents.size());

        assertArrayEquals(TestUtils.fromHexString("20 20 20 20 20"), fileContents.get("LICENSE.txt"));
        assertArrayEquals(TestUtils.fromHexString("23 20 53 69 6D"), fileContents.get("README.md"));
        assertArrayEquals(TestUtils.fromHexString("50 4B 03 04 14"), fileContents.get("simplexml.jar"));
        assertArrayEquals(TestUtils.fromHexString("3C 70 72 6F 6A"), fileContents.get("simplexml.pom"));
        assertArrayEquals(TestUtils.fromHexString("3C 3F 78 6D 6C"), fileContents.get("simplexml.xml"));
    }
}
