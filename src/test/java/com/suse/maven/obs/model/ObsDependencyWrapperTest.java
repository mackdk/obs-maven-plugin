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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.suse.maven.obs.TestUtils;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

class ObsDependencyWrapperTest {

    @Test
    void equalsContract() {
        EqualsVerifier.forClass(ObsDependencyWrapper.class)
            .withOnlyTheseFields("dependency", "repository")
            .verify();
    }

    @Test
    void toStringContract() {
        ObsDependencyWrapper obsArtifact = TestUtils.createObsArtifactWrapper(
            "Uyuni",
            "systemsmanagement:Uyuni:Master/openSUSE_Leap_15.6",
            "salt-net-api",
            "noarch",
            null,
            "com.suse.salt",
            "salt-netapi-client",
            "0.21.0"
        );

        assertEquals(
            "com.suse.salt:salt-netapi-client:0.21.0 @ Uyuni [systemsmanagement:Uyuni:Master/openSUSE_Leap_15.6]",
            obsArtifact.toString()
        );
    }

    @Test
    void canProduceCorrectMatcherExpression() {
        ObsDependencyWrapper obsArtifact = TestUtils.createObsArtifactWrapper(
            "Uyuni",
            "systemsmanagement:Uyuni:Master/openSUSE_Leap_15.6",
            "test-package",
            "noarch",
            null,
            "suse",
            "test-artifact",
            "1.0"
        );
        assertEquals("glob:test-artifact.{pom,jar}", obsArtifact.getMatcherExpression());

        obsArtifact = TestUtils.createObsArtifactWrapper(
            "Uyuni",
            "systemsmanagement:Uyuni:Master/openSUSE_Leap_15.6",
            "test-package",
            "noarch",
            "test-[A-Z]+\\.(jar|pom)",
            "suse",
            "test-artifact",
            "1.0"
        );
        assertEquals("regex:test-[A-Z]+\\.(jar|pom)", obsArtifact.getMatcherExpression());
    }
}
