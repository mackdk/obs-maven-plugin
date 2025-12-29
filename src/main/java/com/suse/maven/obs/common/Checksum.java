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

import java.util.Objects;
import java.util.StringJoiner;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a cryptographic hash used to verify the integrity of a file or package.
 */
public final class Checksum {

    private final String algorithm;

    private final String hash;

    /**
     * Constructs a new Checksum instance.
     * @param algorithm the hashing algorithm identifier (e.g., "sha256"). Must not be null.
     * @param hash the checksum hash string. Must not be null.
     */
    public Checksum(@NotNull String algorithm, @NotNull String hash) {
        this.algorithm = algorithm;
        this.hash = hash;
    }

    /**
     * Returns the type of hashing algorithm used.
     * @return the algorithm type (e.g., "sha256").
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * Returns the actual hash value.
     * @return the checksum string.
     */
    public String getHash() {
        return hash;
    }

    @Override
    public int hashCode() {
        return Objects.hash(algorithm, hash);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof Checksum))
            return false;
        Checksum other = (Checksum) obj;
        return Objects.equals(algorithm, other.algorithm) && Objects.equals(hash, other.hash);
    }

    @Override
    @NotNull
    public String toString() {
        return new StringJoiner(", ", Checksum.class.getSimpleName() + "[", "]")
            .add("algorithm='" + algorithm + "'")
            .add("hash='" + hash + "'")
            .toString();
    }
}
