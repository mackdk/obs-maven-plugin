package com.suse.maven.obs.rpm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.suse.maven.obs.TestUtils;
import com.suse.maven.obs.model.ObsArtifact;

class RpmArtifactExtractorTest {

    private RpmArtifactExtractor rpmExtractor;

    private Path targetPath;

    @BeforeEach
    void setup() throws IOException {
        rpmExtractor = new RpmArtifactExtractor();
        targetPath = Files.createTempDirectory("RpmArtifactExtractorTest-");
    }

    @AfterEach
    void tearDown() throws IOException {
        try (Stream<Path> temporaryFiles = Files.walk(targetPath)) {
            temporaryFiles.sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }

    @Test
    void canExtractArtifactFromStandardRPM() throws IOException {
        Path packagePath = TestUtils.getResourcePath("simplexml.rpm");
        ObsArtifact artifact = TestUtils.createObsArtifact("org.simpleframework", "simplexml", "2.7.1");

        List<Path> extractedFiles = rpmExtractor.extract(packagePath, targetPath, artifact);

        assertEquals(2, extractedFiles.size());
        assertEquals(Arrays.asList(
            targetPath.resolve("simplexml.jar"),
            targetPath.resolve("simplexml.pom")
        ), extractedFiles);

        assertTrue(Files.exists(targetPath.resolve("simplexml.jar")));
        assertTrue(Files.exists(targetPath.resolve("simplexml.pom")));
    }

    @Test
    void canExtractArtifactWhenJarNameIsDifferent() throws IOException {
        Path packagePath = TestUtils.getResourcePath("byte-buddy.rpm");
        ObsArtifact artifact = TestUtils.createObsArtifact("byte-buddy", "byte-buddy-dep\\.(jar|pom)", "net.bytebuddy", "byte-buddy-dep", "1.14.16");

        List<Path> extractedFiles = rpmExtractor.extract(packagePath, targetPath, artifact);

        assertEquals(2, extractedFiles.size());
        assertEquals(Arrays.asList(
            targetPath.resolve("byte-buddy-dep.jar"),
            targetPath.resolve("byte-buddy-dep.pom")
        ), extractedFiles);

        assertTrue(Files.exists(targetPath.resolve("byte-buddy-dep.jar")));
        assertTrue(Files.exists(targetPath.resolve("byte-buddy-dep.pom")));
    }
}
