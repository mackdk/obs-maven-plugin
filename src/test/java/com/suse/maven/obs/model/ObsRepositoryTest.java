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
package com.suse.maven.obs.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import nl.jqno.equalsverifier.EqualsVerifier;

class ObsRepositoryTest {

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
        // @formatter:off
        "https://download.opensuse.org/repositories, openSUSE:Leap:15.6/standard, openSUSE:Leap:15.6, standard, https://download.opensuse.org/repositories/openSUSE:/Leap:/15.6/standard/",
        "https://download.opensuse.org/repositories/, systemsmanagement:Uyuni:Master:Other/openSUSE_Leap_15.6, systemsmanagement:Uyuni:Master:Other, openSUSE_Leap_15.6, https://download.opensuse.org/repositories/systemsmanagement:/Uyuni:/Master:/Other/openSUSE_Leap_15.6/",
        // @formatter:on
    })
    void canCreateInstanceFromIdentifier(String base, String identifier, String project, String repository,
                                         String url) {
        ObsRepository obsRepository =
            new ObsRepository("test", base, identifier);

        assertAll(
            () -> assertEquals(project, obsRepository.getProject()),
            () -> assertEquals(repository, obsRepository.getRepository()),
            () -> assertEquals(url, obsRepository.getDownloadUrl())
        );
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
        "https://download.opensuse.org/update/leap/15.6/sle, https://download.opensuse.org/update/leap/15.6/sle"
    })
    void canCreateInstanceFromUrl(String url) {
        ObsRepository obsRepository = new ObsRepository("test", url);

        assertAll(
            () -> assertNull(obsRepository.getProject()),
            () -> assertNull(obsRepository.getRepository()),
            () -> assertEquals(url, obsRepository.getDownloadUrl())
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "openSUSE:/Leap:/15.6/standard",
        "openSUSE/Leap/15.6/standard",
        "openSUSE:Leap:15.6",
        "/standard",
    })
    void throwsExceptionWhenIdentifierIsWrong(String identifier) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> new ObsRepository("repo", "https://example.com/repo", identifier)
        );
        assertEquals("The repository identifier is not valid for repository repo", ex.getMessage());
    }

    @Test
    void throwsExceptionWhenUrlIsWrong() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> new ObsRepository("repo", "https://invalid.url/example[/].html"));
        assertEquals("The repository URL is invalid for repository repo", ex.getMessage());

        ex = assertThrows(IllegalArgumentException.class,
            () -> new ObsRepository("repo", "../random/relative/path/"));
        assertEquals("The URL must be absolute for repository repo", ex.getMessage());

        ex = assertThrows(IllegalArgumentException.class,
            () -> new ObsRepository("repo", "file:///var/log/repo"));
        assertEquals("Unsupported schema specified for repository repo", ex.getMessage());
    }

    @Test
    void equalsContract() {
        EqualsVerifier.forClass(ObsRepository.class)
            .withIgnoredFields("project", "repository")
            .verify();
    }

    @Test
    void toStringContract() {
        assertEquals(
            "Leap [openSUSE:Leap:15.6/standard]",
            new ObsRepository("Leap", "https://download.opensuse.org/repositories", "openSUSE:Leap:15.6/standard")
                .toString()
        );

        assertEquals(
            "Leap_sle [https://download.opensuse.org/update/leap/15.6/sle]",
            new ObsRepository("Leap_sle", "https://download.opensuse.org/update/leap/15.6/sle").toString()
        );
    }


}
