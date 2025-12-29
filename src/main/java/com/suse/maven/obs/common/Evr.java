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
package com.suse.maven.obs.common;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.suse.maven.obs.rpm.RpmVersionComparator;

import java.util.Comparator;
import java.util.Objects;

/**
 * Represents the full versioning information of an RPM package (epoch:version-release).
 * <p>
 * This class implements standard RPM comparison logic: epoch is compared first, followed by
 * version, and finally release.
 */
public final class Evr implements Comparable<Evr> {

    private static final String DEFAULT_EPOCH = "0";

    private static final RpmVersionComparator RPM_COMPARATOR = new RpmVersionComparator();

    private static final Comparator<Evr> EVR_COMPARATOR = Comparator.comparing(Evr::getEpoch, RPM_COMPARATOR)
        .thenComparing(Evr::getVersion, RPM_COMPARATOR)
        .thenComparing(Evr::getRelease, RPM_COMPARATOR);

    private final String epoch;

    private final String version;

    private final String release;

    /**
     * Creates a new EVR instance.
     * @param version the upstream version (must not be null).
     * @param release the release version (must not be null).
     */
    public Evr(@NotNull String version, @NotNull String release) {
        this(null, version, release);
    }

    /**
     * Creates a new EVR instance.
     * @param epoch the epoch string (can be null; treated as "0").
     * @param version the upstream version (must not be null).
     * @param release the release version (must not be null).
     */
    public Evr(@Nullable String epoch, @NotNull String version, @NotNull String release) {
        this.epoch = StringUtils.defaultIfBlank(epoch, DEFAULT_EPOCH);
        this.version = Objects.requireNonNull(version, "version must not be null").trim();
        this.release = Objects.requireNonNull(release, "release must not be null").trim();
    }

    /**
     * Retrieves the epoch number, used to override normal version comparisons (defaults to 0).
     * @return the epoch number
     */
    @Nullable
    public String getEpoch() {
        return DEFAULT_EPOCH.equals(epoch) ? null : epoch;
    }

    /**
     * Retrieves the upstream version of the software.
     * @return the upstream version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Retrieves the package maintainer's release number.
     * @return the package maintainer's release number
     */
    @NotNull
    public String getRelease() {
        return release;
    }

    @Override
    public int compareTo(@NotNull Evr other) {
        return EVR_COMPARATOR.compare(this, other);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Evr))
            return false;
        Evr evr = (Evr) o;
        return Objects.equals(epoch, evr.epoch) &&
            Objects.equals(version, evr.version) &&
            Objects.equals(release, evr.release);
    }

    @Override
    public int hashCode() {
        return Objects.hash(epoch, version, release);
    }

    @Override
    public String toString() {
        if (DEFAULT_EPOCH.equals(epoch)) {
            return String.join("-", version, release);
        }

        return String.format("%s:%s-%s", epoch, version, release);
    }
}
