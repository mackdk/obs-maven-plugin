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
package com.suse.maven.obs.repository;

import com.suse.maven.obs.common.Checksum;
import com.suse.maven.obs.common.Evr;
import com.suse.maven.obs.common.ReusableBuilder;

import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Represents a single package definition found in the repository's primary metadata index.
 * <p>
 * This class maps to the {@code <package>} element in the {@code primary.xml} file.
 * It contains the essential metadata (Name, Version, Architecture) required to identify
 * the package and the location/checksum information needed to download and verify it.
 * <p>
 * This class is immutable and uses the package's Name, Architecture, and Version
 * for identity (equality checks).
 */
public final class PackageEntry {

    private final String name;

    private final String arch;

    private final Evr version;

    private final String location;

    private final Checksum checksum;

    /**
     * Constructs a new package entry.
     * @param name the name of the package (e.g., "glibc").
     * @param arch the target architecture (e.g., "x86_64", "noarch").
     * @param version the evr version
     * @param location the relative URL path to the RPM file.
     * @param checksum the checksum hash for file verification.
     */
    public PackageEntry(@NotNull String name, @NotNull String arch, @NotNull Evr version, @NotNull String location,
                        @NotNull Checksum checksum) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.arch = Objects.requireNonNull(arch, "arch must not be null");
        this.version = Objects.requireNonNull(version, "version must not be null");
        this.location = Objects.requireNonNull(location, "location must not be null");
        this.checksum = Objects.requireNonNull(checksum, "checksum must not be null");
    }

    /**
     * Returns the name of the package.
     * @return the package name.
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * Returns the architecture of the package.
     * @return the architecture string (e.g., "x86_64").
     */
    @NotNull
    public String getArch() {
        return arch;
    }

    /**
     * Returns the version of the package.
     * @return the version string.
     */
    @NotNull
    public Evr getVersion() {
        return version;
    }

    /**
     * Returns the location of the RPM file, relative to the repository root.
     * @return the location path.
     */
    @NotNull
    public String getLocation() {
        return location;
    }

    /**
     * Returns the checksum of the RPM file.
     * @return the checksum string.
     */
    @NotNull
    public Checksum getChecksum() {
        return checksum;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, arch, version);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof PackageEntry)) {
            return false;
        }

        PackageEntry other = (PackageEntry) obj;
        return Objects.equals(name, other.name)
            && Objects.equals(arch, other.arch)
            && Objects.equals(version, other.version);
    }

    @Override
    @NotNull
    public String toString() {
        return new StringJoiner(", ", PackageEntry.class.getSimpleName() + "[", "]")
            .add("name='" + name + "'")
            .add("arch='" + arch + "'")
            .add("version='" + version + "'")
            .add("location='" + location + "'")
            .add("checksum='" + checksum + "'")
            .toString();
    }

    /**
     * A reusable builder for creating {@link PackageEntry} instances.
     */
    public static final class Builder implements ReusableBuilder<PackageEntry> {
        private String name;
        private String arch;
        private Evr version;
        private String location;
        private Checksum checksum;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder arch(String arch) {
            this.arch = arch;
            return this;
        }

        public Builder version(String version, String release) {
            return version(new Evr(version, release));
        }

        @SuppressWarnings("UnusedReturnValue")
        public Builder version(String epoch, String version, String release) {
            return version(new Evr(epoch, version, release));
        }

        public Builder version(Evr version) {
            this.version = version;
            return this;
        }

        public Builder location(String location) {
            this.location = location;
            return this;
        }

        public Builder checksum(String algorithm, String hash) {
            return checksum(new Checksum(algorithm, hash));
        }

        private Builder checksum(Checksum checksum) {
            this.checksum = checksum;
            return this;
        }

        @Override
        public void reset() {
            name = null;
            arch = null;
            version = null;
            location = null;
            checksum = null;
        }

        @Override
        public boolean isComplete() {
            return ObjectUtils.allNotNull(name, arch, version, location, checksum);
        }

        @Override
        public PackageEntry build() {
            if (!isComplete()) {
                throw new IllegalStateException("Unable to build: not all fields have been initialized " + this);
            }

            return new PackageEntry(name, arch, version, location, checksum);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", Builder.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("arch='" + arch + "'")
                .add("version=" + version)
                .add("location='" + location + "'")
                .add("checksum='" + checksum + "'")
                .toString();
        }
    }
}
