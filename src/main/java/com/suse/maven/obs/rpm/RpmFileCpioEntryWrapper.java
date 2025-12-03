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

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

/**
 * Internal implementation of {@link RpmFile} that wraps a {@link CpioArchiveEntry}.
 */
class RpmFileCpioEntryWrapper implements RpmFile {

    private final CpioArchiveEntry entry;

    /**
     * Creates a new wrapper for the given CPIO archive entry.
     * @param entry the raw entry from the archive stream.
     */
    RpmFileCpioEntryWrapper(@NotNull CpioArchiveEntry entry) {
        this.entry = Objects.requireNonNull(entry);
    }

    @Override
    @NotNull
    public Path getPath() {
        return Paths.get(sanitizeFileName(entry.getName()));
    }

    @Override
    public long getSize() {
        return entry.getSize();
    }

    @Override
    public boolean isDirectory() {
        return entry.isDirectory();
    }

    @Override
    public boolean isRegularFile() {
        return entry.isRegularFile();
    }

    @Override
    public boolean isSymbolicLink() {
        return entry.isSymbolicLink();
    }

    @Override
    @NotNull
    public LocalDateTime getLastModified() {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(entry.getTime()), ZoneId.systemDefault());
    }

    // Removes leading dot (e.g. "./usr/lib" -> "/usr/lib")
    private static String sanitizeFileName(String rpmPath) {
        if (rpmPath.startsWith(".")) {
            return rpmPath.substring(1);
        }

        return rpmPath;
    }
}
