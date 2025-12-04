package com.suse.maven.obs.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

import com.google.common.base.Objects;

public final class ObsRepository {

    private static final Pattern OBS_PROJECT_PATTERN = Pattern
            .compile("^[a-zA-Z0-9._-]++(?::[a-zA-Z0-9._-]++)*+/[a-zA-Z0-9._-]++$");

    private final String name;

    private final String project;

    private final String repository;

    private final String downloadUrl;

    public ObsRepository(String name, String path) {
        this.name = name;

        if (isValidObsProject(path)) {
            String[] parts = path.split("/");

            this.project = parts[0];
            this.repository = parts[1];
            this.downloadUrl = String.format("https://download.opensuse.org/repositories/%s/", path.replace(":", ":/"));
        } else if (isValidRepositoryUrl(path)) {
            this.project = null;
            this.repository = null;
            this.downloadUrl = path;
        } else {
            throw new IllegalArgumentException("Invalid path " + path);
        }
    }

    public String getName() {
        return name;
    }

    public String getProject() {
        return project;
    }

    public String getRepository() {
        return repository;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, downloadUrl);
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
        return Objects.equal(name, other.name) && Objects.equal(downloadUrl, other.downloadUrl);
    }

    @Override
    public String toString() {
        if (project != null && repository != null) {
            return String.format("%s [%s/%s]", name, project, repository);
        }

        return String.format("%s [%s]", name, downloadUrl);
    }

    private static boolean isValidObsProject(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }

        return OBS_PROJECT_PATTERN.matcher(path).matches();
    }

    private static boolean isValidRepositoryUrl(String path) {
        if (path == null) {
            return false;
        }

        try {
            URI uri = new URI(path);

            // URI must be absolute
            if (!uri.isAbsolute()) {
                return false;
            }

            // Only http or https URL are supported
            String scheme = uri.getScheme();
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);

        } catch (URISyntaxException e) {
            // malformed URL syntax
            return false;
        }
    }
}
