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
package com.suse.maven.obs.repository;

import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import com.suse.maven.obs.common.Checksum;
import com.suse.maven.obs.common.ReusableBuilder;

import java.time.Instant;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Represents a single entry within the repository's metadata index.
 * <p>
 * This class corresponds to a {@code <data>} element in the {@code repomd.xml} index file. It
 * serves as a descriptor of a specific metadata resource (such as the 'primary' package index),
 * providing the necessary details to locate, verify, and identify that resource.
 */
public final class MetadataEntry {

    private final String type;

    private final String location;

    private final Checksum checksum;

    private final long size;

    private final Instant timestamp;

    /**
     * Creates a new metadata entry.
     * @param type the semantic type of the resource (e.g., "primary").
     * @param location the relative path to the file within the repository structure.
     * @param checksum the checksum hash used to verify the file's integrity.
     * @param size the expected size of the file in bytes.
     * @param timestamp the timestamp of the file's last modification.
     */
    public MetadataEntry(@NotNull String type, @NotNull String location, @NotNull Checksum checksum, long size,
                         @NotNull Instant timestamp) {
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.location = Objects.requireNonNull(location, "location cannot be null");
        this.checksum = Objects.requireNonNull(checksum, "checksum cannot be null");
        this.size = size;
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp cannot be null");
    }

    /**
     * Returns the type identifier for this metadata resource.
     * @return the type string (e.g., "primary").
     */
    @NotNull
    public String getType() {
        return type;
    }

    /**
     * Returns the relative path to the resource.
     * @return the relative location path.
     */
    @NotNull
    public String getLocation() {
        return location;
    }


    /**
     * Returns the hash of the resource file.
     * @return the checksum string.
     */
    @NotNull
    public Checksum getChecksum() {
        return checksum;
    }

    /**
     * Returns the size of the resource in bytes.
     * @return the size in bytes.
     */
    public long getSize() {
        return size;
    }


    /**
     * Returns the timestamp of the resource.
     * @return the modification time.
     */
    @NotNull
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, location, checksum);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof MetadataEntry)) {
            return false;
        }

        MetadataEntry other = (MetadataEntry) obj;
        return Objects.equals(type, other.type)
            && Objects.equals(location, other.location)
            && Objects.equals(checksum, other.checksum);
    }

    @Override
    @NotNull
    public String toString() {
        return new StringJoiner(", ", MetadataEntry.class.getSimpleName() + "[", "]")
            .add("type='" + type + "'")
            .add("location='" + location + "'")
            .add("checksum='" + checksum + "'")
            .add("size=" + size)
            .add("timestamp=" + timestamp)
            .toString();
    }

    /**
     * A reusable builder for creating {@link MetadataEntry} instances.
     */
    public static class Builder implements ReusableBuilder<MetadataEntry> {
        private String type;
        private String location;
        private Checksum checksum;
        private long size;
        private Instant timestamp;

        public String type() {
            return this.type;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder location(String location) {
            this.location = location;
            return this;
        }

        public Builder checksum(String algorithm, String hash) {
            return checksum(new Checksum(algorithm, hash));
        }

        public Builder checksum(Checksum checksum) {
            this.checksum = checksum;
            return this;
        }

        public Builder size(long size) {
            this.size = size;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        @Override
        public void reset() {
            type = null;
            location = null;
            checksum = null;
            size = 0;
            timestamp = null;
        }

        @Override
        public boolean isComplete() {
            return size > 0 && ObjectUtils.allNotNull(location, checksum, timestamp);
        }

        @Override
        public MetadataEntry build() {
            if (!isComplete()) {
                throw new IllegalStateException("Unable to build: not all fields have been initialized " + this);
            }

            return new MetadataEntry(type, location, checksum, size, timestamp);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", Builder.class.getSimpleName() + "[", "]")
                .add("type='" + type + "'")
                .add("location='" + location + "'")
                .add("checksum='" + checksum + "'")
                .add("size=" + size)
                .add("timestamp=" + timestamp)
                .toString();
        }
    }
}
