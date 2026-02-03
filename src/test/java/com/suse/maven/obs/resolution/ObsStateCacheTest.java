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
package com.suse.maven.obs.resolution;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.maven.model.Dependency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.ElementSelectors;

import com.suse.maven.obs.TestUtils;
import com.suse.maven.obs.common.Checksum;
import com.suse.maven.obs.repository.MetadataEntry;

class ObsStateCacheTest {

    @TempDir
    private Path cacheLocation;

    @Test
    void canInitializeWithoutExistingCache() throws IOException {
        assertDoesNotThrow(() -> new ObsStateCache(cacheLocation));
        assertNothingCached(cacheLocation);
    }

    @Test
    void throwsExceptionIfLocationIsInvalid() {
        Path nonExistingCacheDir = Paths.get("/non/existing/directory");
        assertThrows(IllegalArgumentException.class, () -> new ObsStateCache(nonExistingCacheDir));
    }

    @Test
    void storesAndRetrievesDependencies() throws IOException {
        ObsStateCache stateCache = new ObsStateCache(cacheLocation);

        Dependency firstDep = TestUtils.createMavenDependency("testGroup", "firstArtifact", "1.2.0");
        Checksum firstChecksum = getDummyChecksum();

        assertFalse(stateCache.isDependencyUpToDate(firstDep, firstChecksum));
        stateCache.registerDependency(firstDep, firstChecksum);
        assertTrue(stateCache.isDependencyUpToDate(firstDep, firstChecksum));

        Dependency secondDep = TestUtils.createMavenDependency("testGroup", "secondArtifact", "3.5.2");
        Checksum secondChecksum = getDummyChecksum();

        assertFalse(stateCache.isDependencyUpToDate(secondDep, secondChecksum));
        stateCache.registerDependency(secondDep, secondChecksum);
        assertTrue(stateCache.isDependencyUpToDate(secondDep, secondChecksum));

        Checksum updatedFirstChecksum = getDummyChecksum();
        assertFalse(stateCache.isDependencyUpToDate(firstDep, updatedFirstChecksum));
        stateCache.registerDependency(firstDep, updatedFirstChecksum);
        assertTrue(stateCache.isDependencyUpToDate(firstDep, updatedFirstChecksum));

        assertNothingCached(cacheLocation);
    }

    @Test
    void retrievesAndCacheRepositoryIndexes() throws IOException {
        ObsStateCache stateCache = new ObsStateCache(cacheLocation);

        MetadataEntry metadata = new MetadataEntry("test-type", "primary.xml", getDummyChecksum(), 0L, Instant.now());

        assertNothingCached(cacheLocation);
        Path cached = stateCache.getOrStoreMetadata("test-repo", metadata, () -> toInputStream("ABCD"));

        // Ensure the file was downloaded and cached
        assertNotNull(cached);
        assertEquals(cacheLocation.resolve("test-repo_test-type.xml"), cached);
        assertEquals("ABCD", toString(cached));

        // When the checksum is the same the cached version should be retrieved
        cached = stateCache.getOrStoreMetadata("test-repo", metadata, () -> fail("Data should not be fetched"));

        // Cached file should still be correct
        assertNotNull(cached);
        assertEquals(cacheLocation.resolve("test-repo_test-type.xml"), cached);
        assertEquals("ABCD", toString(cached));

        // Changing the checksum should trigger a new download
        metadata = new MetadataEntry("test-type", "primary.xml", getDummyChecksum(), 0L, Instant.now());
        cached = stateCache.getOrStoreMetadata("test-repo", metadata, () -> toInputStream("DEFG"));
        assertNotNull(cached);
        assertEquals(cacheLocation.resolve("test-repo_test-type.xml"), cached);
        assertEquals("DEFG", toString(cached));
    }

    @Test
    void persistsStateToXml() throws IOException {
        ObsStateCache stateCache = new ObsStateCache(cacheLocation);
        assertNothingCached(cacheLocation);

        Dependency firstDependency = TestUtils.createMavenDependency("testGroup", "firstArtifact", "1.2.0");
        Checksum firstChecksum = new Checksum("sha1", "09a902b5e4b4749d7176c608291d34563cceb09e");
        stateCache.registerDependency(firstDependency, firstChecksum);

        Dependency secondDependency = TestUtils.createMavenDependency("testGroup", "secondArtifact", "3.5.2");
        Checksum secondChecksum = new Checksum("md5", "457de9b3e0a4942b81a46c0b8ce5db7f");
        stateCache.registerDependency(secondDependency, secondChecksum);

        stateCache.getOrStoreMetadata("repo-one", entryOne(), () -> toInputStream("ABCD"));
        stateCache.getOrStoreMetadata("repo-two", entryTwo(), () -> toInputStream("DEFG"));

        stateCache.persist();

        assertTrue(Files.isReadable(cacheLocation.resolve("repo-one_primary.xml")));
        assertTrue(Files.isReadable(cacheLocation.resolve("repo-two_primary.xml")));
        assertTrue(Files.isReadable(cacheLocation.resolve("cache.xml")));

        String expectedXml = toString(TestUtils.getResourcePath("test-cache.xml"));
        String actualXml = toString(cacheLocation.resolve("cache.xml"));

        Diff diff = DiffBuilder.compare(expectedXml)
            .withTest(actualXml)
            .ignoreWhitespace()
            .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndAllAttributes))
            .checkForSimilar()
            .build();

        assertFalse(diff.hasDifferences(), "Cache xml is not correct: " + diff.fullDescription()
            + "\nExpected XML:\n" + expectedXml
            + "\nActual XML:\n" + actualXml);
    }

    @Test
    void restoresCacheFromXml() throws IOException {
        Files.copy(TestUtils.getResourcePath("test-cache.xml"), cacheLocation.resolve("cache.xml"));
        Files.copy(toInputStream("ABCD"), cacheLocation.resolve("repo-one_primary.xml"));
        Files.copy(toInputStream("DEFG"), cacheLocation.resolve("repo-two_primary.xml"));

        ObsStateCache stateCache = new ObsStateCache(cacheLocation);

        // Verify the dependencies are up to date
        Dependency firstDep = TestUtils.createMavenDependency("testGroup", "firstArtifact", "1.2.0");
        Checksum firstChecksum = new Checksum("sha1", "09a902b5e4b4749d7176c608291d34563cceb09e");
        assertTrue(stateCache.isDependencyUpToDate(firstDep, firstChecksum));

        Dependency secondDep = TestUtils.createMavenDependency("testGroup", "secondArtifact", "3.5.2");
        Checksum secondChecksum = new Checksum("md5", "457de9b3e0a4942b81a46c0b8ce5db7f");
        assertTrue(stateCache.isDependencyUpToDate(secondDep, secondChecksum));

        // Ensure index files are not fetched but retrieved from the cache
        Path onePath = stateCache.getOrStoreMetadata("repo-one", entryOne(), () -> fail("Data should not be fetched"));
        Path twoPath = stateCache.getOrStoreMetadata("repo-two", entryTwo(), () -> fail("Data should not be fetched"));

        assertEquals("ABCD", toString(onePath));
        assertEquals("DEFG", toString(twoPath));
    }

    @Test
    void cleansUpTempFilesOnFailure() throws IOException {
        ObsStateCache stateCache = new ObsStateCache(cacheLocation);
        MetadataEntry metadata = entryOne();

        // Simulate a failure during the creation of the data stream
        assertThrows(IOException.class, () -> stateCache.getOrStoreMetadata("repo-fail", metadata, () -> {
            throw new IOException("Network timeout");
        }));

        // Verify that no ".tmp" files were left behind in the cache directory
        try (Stream<Path> files = Files.walk(cacheLocation)) {
            boolean hasTempFiles = files
                .map(Path::toString)
                .anyMatch(path -> path.endsWith(".tmp"));

            assertFalse(hasTempFiles, "Temp files were not cleaned up after failure");
        }
    }

    @Test
    void recoversFromCorruptedCache() throws IOException {
        // Corrupted cache (invalid xml)
        Files.copy(TestUtils.getResourcePath("broken-cache.xml"), cacheLocation.resolve("cache.xml"));

        // Initialize, should not throw exception
        ObsStateCache stateCache = new ObsStateCache(cacheLocation);

        // Verify the cache still starts empty and functional
        Dependency dependency = TestUtils.createMavenDependency("testGroup", "testArtifact", "1.2.0");
        Checksum checksum = new Checksum("sha256", "9c8a5ae09f3d82cf3af128150682a3590a9c239062ec2a97010ac5f19647f34b");
        assertFalse(stateCache.isDependencyUpToDate(dependency, checksum));

        // Verify we can still write to it (self-healing)
        stateCache.registerDependency(dependency, checksum);
        stateCache.persist();

        // Check that the file was overwritten with valid XML
        String expectedXml = toString(TestUtils.getResourcePath("fixed-broken-cache.xml"));
        String actualXml = toString(cacheLocation.resolve("cache.xml"));

        Diff diff = DiffBuilder.compare(expectedXml)
            .withTest(actualXml)
            .ignoreWhitespace()
            .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndAllAttributes))
            .checkForSimilar()
            .build();

        assertFalse(diff.hasDifferences(), "Cache xml is not correct: " + diff.fullDescription()
            + "\nExpected XML:\n" + expectedXml
            + "\nActual XML:\n" + actualXml);
    }

    @Test
    void redownloadsIfFileIsMissingDespiteCacheHit() throws IOException {
        ObsStateCache stateCache = new ObsStateCache(cacheLocation);
        MetadataEntry metadata = entryOne();

        stateCache.getOrStoreMetadata("repo-missing", metadata, () -> toInputStream("ORIGINAL"));

        Path cachedFile = cacheLocation.resolve("repo-missing_primary.xml");
        assertTrue(Files.exists(cachedFile));
        Files.delete(cachedFile);

        // Requesting the file again should trigger a redownload
        Path cacheFile = stateCache.getOrStoreMetadata("repo-missing", metadata, () -> toInputStream("FRESH"));
        assertEquals("FRESH", toString(cacheFile));
    }

    @Test
    void canCompareDependenciesWithTypesAndClassifiers() {
        ObsStateCache stateCache = new ObsStateCache(cacheLocation);

        // The checksum is artificially the same for all dependencies to test the classifier/type discrimination
        Checksum checksum = new Checksum("sha256", "9c8a5ae09f3d82cf3af128150682a3590a9c239062ec2a97010ac5f19647f34b");

        Dependency defaultDep = TestUtils.createMavenDependency("testGroup", "testArtifact", "1.2.0");
        assertFalse(stateCache.isDependencyUpToDate(defaultDep, checksum));
        stateCache.registerDependency(defaultDep, checksum);

        Dependency pomDep = TestUtils.createMavenDependency("testGroup", "testArtifact", "1.2.0", "pom");
        assertFalse(stateCache.isDependencyUpToDate(pomDep, checksum));
        stateCache.registerDependency(pomDep, checksum);

        Dependency altDep = TestUtils.createMavenDependency("testGroup", "testArtifact", "1.2.0", "jar", "jakarta");
        assertFalse(stateCache.isDependencyUpToDate(altDep, checksum));
        stateCache.registerDependency(pomDep, checksum);
    }

    // Ensure that the given directory is empty
    private static void assertNothingCached(Path location) throws IOException {
        // Ensure nothing has been created inside the cache location
        try (Stream<Path> childrenStream = Files.walk(location)) {
            long childrenCount = childrenStream.filter(p -> !p.equals(location)).count();
            assertEquals(0L, childrenCount);
        }
    }

    // Creates a fake checksum
    private static Checksum getDummyChecksum() {
        return new Checksum("sha256", RandomStringUtils.insecure().nextAlphanumeric(10));
    }

    // Converts the string to an input stream
    private static InputStream toInputStream(String text) {
        return IOUtils.toInputStream(text, StandardCharsets.UTF_8);
    }

    // Converts the content of the file to a string
    private static String toString(Path location) throws IOException {
        return IOUtils.toString(location.toUri(), StandardCharsets.UTF_8);
    }

    private static MetadataEntry entryOne() {
        return new MetadataEntry(
            "primary",
            "primary.xml",
            new Checksum("sha256", "82129c9aa76957ea5e0de3d47409378324836ae7cc6319e0db8b3549d1146c9b"),
            0L,
            Instant.now()
        );
    }

    private static MetadataEntry entryTwo() {
        return new MetadataEntry(
            "primary",
            "primary.xml",
            new Checksum("sha3", "eb9aaf76d660b196a8a296ee9b2d4704272bdee7a137326e6d101e030583c9fb"),
            0L,
            Instant.now()
        );
    }
}
