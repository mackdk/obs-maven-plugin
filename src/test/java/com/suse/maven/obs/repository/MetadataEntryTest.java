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

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.jparams.verifier.tostring.ToStringVerifier;
import com.suse.maven.obs.common.Checksum;

import nl.jqno.equalsverifier.EqualsVerifier;

class MetadataEntryTest {

    @Test
    void equalsContract() {
        EqualsVerifier.forClass(MetadataEntry.class)
            .withOnlyTheseFields("type", "location", "checksum")
            .verify();
    }

    @Test
    void toStringContract() {
        ToStringVerifier.forClasses(MetadataEntry.class, MetadataEntry.Builder.class)
            .verify();
    }

    @Test
    void canCreateInstanceUsingBuilder() {
        Instant timestamp = Instant.now();

        MetadataEntry.Builder builder = new MetadataEntry.Builder()
            .type("primary")
            .location("http://example.com/repo")
            .size(12345L)
            .timestamp(timestamp)
            .checksum("sha256", "DUMMY_CHECKSUM");

        assertTrue(builder.isComplete(), "Builder should be complete");
        assertEquals(
            new MetadataEntry(
                "primary",
                "http://example.com/repo",
                new Checksum("sha256", "DUMMY_CHECKSUM"),
                12345L,
                timestamp
            ),
            builder.build()
        );
    }

    @Test
    void builderThrowsExceptionWhenFieldsAreMissing() {
        MetadataEntry.Builder builder = new MetadataEntry.Builder()
            .type("primary")
            .size(12345L);

        assertFalse(builder.isComplete(), "Builder should be incomplete");
        assertThrows(IllegalStateException.class, builder::build);
    }

}
