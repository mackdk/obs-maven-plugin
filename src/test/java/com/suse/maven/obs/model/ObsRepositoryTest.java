package com.suse.maven.obs.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import nl.jqno.equalsverifier.EqualsVerifier;

class ObsRepositoryTest {

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
        "openSUSE:Leap:15.6/standard, openSUSE:Leap:15.6, standard, https://download.opensuse.org/repositories/openSUSE:/Leap:/15.6/standard/",
        "systemsmanagement:Uyuni:Master:Other/openSUSE_Leap_15.6, systemsmanagement:Uyuni:Master:Other, openSUSE_Leap_15.6, https://download.opensuse.org/repositories/systemsmanagement:/Uyuni:/Master:/Other/openSUSE_Leap_15.6/",
        "https://download.opensuse.org/update/leap/15.6/sle, null, null, https://download.opensuse.org/update/leap/15.6/sle"
    })
    void canParseCorrectPaths(String path, String project, String repository, String url) {
        ObsRepository obsRepository = new ObsRepository("test", path);

        assertAll(
            () -> assertEquals(project, obsRepository.getProject()),
            () -> assertEquals(repository, obsRepository.getRepository()),
            () -> assertEquals(url, obsRepository.getDownloadUrl())
        );
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
        "openSUSE:/Leap:/15.6/standard",
        "openSUSE/Leap/15.6/standard",
        "openSUSE:Leap:15.6",
        "/standard",
        "file:///var/log/repo",
        "../random/relative/path/",
        "",
        "https://invalid.url/example[/].html"
    })
    void throwsExceptionWhenPathIsWrong(String path) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new ObsRepository("test", path));
        assertEquals("Invalid path " + path, ex.getMessage());
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
            new ObsRepository("Leap", "openSUSE:Leap:15.6/standard").toString()
        );
        
        assertEquals(
            "Leap_sle [https://download.opensuse.org/update/leap/15.6/sle]",
            new ObsRepository("Leap_sle", "https://download.opensuse.org/update/leap/15.6/sle").toString()
        );
    }
    

}
