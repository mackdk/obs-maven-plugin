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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.suse.maven.obs.TestUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Arrays;
import java.util.List;

class RpmArtifactExtractorTest {

    private RpmArtifactExtractor rpmExtractor;

    @TempDir
    private Path targetPath;

    @BeforeEach
    void setup() throws IOException {
        rpmExtractor = new RpmArtifactExtractor();
    }

    @Test
    void canExtractArtifactWithGlob() throws IOException {
        Path packagePath = TestUtils.getResourcePath("simplexml.rpm");

        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:simplexml.{pom,jar}");
        List<Path> extractedFiles = rpmExtractor.extract(packagePath, targetPath, matcher);

        assertEquals(2, extractedFiles.size());
        assertEquals(Arrays.asList(
            targetPath.resolve("simplexml.jar"),
            targetPath.resolve("simplexml.pom")
        ), extractedFiles);

        assertFileExists(targetPath.resolve("simplexml.jar"));
        assertFileExists(targetPath.resolve("simplexml.pom"));
    }

    @Test
    void canExtractArtifactWithRegex() throws IOException {
        Path packagePath = TestUtils.getResourcePath("byte-buddy.rpm");

        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("regex:byte-buddy-dep\\.(jar|pom)");
        List<Path> extractedFiles = rpmExtractor.extract(packagePath, targetPath, matcher);

        assertEquals(2, extractedFiles.size());
        assertEquals(Arrays.asList(
            targetPath.resolve("byte-buddy-dep.jar"),
            targetPath.resolve("byte-buddy-dep.pom")
        ), extractedFiles);

        assertFileExists(targetPath.resolve("byte-buddy-dep.jar"));
        assertFileExists(targetPath.resolve("byte-buddy-dep.pom"));
    }

    private static void assertFileExists(Path path) {
        assertTrue(Files.isRegularFile(path));
        assertTrue(Files.exists(path));
    }
}
