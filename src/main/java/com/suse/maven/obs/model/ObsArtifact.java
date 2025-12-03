package com.suse.maven.obs.model;

import org.apache.maven.artifact.Artifact;

import com.google.common.base.Objects;

public final class ObsArtifact {

    private final String groupId;

    private final String artifactId;

    private final String version;

    private final ObsRepository repository;

    private final String packageName;

    private final String fileFilter;

    public ObsArtifact(Artifact artifact, ObsRepository repository) {
        this(artifact, repository, artifact.getArtifactId(), null);
    }

    public ObsArtifact(Artifact artifact, ObsRepository repository, String packageName) {
        this(artifact, repository, packageName, null);
    }

    public ObsArtifact(Artifact artifact, ObsRepository repository, String packageName, String fileFilter) {
        this.artifactId = artifact.getArtifactId();
        this.groupId = artifact.getGroupId();
        this.version = artifact.getVersion();
        this.repository = repository;
        this.packageName = packageName;
        this.fileFilter = fileFilter;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public ObsRepository getRepository() {
        return repository;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getFileFilter() {
        return fileFilter;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof ObsArtifact)) {
            return false;
        }

        ObsArtifact other = (ObsArtifact) obj;
        return Objects.equal(groupId, other.groupId)
            && Objects.equal(artifactId, other.artifactId)
            && Objects.equal(version, other.version);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(groupId, artifactId, version);
    }

    public String toString() {
        return String.format("%s:%s:%s @ %s", groupId, artifactId, version, repository);
    }
}
