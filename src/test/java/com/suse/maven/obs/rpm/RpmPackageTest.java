package com.suse.maven.obs.rpm;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.List;
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

        List<String> fileContents = new ArrayList<String>();
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
            assertEquals(1, fileContents.size());
            assertEquals(IOUtils.toString(pomResource, StandardCharsets.UTF_8), fileContents.get(0));
        }
    }

    @Test 
    void throwsExceptionOnSourcePackage() {
        RpmPackage sourcPackage = new RpmPackage(sourcePackagePath);

        IOException ex = assertThrows(
            IOException.class,
            () -> sourcPackage.forEach((file, inputStream) -> fail("Implementation should reject source packages"))
        );

        assertEquals("Invalid RPM file: Source RPMS are not supported", ex.getMessage());
    }
}
