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

import org.jetbrains.annotations.NotNull;


/**
 * Interface for object builders that can be reset and reused.
 * <p>
 * Unlike standard builders which are often designed for a single use, a {@code ReusableBuilder}
 * is exposes a {@link #reset()} method to clear its internal state, allowing the same instance to
 * construct multiple objects sequentially.
 * @param <T> the type of object constructed by this builder.
 */
public interface ReusableBuilder<T> {

    /**
     * Resets the builder to its initial, empty state.
     */
    void reset();

    /**
     * Checks if that all mandatory fields have been set.
     * @return {@code true} if the builder is ready to build; {@code false} otherwise.
     */
    boolean isComplete();

    /**
     * Constructs the final object instance.
     * @return the constructed object.
     * @throws IllegalStateException if the builder is not complete (i.e., {@link #isComplete()} returns
     *     {@code false}).
     */
    @NotNull
    T build();
}
