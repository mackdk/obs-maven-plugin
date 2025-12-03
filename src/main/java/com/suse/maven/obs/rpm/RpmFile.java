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

import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * Represents a single file entry within an RPM package.
 */
public interface RpmFile {

    /**
     * Returns the sanitized, relative path of this entry.
     * @return the relative path of the file (e.g., {@code usr/lib/example.jar}).
     */
    @NotNull
    Path getPath();

    /**
     * Returns the uncompressed size of the file in bytes.
     * @return the size of the file in bytes.
     */
    long getSize();

    /**
     * Checks if this entry represents a directory.
     * @return {@code true} if this entry is a directory; {@code false} otherwise.
     */
    boolean isDirectory();

    /**
     * Checks if this entry represents a regular file.
     * @return {@code true} if this entry is a regular file; {@code false} otherwise.
     */
    boolean isRegularFile();

    /**
     * Checks if this entry represents a symbolic link.
     * @return {@code true} if this entry is a symbolic link; {@code false} otherwise.
     */
    boolean isSymbolicLink();

    /**
     * Returns the last modification time of the file.
     * @return the local date and time when the file was last modified.
     */
    @NotNull
    LocalDateTime getLastModified();
}
