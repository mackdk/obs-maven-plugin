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
package com.suse.maven.obs.model;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A class describing an OBS repository.
 */
public final class ObsRepository {

    private static final Pattern OBS_PROJECT_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._-]++(?::[a-zA-Z0-9._-]++)*+/[a-zA-Z0-9._-]++$"
    );

    private final String name;

    private final String project;

    private final String repository;

    private final String downloadUrl;

    /**
     * Default constructor.
     * @param name the name of the repository
     * @param baseUrl the base HTTP/S url of all the repositories
     * @param identifier an OBS identifier in the form of {@code project}/{@code repository},
     *     which is resolved against the {@code baseUrl}
     */
    public ObsRepository(@NotNull String name, @NotNull String baseUrl, @NotNull String identifier) {
        this.name = Objects.requireNonNull(name, "name must be not null");

        String validatedUrl = validateUrl(name, Objects.requireNonNull(baseUrl, "baseUrl must be not null"));
        String[] parts = validateProject(name, Objects.requireNonNull(identifier, "identifier must be not null"))
            .split("/");

        this.project = parts[0];
        this.repository = parts[1];
        this.downloadUrl = String.format("%s/%s/",
            StringUtils.stripEnd(validatedUrl, "/"),
            identifier.replace(":", ":/")
        );
    }

    /**
     * Default constructor.
     * @param name the name of the repository
     * @param url a full direct HTTP/S url pointing to the published repository
     */
    public ObsRepository(@NotNull String name, @NotNull String url) {
        this.name = Objects.requireNonNull(name, "name must be not null");
        this.project = null;
        this.repository = null;
        this.downloadUrl = validateUrl(name, Objects.requireNonNull(url, "url must be not null"));
    }

    /**
     * Retrieves the name of the repository. It must be unique in the context of the current maven
     * project configuration.
     * @return the name identifying this repository instance.
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * Retrieves the OBS project name, if configured.
     * @return the project name or {@code null} if this instance was initialized with a full HTTP
     *     url.
     */
    @Nullable
    public String getProject() {
        return project;
    }

    /**
     * Retrieves the repository name as declared inside the OBS project, if configured.
     * @return the repository name, or {@code null} if this instance was initialized with a full
     *     HTTP url.
     */
    @Nullable
    public String getRepository() {
        return repository;
    }

    /**
     * The HTTP/S url where the repository is accessible and from where the packages can be downloaded.
     * @return a string representing the HTTP/S url.
     */
    @NotNull
    public String getDownloadUrl() {
        return downloadUrl;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, downloadUrl);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof ObsRepository)) {
            return false;
        }

        ObsRepository other = (ObsRepository) obj;
        return Objects.equals(name, other.name) && Objects.equals(downloadUrl, other.downloadUrl);
    }

    @Override
    @NotNull
    public String toString() {
        if (project != null && repository != null) {
            return String.format("%s [%s/%s]", name, project, repository);
        }

        return String.format("%s [%s]", name, downloadUrl);
    }

    private static String validateProject(String name, String identifier) {
        if (identifier == null || !OBS_PROJECT_PATTERN.matcher(identifier).matches()) {
            throw new IllegalArgumentException("The repository identifier is not valid for repository " + name);
        }

        return identifier;
    }

    private static String validateUrl(String name, String repositoryUrl) {
        if (repositoryUrl == null) {
            throw new IllegalArgumentException("The URL must be non-null for repository " + name);
        }

        try {
            URI uri = new URI(repositoryUrl);

            // URI must be absolute
            if (!uri.isAbsolute()) {
                throw new IllegalArgumentException("The URL must be absolute for repository " + name);
            }

            // Only http or https URL are supported
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new IllegalArgumentException("Unsupported schema specified for repository " + name);
            }

            return repositoryUrl;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("The repository URL is invalid for repository " + name);
        }
    }
}
