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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

class EvrTest {

    @Test
    void equalsContract() {
        EqualsVerifier.forClass(Evr.class)
            .verify();
    }

    @Test
    void toStringContract() {
        assertEquals("3.5.6-2", new Evr("3.5.6", "2").toString());
        assertEquals("2:3.5.6-2", new Evr("2", "3.5.6", "2").toString());
    }

    @Nested
    class CompareToTests {

        @Test
        void testEpochTakesPrecedenceOverVersion() {
            Evr v1 = new Evr("1", "1.0", "1");
            Evr v2 = new Evr("0", "9.0", "1");

            assertGreaterThan(v1, v2);
        }

        @Test
        void testEpochTakesPrecedenceOverRelease() {
            Evr v1 = new Evr("2", "1.0", "1");
            Evr v2 = new Evr("1", "99.99", "9999");

            assertGreaterThan(v1, v2);
        }

        @Test
        void testVersionDecidesWhenEpochsAreEqual() {
            Evr v1 = new Evr("0", "2.0", "1");
            Evr v2 = new Evr("0", "1.0", "999"); // Release doesn't matter yet

            assertGreaterThan(v1, v2);
        }

        @Test
        void testReleaseDecidesWhenEpochAndVersionAreEqual() {
            Evr v1 = new Evr("0", "1.0", "5.1");
            Evr v2 = new Evr("0", "1.0", "5.0");

            assertGreaterThan(v1, v2);
        }

        @Test
        void testEquality() {
            Evr v1 = new Evr("1", "2.3", "4");
            Evr v2 = new Evr("1", "2.3", "4");

            assertEqualOrder(v1, v2);

            // Verify hashCode/equals consistency while we are at it
            assertEquals(v1, v2);
            assertEquals(v1.hashCode(), v2.hashCode());
        }

        @Test
        void testNullAndEmptyHandling() {
            // "0" vs null (which defaults to "0") -> Equal
            Evr v1 = new Evr("0", "1.0", "1");
            Evr v2 = new Evr(null, "1.0", "1");
            assertEqualOrder(v1, v2);
        }

        @Test
        void testTildeHandlingInEvr() {
            Evr rcCandidate = new Evr("0", "1.0~rc1", "1");
            Evr release = new Evr("0", "1.0", "1");

            // 1.0~rc1 should be LESS than 1.0
            assertLessThan(rcCandidate, release);
        }
    }

    // Helper to make assertions readable: assert v1 > v2
    private static void assertGreaterThan(Evr v1, Evr v2) {
        assertTrue(v1.compareTo(v2) > 0, String.format("Expected '%s' to be greater than '%s'", v1, v2));
    }

    // Helper to make assertions readable: assert v1 < v2
    private static void assertLessThan(Evr v1, Evr v2) {
        assertTrue(v1.compareTo(v2) < 0, String.format("Expected '%s' to be less than '%s'", v1, v2));
    }

    // Helper to make assertions readable: assert v1 == v2
    private static void assertEqualOrder(Evr v1, Evr v2) {
        assertEquals(0, v1.compareTo(v2), String.format("Expected '%s' to be equal to '%s'", v1, v2));
    }
}
