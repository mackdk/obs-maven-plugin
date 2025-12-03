package com.suse.maven.obs.rpm;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import com.suse.maven.obs.model.ObsArtifact;

public class RpmArtifactExtractor {

    public List<Path> extract(Path rpmFile, Path outputDirectory, ObsArtifact artifact) throws IOException {
        List<Path> extractedFiles = new ArrayList<>();

        if (!Files.isReadable(outputDirectory)) {
            throw new IOException("Unable to read RPM archive " + rpmFile);
        }

        RpmPackage rpmPackage = new RpmPackage(rpmFile);
        PathMatcher matcher = getArtifactMatcher(artifact);

        rpmPackage.filter(file -> file.isRegularFile() && matcher.matches(file.getPath().getFileName()))
            .forEach((file, inputStream) -> {
                Path targetFile = outputDirectory.resolve(file.getPath().getFileName());
                
                Files.createDirectories(targetFile);
                Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);

                extractedFiles.add(targetFile);
            });

        return extractedFiles;
    }

    private static PathMatcher getArtifactMatcher(ObsArtifact artifact) {
        String matcherExpression;
        if (artifact.getFileFilter() == null) {
            matcherExpression = String.format("glob:%s.{pom,jar}", artifact.getArtifactId());
        } else {
            matcherExpression = String.format("regex:%s", artifact.getFileFilter());
        }

        return FileSystems.getDefault().getPathMatcher(matcherExpression);
    }
}
