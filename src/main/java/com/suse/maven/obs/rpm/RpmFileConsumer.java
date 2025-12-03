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

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * A functional interface for performing operations on a file entry extracted from an RPM.
 * <p>
 * This is a specialized version of {@link java.util.function.BiConsumer} that accepts
 * an {@link RpmFile} and its corresponding {@link InputStream}, and allows for throwing
 * {@link IOException}.
 */
@FunctionalInterface
public interface RpmFileConsumer {

    /**
     * Performs this operation on the given RPM file entry and its data stream.
     * <p>
     * The {@link InputStream} provided is a view of the current entry's data, and it's valid only for
     * the duration of the call. As the input stream is managed by the invoker, closing it is not need.
     * @param rpmFile the metadata of the file entry being processed.
     * @param inputStream the input stream containing the file's data.
     * @throws IOException if an I/O error occurs during the operation.
     */
    void accept(@NotNull RpmFile rpmFile, @NotNull InputStream inputStream) throws IOException;

    /**
     * Returns a composed consumer that performs, in sequence, this operation followed by the
     * {@code after} operation.
     * <p>
     * If performing this operation throws an exception, the {@code after} operation will not be
     * performed.
     * @param after the operation to perform after this operation.
     * @return a composed consumer that performs in sequence this operation followed by the
     *     {@code after} operation.
     * @throws NullPointerException if {@code after} is null.
     */
    @NotNull
    default RpmFileConsumer andThen(@NotNull RpmFileConsumer after) {
        Objects.requireNonNull(after);
        return (l, r) -> {
            this.accept(l, r);
            after.accept(l, r);
        };
    }
}
