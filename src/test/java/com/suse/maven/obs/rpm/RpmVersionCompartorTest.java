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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * These test cases comes from the original C test file of {@code rpmvercmp}
 */
class RpmVersionCompartorTest {

    private RpmVersionComparator comparator;

    @BeforeEach
    void setup() {
        comparator = new RpmVersionComparator();
    }

    @DisplayName("Comparison of basic versions")
    @ParameterizedTest(name = "[{0}, {1}] => {2}")
    @CsvSource(value = {
        "1.0, 1.0, 0",
        "1.0, 2.0, -1",
        "2.0, 1.0, 1",
        "2.0.1, 2.0.1, 0",
        "2.0, 2.0.1, -1",
        "2.0.1, 2.0, 1",
        "2.0.1a, 2.0.1a, 0",
        "2.0.1a, 2.0.1, 1",
        "2.0.1, 2.0.1a, -1",
        "5.5p1, 5.5p1, 0",
        "5.5p1, 5.5p2, -1",
        "5.5p2, 5.5p1, 1",
        "5.5p10, 5.5p10, 0",
        "5.5p1, 5.5p10, -1",
        "5.5p10, 5.5p1, 1",
        "10xyz, 10.1xyz, -1",
        "10.1xyz, 10xyz, 1",
        "xyz10, xyz10, 0",
        "xyz10, xyz10.1, -1",
        "xyz10.1, xyz10, 1",
        "xyz.4, xyz.4, 0",
        "xyz.4, 8, -1",
        "8, xyz.4, 1",
        "xyz.4, 2, -1",
        "2, xyz.4, 1",
        "5.5p2, 5.6p1, -1",
        "5.6p1, 5.5p2, 1",
        "5.6p1, 6.5p1, -1",
        "6.5p1, 5.6p1, 1",
        "6.0.rc1, 6.0, 1",
        "6.0, 6.0.rc1, -1",
        "10b2, 10a1, 1",
        "10a2, 10b2, -1",
        "1.0aa, 1.0aa, 0",
        "1.0a, 1.0aa, -1",
        "1.0aa, 1.0a, 1",
        "10.0001, 10.0001, 0",
        "10.0001, 10.1, 0",
        "10.1, 10.0001, 0",
        "10.0001, 10.0039, -1",
        "10.0039, 10.0001, 1",
        "4.999.9, 5.0, -1",
        "5.0, 4.999.9, 1",
        "20101121, 20101121, 0",
        "20101121, 20101122, -1",
        "20101122, 20101121, 1",
        "2_0, 2_0, 0",
        "2.0, 2_0, 0",
        "2_0, 2.0, 0",
        "1.1, 1a, 1",
        "9, 10, -1",
        "00009, 0010, -1",
        "1.1, 1.1.PTF, -1",
    })
    void canCompareBasicVersions(String first, String second, int outcome) {
        assertCompareToContract(outcome, first, second);
    }

    @DisplayName("Comparison of versions with tilde sorting")
    @ParameterizedTest(name = "[{0}, {1}] => {2}")
    @CsvSource(value = {
        "1.0~rc1, 1.0~rc1, 0",
        "1.0~rc1, 1.0, -1",
        "1.0, 1.0~rc1, 1",
        "1.0~rc1, 1.0~rc2, -1",
        "1.0~rc2, 1.0~rc1, 1",
        "1.0~rc1~git123, 1.0~rc1~git123, 0",
        "1.0~rc1~git123, 1.0~rc1, -1",
        "1.0~rc1, 1.0~rc1~git123, 1",
    })
    void canCompareWithTilde(String first, String second, int outcome) {
        assertCompareToContract(outcome, first, second);
    }

    @DisplayName("Comparison of versions with caret")
    @ParameterizedTest(name = "[{0}, {1}] => {2}")
    @CsvSource(value = {
        "1.0^, 1.0^, 0",
        "1.0^, 1.0, 1",
        "1.0, 1.0^, -1",
        "1.0^git1, 1.0^git1, 0",
        "1.0^git1, 1.0, 1",
        "1.0, 1.0^git1, -1",
        "1.0^git1, 1.0^git2, -1",
        "1.0^git2, 1.0^git1, 1",
        "1.0^git1, 1.01, -1",
        "1.01, 1.0^git1, 1",
        "1.0^20160101, 1.0^20160101, 0",
        "1.0^20160101, 1.0.1, -1",
        "1.0.1, 1.0^20160101, 1",
        "1.0^20160101^git1, 1.0^20160101^git1, 0",
        "1.0^20160102, 1.0^20160101^git1, 1",
        "1.0^20160101^git1, 1.0^20160102, -1",
    })
    void canCompareWithCaret(String first, String second, int outcome) {
        assertCompareToContract(outcome, first, second);
    }

    @DisplayName("Comparison of versions with caret and tilds")
    @ParameterizedTest(name = "[{0}, {1}] => {2}")
    @CsvSource(value = {
        "1.0~rc1^git1, 1.0~rc1^git1, 0",
        "1.0~rc1^git1, 1.0~rc1, 1",
        "1.0~rc1, 1.0~rc1^git1, -1",
        "1.0^git1~pre, 1.0^git1~pre, 0",
        "1.0^git1, 1.0^git1~pre, 1",
        "1.0^git1~pre, 1.0^git1, -1",
    })
    void canCompareWithCaretAndTilde(String first, String second, int outcome) {
        assertCompareToContract(outcome, first, second);
    }

    @DisplayName("Comparison of RHEL 8 module releases")
    @ParameterizedTest(name = "[{0}, {1}] => {2}")
    @CsvSource(value = {
        "7.module_el8.2.0+305+5e198a41, 7.module_el8.2.0+458+dab581ed, -1",
        "10.module+el8.2.0+7749+4a513fb2, 10.module+el8.2.0+7749+5a513fb2, -1",
        "6.module+el8+1645+8d4014a6, 7.module_el8.2.0+458+dab581ed, -1",
    })
    void canCompareRhel8ModuleRelease(String first, String second, int outcome) {
        assertCompareToContract(outcome, first, second);
    }

    // See https://bugzilla.redhat.com/show_bug.cgi?id=178798
    @DisplayName("Ensure BUG 178798 is fixed")
    @ParameterizedTest(name = "[{0}, {1}] => {2}")
    @CsvSource(value = {
        "a, a, 0",
        "a+, a+, 0",
        "a+, a_, 0",
        "a_, a+, 0",
        "+a, +a, 0",
        "+a, _a, 0",
        "_a, +a, 0",
        "+_, +_, 0",
        "_+, +_, 0",
        "_+, _+, 0",
        "+, _, 0",
        "_, +, 0",
    })
    void ensureRhBug178798IsFixed(String first, String second, int outcome) {
        assertCompareToContract(outcome, first, second);
    }

    // See https://bugzilla.redhat.com/show_bug.cgi?id=811992
    @DisplayName("Respecting behaviour of BUG 811992")
    @ParameterizedTest(name = "[{0}, {1}] => {2}")
    @CsvSource(value = {
        "1b.fc17, 1b.fc17, 0",
        "1b.fc17, 1.fc17, -1",
        "1.fc17, 1b.fc17, 1",
        "1g.fc17, 1g.fc17, 0",
        "1g.fc17, 1.fc17, 1",
        "1.fc17, 1g.fc17, -1",
    })
    void ensureBug811992IsRespected(String first, String second, int outcome) {
        assertCompareToContract(outcome, first, second);
    }

    // See https://bugzilla.redhat.com/show_bug.cgi?id=50977
    @DisplayName("Ensure BUG 50977 is fixed")
    @ParameterizedTest(name = "[{0}, {1}] => {2}")
    @CsvSource(value = {
        // From comment #2
        "10mdk, 10, 1",
        "10mdk, 10.1mdk, -1",
        "9, ximian.1, 1",

        // From comment #19
        "1.4snap, 1.4.5, -1",
        "4.0x, 4.0.36, -1",
        "p19, 2.0.0, -1",
        "2.0e, 2.0e, 0",
        "2.0e, 2.0.11, -1",

        // From duplicate bug https://bugzilla.redhat.com/show_bug.cgi?id=82639
        "1, asp1.7x.2, 1",
        "ipl4mdk, alt0.8, 1",
        "alt0.8, ipl4mdk, -1",
        "1asp, alt1, 1",
    })
    void ensureRhBug50977IsFixed(String first, String second, int outcome) {
        assertCompareToContract(outcome, first, second);
    }

    // See https://github.com/uyuni-project/uyuni/issues/2531
    @DisplayName("Ensure Uyuni issue 2531 is fixed")
    @ParameterizedTest(name = "[{0}, {1}] => {2}")
    @CsvSource(value = {
        "1.27+1.3.9, 1.27.1+1.3.9, 1",
        "1.27, 1.3.11, 1",
    })
    void ensureUyuniIssue2531IsFixed(String first, String second, int outcome) {
        assertCompareToContract(outcome, first, second);
    }

    @DisplayName("Ensure non ASCII characters are considered equals")
    @ParameterizedTest(name = "[{0}, {1}] => {2}")
    @CsvSource(value = {
        "1.1.α, 1.1.α, 0",
        "1.1.α, 1.1.β, 0",
        "1.1.β, 1.1.α, 0",
        "1.1.αα, 1.1.α, 0",
        "1.1.α, 1.1.ββ, 0",
        "1.1.ββ, 1.1.αα, 0",
    })
    void ensureNonAsciiAreConsideredEquals(String first, String second, int outcome) {
        assertCompareToContract(outcome, first, second);
    }

    private void assertCompareToContract(int outcome, String first, String second) {
        if (outcome == 0) {
            assertEquals(outcome, comparator.compare(first, second), "Result should be 0");
            assertEquals(outcome, comparator.compare(second, first), "Symmetrict result should be 0");
        } else if (outcome < 0) {
            assertTrue(comparator.compare(first, second) < 0, "Result should have been negative");
            assertTrue(comparator.compare(second, first) > 0, "Symmetric result should have been positive");
        } else {
            assertTrue(comparator.compare(first, second) > 0, "Result should have been positive");
            assertTrue(comparator.compare(second, first) < 0, "Symmetric result should have been negative");
        }
    }
}
