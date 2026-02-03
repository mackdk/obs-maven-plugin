/*
 * Copyright (c) 2026 SUSE LLC
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 */
package com.suse.maven.obs.common;

/**
 * A supplier of results that can throw a checked exception.
 * <p>
 * This functional interface is equivalent to the standard {@link java.util.function.Supplier}, but
 * it allows the definition of a checked exception (e.g., {@link java.io.IOException}).
 * @param <T> the type of results supplied by this supplier.
 * @param <E> the type of checked exception that may be thrown.
 */
@FunctionalInterface
public interface ThrowingSupplier<T, E extends Exception> {

    /**
     * Gets a result.
     * @return a result.
     * @throws E if the operation fails.
     */
    T get() throws E;
}
