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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Wrapper for an {@link Artifact} that adds the pieces of information needed to allow retrieving
 * the artifact from an OBS repository.
 */
public final class ObsDependencyWrapper {

    private final Dependency dependency;

    private final ObsRepository repository;

    private final String packageName;

    private final String fileFilter;

    private final String architecture;

    /**
     * Create a new instance wrapping an existing maven dependency.
     * @param dependency the maven dependency
     * @param repository the obs repository publishing the rpm containing the dependency
     */
    public ObsDependencyWrapper(@NotNull Dependency dependency, @NotNull ObsRepository repository) {
        this(dependency, repository, Objects.requireNonNull(dependency, "dependency must be not null").getArtifactId(),
            "noarch", null);
    }

    /**
     * Create a new instance wrapping an existing maven dependency.
     * @param dependency the maven dependency
     * @param repository the obs repository publishing the rpm containing the dependency
     * @param pkgName the name of the rpm package
     */
    public ObsDependencyWrapper(@NotNull Dependency dependency, @NotNull ObsRepository repository,
                                @NotNull String pkgName) {
        this(dependency, repository, pkgName, "noarch", null);
    }

    /**
     * Create a new instance wrapping an existing maven dependency.
     * @param dependency the maven dependency
     * @param repository the obs repository publishing the rpm containing the dependency
     * @param pkgName the name of the rpm package
     * @param arch the architecture of the rpm package
     * @param fileFilter a regular expression used to extract the dependency from the package
     */
    public ObsDependencyWrapper(@NotNull Dependency dependency, @NotNull ObsRepository repository,
                                @NotNull String pkgName, @NotNull String arch, @Nullable String fileFilter) {
        this.dependency = Objects.requireNonNull(dependency, "dependency must be not null");
        this.repository = Objects.requireNonNull(repository, "repository must be not null");
        this.packageName = Objects.requireNonNull(pkgName, "packageName must be not null");
        this.architecture = Objects.requireNonNull(arch, "architecture must not null");
        this.fileFilter = fileFilter;
    }

    /**
     * Retrieves the Maven {@link Dependency} wrapped by this instance.
     * @return the wrapped maven dependency.
     */
    @NotNull
    public Dependency getMavenDependency() {
        return dependency;
    }

    /**
     * Retrieves the {@link ObsRepository} publishing the package of this dependency.
     * @return the OBS repository
     */
    @NotNull
    public ObsRepository getRepository() {
        return repository;
    }

    /**
     * Retrieves the name of the package containing the Maven dependency.
     * @return the name of the package inside the repository.
     */
    @NotNull
    public String getPackageName() {
        return packageName;
    }

    /**
     * Retrieves the architecture of the package containing the Maven dependency.
     * @return the architecture of the rpm package
     */
    @NotNull
    public String getArchitecture() {
        return architecture;
    }

    /**
     * Retrieves the filter used to match the jar and the pom within the RPM package.
     * @return a regular expression or {@code null} if the jar and the pom
     *     match the artifact id.
     */
    @Nullable
    public String getFileFilter() {
        return fileFilter;
    }

    /**
     * Retrieves an expression that can be used in {@link java.nio.file.PathMatcher} to filter the
     * artifact files.
     * @return a valid matcher expression.
     */
    @NotNull
    public String getMatcherExpression() {
        if (fileFilter != null) {
            return String.format("regex:%s", fileFilter);
        }

        return String.format("glob:%s.{pom,jar}", dependency.getArtifactId());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof ObsDependencyWrapper)) {
            return false;
        }

        ObsDependencyWrapper other = (ObsDependencyWrapper) obj;
        return Objects.equals(dependency, other.dependency) && Objects.equals(repository, other.repository);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dependency, repository);
    }

    @Override
    @NotNull
    public String toString() {
        return String.format("%s:%s:%s @ %s",
            dependency.getGroupId(),
            dependency.getArtifactId(),
            dependency.getVersion(),
            repository
        );
    }
}
