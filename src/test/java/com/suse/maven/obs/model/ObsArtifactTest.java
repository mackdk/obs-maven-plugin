package com.suse.maven.obs.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.suse.maven.obs.TestUtils;

import nl.jqno.equalsverifier.EqualsVerifier;

class ObsArtifactTest {

    @Test
    void equalsContract() {
        EqualsVerifier.forClass(ObsArtifact.class)
            .withOnlyTheseFields("groupId", "artifactId", "version")
            .verify();
    }

    @Test
    void toStringContract() {
        ObsArtifact obsArtifact = TestUtils.createObsArtifact("Uyuni", "systemsmanagement:Uyuni:Master/openSUSE_Leap_15.6",  "salt-net-api", null, "com.suse.salt", "salt-netapi-client", "0.21.0");

        assertEquals(
            "com.suse.salt:salt-netapi-client:0.21.0 @ Uyuni [systemsmanagement:Uyuni:Master/openSUSE_Leap_15.6]",
            obsArtifact.toString()
        );
    }
}
