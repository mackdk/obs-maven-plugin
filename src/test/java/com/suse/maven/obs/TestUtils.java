package com.suse.maven.obs;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.testing.stubs.ArtifactStub;

import com.suse.maven.obs.model.ObsArtifact;
import com.suse.maven.obs.model.ObsRepository;

public class TestUtils {

    private TestUtils() {
        // Prevent instantiation
    }

    public static Path getResourcePath(String packageName) {
        try {
            URL resourceUrl = getTestClass().getResource(packageName);
            return Paths.get(resourceUrl.toURI());
        } catch (URISyntaxException ex) {
            throw new IllegalStateException("Unable to define path for resource " + packageName, ex);
        }
    }


    public static ObsArtifact createObsArtifact(String groupId, String artifactId, String version) {
        return createObsArtifact("test", "SUSE:test/standard", artifactId, null, groupId, artifactId, version);
    }

    public static ObsArtifact createObsArtifact(String packageName, String fileFilter, String groupId, String artifactId, String version) {
        return createObsArtifact("test", "SUSE:test/standard", packageName, fileFilter, groupId, artifactId, version);
    }

    public static ObsArtifact createObsArtifact(String obsProjectName, String obsPath, String packageName, String fileFilter, String groupId, String artifactId, String version) {
        Artifact mvnArtifact = createMavenArtifact(groupId, artifactId, version);
        ObsRepository obsRepo = new ObsRepository(obsProjectName, obsPath);

        return new ObsArtifact(mvnArtifact, obsRepo, packageName, fileFilter);
    }

    public static Artifact createMavenArtifact(String groupId, String artifactId, String version) {
        Artifact mvnArtifact = new ArtifactStub();
        mvnArtifact.setGroupId(groupId);
        mvnArtifact.setArtifactId(artifactId);
        mvnArtifact.setVersion(version);
        return mvnArtifact;
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
