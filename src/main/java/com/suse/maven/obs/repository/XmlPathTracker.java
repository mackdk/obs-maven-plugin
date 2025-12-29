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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import org.jetbrains.annotations.NotNull;

/**
 * Helper class to keep track of the depth and the tags processed when processing an
 * {@link javax.xml.stream.XMLStreamReader} data stream.
 * <p>
 * The path tracking is limited to a maximum number of tracked tags in order to limit memory usage.
 * When the number of nested tags is higher than the maximum specified the path is truncated. For
 * example, if the limit is 2 any tag deeper than that will have a path of
 * {@code /parent/nested/...}.
 */
final class XmlPathTracker {

    private static final String SEPARATOR = "/";

    private final int maxTrackedDepth;

    private final List<String> path;

    private int depth;

    /**
     * Builds a tracker instance with the specified limit.
     * @param maxTrackedDepth the maximum depth to keep track of in the path
     */
    XmlPathTracker(int maxTrackedDepth) {
        this.depth = 0;
        this.maxTrackedDepth = maxTrackedDepth;
        this.path = new ArrayList<>(maxTrackedDepth);
    }

    /**
     * Adds an element to the tracker
     * @param element the name of the element
     */
    public void push(@NotNull String element) {
        if (depth < maxTrackedDepth) {
            path.add(element);
        }

        depth++;
    }

    /**
     * Adds all the elements to the tracker.
     * <p>
     * The invocation {@code push("a", "b", "c")} has the same effect as
     * {@code push("a"); push("b"); push("c")}.
     * @param elements the names of the elements to push
     */
    public void push(@NotNull String... elements) {
        Arrays.stream(Objects.requireNonNull(elements, "elements must not be null"))
            .forEach(this::push);
    }

    /**
     * Removes the last pushed element from the tracker.
     */
    public void pop() {
        if (depth == 0) {
            return;
        }

        depth--;

        if (depth < maxTrackedDepth) {
            path.remove(depth);
        }
    }

    /**
     * Removes the last pushed elements from the tracker.
     * <p>
     * The invocation {@code pop(n)} has the same effect of calling {@code pop()} for {@code n} times.
     * @param times the number of elements to remove.
     */
    public void pop(int times) {
        for (int i = 0; i < times; i++) {
            pop();
        }
    }

    /**
     * Retrieves the current path.
     * @return the exact path if the current depth is lower than the limit set on creation, or a
     *     truncated path instead.
     *     <p>
     *     For example, if the elements {@code parent, child, leaf} were pushed to the tracker before
     *     calling {@code getPath()} the result will be:
     *     <ul>
     *     <li>{@code /parent/child/leaf} if the max depth was set to 3 or higher</li>
     *     <li>{@code /parent/child/...} if the max depth was set to 2</li>
     *     <li>{@code /parent/...} if the max depth was set to 1</li>
     *     </ul>
     */
    @NotNull
    public String getPath() {
        StringJoiner joiner = new StringJoiner(SEPARATOR, SEPARATOR, "");

        path.forEach(joiner::add);

        if (depth > maxTrackedDepth) {
            joiner.add("...");
        }

        return joiner.toString();
    }

    /**
     * Get the current depth.
     * @return the current value of the depth (i.e. the number of pushed elements minus the number of
     *     popped elements).
     */
    public int getDepth() {
        return depth;
    }
}
