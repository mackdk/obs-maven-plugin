/*
 * Copyright (c) 2025--2026 SUSE LLC
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 */
package com.suse.maven.obs;

import com.suse.maven.obs.model.ObsDependencyWrapper;
import com.suse.maven.obs.model.ObsRepository;

import org.apache.maven.model.Dependency;

import java.math.BigInteger;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Utility class providing helper methods for unit tests.
 */
public class TestUtils {

    private TestUtils() {
        // Prevent instantiation
    }

    /**
     * Resolves the file system path for a test resource file, obtaining the test class location
     * from the stack trace.
     * @param resourceName the name of the resource file (relative to the test class).
     * @return the {@link Path} to the resource file.
     * @throws IllegalStateException if the resource URI syntax is invalid, the resource cannot be
     *     found, or the calling test class cannot be identified.
     */
    public static Path getResourcePath(String resourceName) {
        try {
            URL resourceUrl = getTestClass().getResource(resourceName);
            if (resourceUrl == null) {
                throw new IllegalStateException("Cannot load resource " + resourceName);
            }
            return Paths.get(resourceUrl.toURI());
        } catch (URISyntaxException ex) {
            throw new IllegalStateException("Unable to define path for resource " + resourceName, ex);
        }
    }

    /**
     * Creates a minimal {@link ObsDependencyWrapper} for testing purposes and project="test",
     * repo="SUSE:test/standard", arch="noarch", filter=null.
     * @param groupId the Maven group ID.
     * @param artifactId the Maven artifact ID.
     * @param version the Maven version.
     * @return a populated {@link ObsDependencyWrapper}.
     */
    public static ObsDependencyWrapper createObsArtifactWrapper(String groupId, String artifactId, String version) {
        return createObsArtifactWrapper("test", "SUSE:test/standard", artifactId, "noarch", null, groupId, artifactId,
            version);
    }

    /**
     * Creates an {@link ObsDependencyWrapper} with a specific package name and file filter and
     * project="test", repo="SUSE:test/standard", arch="noarch".
     * @param packageName the OBS package name.
     * @param fileFilter the regex filter for files inside the package (nullable).
     * @param groupId the Maven group ID.
     * @param artifactId the Maven artifact ID.
     * @param version the Maven version.
     * @return a populated {@link ObsDependencyWrapper}.
     */
    public static ObsDependencyWrapper createObsArtifactWrapper(String packageName, String fileFilter, String groupId,
                                                                String artifactId, String version) {
        return createObsArtifactWrapper("test", "SUSE:test/standard", packageName, "noarch", fileFilter, groupId,
            artifactId, version);
    }

    /**
     * Creates a fully customized {@link ObsDependencyWrapper}.
     * @param obsProjectName the OBS project name.
     * @param obsPath the OBS repository path (e.g., "SUSE:SLES15:SP3/standard").
     * @param packageName the OBS package name.
     * @param arch the target architecture (e.g., "x86_64", "noarch").
     * @param fileFilter the regex filter for files inside the package (nullable).
     * @param groupId the Maven group ID.
     * @param artifactId the Maven artifact ID.
     * @param version the Maven version.
     * @return a populated {@link ObsDependencyWrapper}.
     */
    public static ObsDependencyWrapper createObsArtifactWrapper(String obsProjectName, String obsPath,
                                                                String packageName,
                                                                String arch, String fileFilter, String groupId,
                                                                String artifactId, String version) {
        Dependency mvnArtifact = createMavenDependency(groupId, artifactId, version);
        ObsRepository obsRepo = new ObsRepository(obsProjectName, "https://download.opensuse.org/repositories",
            obsPath);

        return new ObsDependencyWrapper(mvnArtifact, obsRepo, packageName, arch, fileFilter);
    }

    /**
     * Creates a Maven {@link Dependency} with the specified coordinates.
     * @param groupId the group ID.
     * @param artifactId the artifact ID.
     * @param version the version string.
     * @return a maven dependency.
     */
    public static Dependency createMavenDependency(String groupId, String artifactId, String version) {
        return createMavenDependency(groupId, artifactId, version, "compile", "jar", null);
    }

    /**
     * Creates a Maven {@link Dependency} with the specified coordinates.
     * @param groupId the group ID.
     * @param artifactId the artifact ID.
     * @param version the version string.
     * @param type the type
     * @return a maven dependency.
     */
    public static Dependency createMavenDependency(String groupId, String artifactId, String version, String type) {
        return createMavenDependency(groupId, artifactId, version, "compile", type, null);
    }

    /**
     * Creates a Maven {@link Dependency} with the specified coordinates.
     * @param groupId the group ID.
     * @param artifactId the artifact ID.
     * @param version the version string.
     * @param type the type
     * @param classifier the classifier
     * @return a maven dependency.
     */
    public static Dependency createMavenDependency(String groupId, String artifactId, String version, String type,
                                                   String classifier) {
        return createMavenDependency(groupId, artifactId, version, "compile", type, classifier);
    }

    /**
     * Creates a Maven {@link Dependency} with the specified coordinates.
     * @param groupId the group ID.
     * @param artifactId the artifact ID.
     * @param version the version string.
     * @param scope the scope
     * @param type the type
     * @param classifier the classifier
     * @return a maven dependency.
     */
    public static Dependency createMavenDependency(String groupId, String artifactId, String version, String scope,
                                                   String type, String classifier) {
        Dependency dependency = new Dependency();
        dependency.setGroupId(groupId);
        dependency.setArtifactId(artifactId);
        dependency.setVersion(version);
        dependency.setScope(scope);
        dependency.setType(type);
        dependency.setClassifier(classifier);
        return dependency;
    }

    /**
     * Converts a hexadecimal string into a byte array.
     * <p>
     * This method is lenient regarding formatting; it automatically strips any whitespace (spaces,
     * tabs, newlines) from the input string before parsing.
     * @param src the non-null hexadecimal string to convert. It may contain whitespace separators.
     * @return a byte array representing the hexadecimal values.
     * @throws NumberFormatException if the input string (after removing whitespace) contains characters
     *     that are not valid hexadecimal digits.
     */
    public static byte[] fromHexString(String src) {
        // Convert to BigInteger by prepending "10" in order to:
        // - preserve leading zeros
        // - have the resulting byte array always treated as a positive magnitude
        byte[] biBytes = new BigInteger("10" + src.replaceAll("\\s", ""), 16).toByteArray();
        return Arrays.copyOfRange(biBytes, 1, biBytes.length);
    }

    /**
     * Recursively deletes a directory and all its contents.
     * @param targetPath the path to the directory to remove.
     * @throws IOException if the directory traversal fails or a file cannot be deleted.
     */
    public static void removeDirectoryTree(Path targetPath) throws IOException {
        if (!Files.exists(targetPath)) {
            return;
        }

        try (Stream<Path> temporaryFiles = Files.walk(targetPath)) {
            temporaryFiles.sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        } catch (UncheckedIOException ex) {
            throw ex.getCause();
        }
    }

    private static Class<?> getTestClass() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        return Arrays.stream(stackTrace)
            .map(StackTraceElement::getClassName)
            .filter(className -> className.endsWith("Test"))
            .findFirst()
            .map(TestUtils::loadClassByName)
            .orElseThrow(() -> new IllegalStateException("Unable to find unit test class from stacktrace"));
    }

    private static Class<?> loadClassByName(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Unable to load unit test class " + className, ex);
        }
    }
}
