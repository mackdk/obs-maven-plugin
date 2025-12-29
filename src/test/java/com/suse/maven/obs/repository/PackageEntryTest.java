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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.jparams.verifier.tostring.ToStringVerifier;
import com.suse.maven.obs.common.Checksum;
import com.suse.maven.obs.common.Evr;

import nl.jqno.equalsverifier.EqualsVerifier;

class PackageEntryTest {

    @Test
    void equalsContract() {
        EqualsVerifier.forClass(PackageEntry.class)
            .withOnlyTheseFields("name", "arch", "version")
            .verify();
    }

    @Test
    void toStringContract() {
        ToStringVerifier.forClasses(PackageEntry.class, PackageEntry.Builder.class)
            .verify();
    }

    @Test
    void canCreateInstanceUsingBuilder() {
        PackageEntry.Builder builder = new PackageEntry.Builder()
            .name("test-package")
            .arch("x86_64")
            .version("1.3.2", "5")
            .location("http://example.com/repo/test-package.rpm")
            .checksum("sha256", "DUMMY_CHECKSUM");

        assertTrue(builder.isComplete(), "Builder should be complete");
        assertEquals(
            new PackageEntry(
                "test-package",
                "x86_64",
                new Evr("1.3.2", "5"),
                "http://example.com/repo/test-package.rpm",
                new Checksum("sha256", "DUMMY_CHECKSUM")
            ),
            builder.build()
        );
    }

    @Test
    void builderThrowsExceptionWhenFieldsAreMissing() {
        PackageEntry.Builder builder = new PackageEntry.Builder()
            .name("test-package")
            .arch("noarch");

        assertFalse(builder.isComplete(), "Builder should be incomplete");
        assertThrows(IllegalStateException.class, builder::build);
    }
}
