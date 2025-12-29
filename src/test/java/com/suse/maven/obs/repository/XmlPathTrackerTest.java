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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class XmlPathTrackerTest {

    private XmlPathTracker pathTracker;

    @BeforeEach
    void setup() {
        pathTracker = new XmlPathTracker(3);
    }

    @Test
    void trackerStartsFromZero() {
        assertEquals(0, pathTracker.getDepth());
        assertEquals("/", pathTracker.getPath());
    }

    @Test
    void canTrackUpToMaxTracked() {
        pathTracker.push("test");
        assertEquals(1, pathTracker.getDepth());
        assertEquals("/test", pathTracker.getPath());

        pathTracker.push("first");
        assertEquals(2, pathTracker.getDepth());
        assertEquals("/test/first", pathTracker.getPath());

        pathTracker.pop();
        pathTracker.push("second");
        assertEquals(2, pathTracker.getDepth());
        assertEquals("/test/second", pathTracker.getPath());

        pathTracker.push("leaf");
        assertEquals(3, pathTracker.getDepth());
        assertEquals("/test/second/leaf", pathTracker.getPath());

        pathTracker.pop();
        assertEquals(2, pathTracker.getDepth());
        assertEquals("/test/second", pathTracker.getPath());
    }

    @Test
    void doesNotTrackAboveMaxTracked() {
        pathTracker.push("test", "nested", "leaf");
        assertEquals(3, pathTracker.getDepth());
        assertEquals("/test/nested/leaf", pathTracker.getPath());

        pathTracker.push("yet");
        assertEquals(4, pathTracker.getDepth());
        assertEquals("/test/nested/leaf/...", pathTracker.getPath());

        pathTracker.push("another");
        assertEquals(5, pathTracker.getDepth());
        assertEquals("/test/nested/leaf/...", pathTracker.getPath());

        pathTracker.push("element");
        assertEquals(6, pathTracker.getDepth());
        assertEquals("/test/nested/leaf/...", pathTracker.getPath());

        pathTracker.pop();
        assertEquals(5, pathTracker.getDepth());
        assertEquals("/test/nested/leaf/...", pathTracker.getPath());

        pathTracker.pop();
        assertEquals(4, pathTracker.getDepth());
        assertEquals("/test/nested/leaf/...", pathTracker.getPath());

        pathTracker.pop();
        assertEquals(3, pathTracker.getDepth());
        assertEquals("/test/nested/leaf", pathTracker.getPath());

        pathTracker.pop();
        assertEquals(2, pathTracker.getDepth());
        assertEquals("/test/nested", pathTracker.getPath());

        pathTracker.pop(2);
        assertEquals(0, pathTracker.getDepth());
        assertEquals("/", pathTracker.getPath());
    }
}
