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

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * A utility service responsible for extracting specific artifacts from an RPM package.
 * <p>
 * This extractor streams the content of an RPM file, filters entries based on a
 * provided {@link PathMatcher}, and extracts matching files to a specified
 * output directory.
 */
@Named
@Singleton
public class RpmArtifactExtractor {

    /**
     * Extracts files from the specified RPM that match the given pattern.
     * <p>
     * This method processes the RPM stream sequentially. Only entries that are regular files and whose
     * filenames match the provided {@code matcher} are extracted.
     * @param rpmFile the path to the source RPM file to be processed. Must be a readable file.
     * @param outputDirectory the target directory where matching files will be stored. Directories will
     *     be created as needed.
     * @param matcher a {@link PathMatcher} used to filter files by name (e.g., glob:*.jar).
     * @return a list of {@link Path}s representing the successfully extracted files.
     * @throws IOException if the RPM file cannot be read, the output directory cannot be created, or an
     *     I/O error occurs during extraction.
     */
    @NotNull
    public List<Path> extract(@NotNull Path rpmFile, @NotNull Path outputDirectory, @NotNull PathMatcher matcher)
        throws IOException {
        List<Path> extractedFiles = new ArrayList<>();

        if (!Files.isReadable(rpmFile)) {
            throw new IOException("Unable to read RPM archive " + rpmFile);
        }

        RpmPackage rpmPackage = new RpmPackage(rpmFile);

        rpmPackage.filter(file -> file.isRegularFile() && matcher.matches(file.getPath().getFileName()))
            .forEach((file, inputStream) -> {
                Path targetFile = outputDirectory.resolve(file.getPath().getFileName());

                Files.createDirectories(targetFile.getParent());
                Files.copy(inputStream, targetFile);

                extractedFiles.add(targetFile);
            });

        return extractedFiles;
    }
}
