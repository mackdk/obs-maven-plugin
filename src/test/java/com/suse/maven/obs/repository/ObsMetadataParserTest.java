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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.suse.maven.obs.common.Checksum;
import com.suse.maven.obs.common.Evr;

class ObsMetadataParserTest {

    private ObsMetadataParser parser;

    @BeforeEach
    void setup() {
        parser = new ObsMetadataParser();
    }

    @Nested
    class GetMetadataTest {

        @Test
        void canParseRepomd() throws Exception {
            try (InputStream inputStream = getResourceAsStream("repomd.xml")) {
                MetadataEntry entry = parser.getMetadata(inputStream, "primary");

                assertNotNull(entry, "primary must be parsed successfully");
                assertAll(
                    () -> assertEquals("primary", entry.getType()),
                    () -> assertEquals(
                        "repodata/2a478f6f712afd8ba62215fa33cdb12c5cc3efbc79240bfaae258d4201db260c-primary.xml.gz",
                        entry.getLocation()
                    ),
                    () -> assertEquals(
                        new Checksum("sha256", "2a478f6f712afd8ba62215fa33cdb12c5cc3efbc79240bfaae258d4201db260c"),
                        entry.getChecksum()
                    ),
                    () -> assertEquals(23111L, entry.getSize()),
                    () -> assertEquals(Instant.ofEpochSecond(1766158663), entry.getTimestamp())
                );
            }
        }

        @Test
        void returnsNullWhenMetadataIsNotFound() throws Exception {
            try (InputStream inputStream = getResourceAsStream("repomd.xml")) {
                MetadataEntry entry = parser.getMetadata(inputStream, "fake_dummy_entry");

                assertNull(entry, "fake metadata must not be parsed successfully");
            }
        }

        @Test
        void retrievesFirstEntryIfMoreThanOneAreAvailable() throws Exception {
            try (InputStream inputStream = getResourceAsStream("repomd-wrong-entries.xml")) {
                MetadataEntry entry = parser.getMetadata(inputStream, "primary");

                assertNotNull(entry, "primary must be parsed successfully");
                assertAll(
                    () -> assertEquals("primary", entry.getType()),
                    () -> assertEquals(
                        "repodata/2a478f6f712afd8ba62215fa33cdb12c5cc3efbc79240bfaae258d4201db260c-primary.xml.gz",
                        entry.getLocation()
                    ),
                    () -> assertEquals(
                        new Checksum("sha256", "2a478f6f712afd8ba62215fa33cdb12c5cc3efbc79240bfaae258d4201db260c"),
                        entry.getChecksum()
                    ),
                    () -> assertEquals(178838, entry.getSize()),
                    () -> assertEquals(Instant.ofEpochSecond(1766158779), entry.getTimestamp())
                );
            }
        }

        @Test
        void returnsSecondEntryIfFirstOneIsIncomplete() throws Exception {
            try (InputStream inputStream = getResourceAsStream("repomd-wrong-entries.xml")) {
                MetadataEntry entry = parser.getMetadata(inputStream, "other");

                assertNotNull(entry, "other must be parsed successfully");
                assertAll(
                    () -> assertEquals("other", entry.getType()),
                    () -> assertEquals(
                        "repodata/80ce304e53183f83063365e2fe6f4b677e41c1d2d965fbc53417d8984adf01d4-other.xml.gz",
                        entry.getLocation()
                    ),
                    () -> assertEquals(
                        new Checksum("sha256", "80ce304e53183f83063365e2fe6f4b677e41c1d2d965fbc53417d8984adf01d4"),
                        entry.getChecksum()
                    ),
                    () -> assertEquals(129012, entry.getSize()),
                    () -> assertEquals(Instant.ofEpochSecond(1163457863), entry.getTimestamp())
                );
            }
        }
    }

    @Nested
    class ProcessPrimaryTest {

        @Test
        void canExtractSinglePackage() throws Exception {
            try (InputStream inputStream = getResourceAsStream("primary.xml")) {
                List<PackageEntry> resultList = parser.processPrimary(
                    inputStream,
                    entry -> entry.getName().equals("kie-soup") && entry.getArch().equals("noarch")
                );

                assertNotNull(resultList, "kie-soup must be found in primary.xml");
                assertEquals(1, resultList.size(), "Exactly 1 package must be extracted");

                PackageEntry packageEntry = resultList.get(0);
                assertNotNull(packageEntry, "Extracted PackageEntry must not be null");
                assertAll(
                    () -> assertEquals("kie-soup", packageEntry.getName()),
                    () -> assertEquals("noarch", packageEntry.getArch()),
                    () -> assertEquals(new Evr("7.17.0.Final", "6.125.uyuni3"), packageEntry.getVersion()),
                    () -> assertEquals("noarch/kie-soup-7.17.0.Final-6.125.uyuni3.noarch.rpm",
                        packageEntry.getLocation()),
                    () -> assertEquals(
                        new Checksum("sha256", "ba6831bdf9a3518f2143d88d04b5a16dafee4cc20aac3b9a0d343af683740187"),
                        packageEntry.getChecksum()
                    )
                );
            }
        }

        @Test
        void canMatchAllAvailablePackages() throws Exception {
            try (InputStream inputStream = getResourceAsStream("primary.xml")) {
                List<PackageEntry> resultList = parser.processPrimary(
                    inputStream,
                    entry -> entry.getName().startsWith("apache-commons") && entry.getArch().equals("noarch")
                );

                assertNotNull(resultList, "Extracted result list must not be null");
                assertEquals(6, resultList.size());
                assertEquals(
                    Arrays.asList(
                        "apache-commons-fileupload2-core",
                        "apache-commons-fileupload2-jakarta-servlet5",
                        "apache-commons-fileupload2-jakarta-servlet6",
                        "apache-commons-fileupload2-javadoc",
                        "apache-commons-fileupload2-javax",
                        "apache-commons-fileupload2-portlet"
                    ),
                    resultList.stream().map(PackageEntry::getName).collect(Collectors.toList()));
            }
        }

        @Test
        void returnsEmptyListWhenNothingMatches() throws Exception {
            try (InputStream inputStream = getResourceAsStream("primary.xml")) {
                List<PackageEntry> resultList = parser.processPrimary(inputStream, entry -> false);

                assertNotNull(resultList);
                assertTrue(resultList.isEmpty(), "Result list must be empty");
            }
        }

        @Test
        void ensuresStateDoesNotLeakBetweenEntries() throws Exception {
            try (InputStream inputStream = getResourceAsStream("primary-malformed.xml")) {
                List<PackageEntry> result = parser.processPrimary(inputStream, p -> true);

                assertNotNull(result);
                assertEquals(1, result.size(), "Must return only the valid package");

                PackageEntry packageEntry = result.get(0);
                assertNotNull(packageEntry);
                assertAll(
                    () -> assertEquals("xmlsec", packageEntry.getName()),
                    () -> assertEquals(new Evr("2.0.7", "5"), packageEntry.getVersion()),
                    () -> assertEquals(
                        new Checksum("sha256", "75574bf304802411dd07d572f4c40aab89e28fd9d8a253ee0245aa24d1b592ee"),
                        packageEntry.getChecksum()
                    )
                );
            }
        }

        @Test
        void correctlyParsesPackageWithEpoch() throws Exception {
            try (InputStream inputStream = getResourceAsStream("primary-epoch.xml")) {
                List<PackageEntry> result = parser.processPrimary(
                    inputStream,
                    p -> "apache-commons-fileupload2-core".equals(p.getName()) && "1".equals(p.getVersion().getEpoch())
                );

                assertNotNull(result);
                assertEquals(1, result.size());

                PackageEntry packageEntry = result.get(0);
                assertNotNull(packageEntry);
                assertAll(
                    () -> assertEquals("apache-commons-fileupload2-core", packageEntry.getName()),
                    () -> assertEquals(
                        new Checksum("sha256", "fefe2ccdd9ec87c8de1ebfbda6f24d147145fd080d3d6c0b5634df52e2c5aab1"),
                        packageEntry.getChecksum()
                    ),
                    () -> assertEquals("1", packageEntry.getVersion().getEpoch())
                );
            }
        }

        @Test
        void ignoresNamespacesAndExtraTags() throws Exception {
            try (InputStream inputStream = getResourceAsStream("primary-namespaced.xml")) {
                List<PackageEntry> result = parser.processPrimary(inputStream, p -> true);

                assertNotNull(result);
                assertEquals(1, result.size());

                PackageEntry packageEntry = result.get(0);
                assertNotNull(packageEntry);
                assertAll(
                    () -> assertEquals("optaplanner", packageEntry.getName()),
                    () -> assertEquals("noarch", packageEntry.getArch()),
                    () -> assertEquals(new Evr("7.17.0", "6.125.uyuni3"), packageEntry.getVersion()),
                    () -> assertEquals(
                        new Checksum("sha256", "ff4a7f487bce111679f5823379f483a8fbd9a5abbdcc88104a9fa590f9b9f4c8"),
                        packageEntry.getChecksum()
                    ),
                    () -> assertEquals("noarch/optaplanner-7.17.0-6.125.uyuni3.noarch.rpm", packageEntry.getLocation())
                );
            }
        }
    }

    private static InputStream getResourceAsStream(String resource) {
        InputStream inputStream = ObsMetadataParserTest.class.getResourceAsStream(resource);
        if (inputStream == null) {
            throw new IllegalStateException("Unable to load resource " + resource);
        }

        return inputStream;
    }
}
