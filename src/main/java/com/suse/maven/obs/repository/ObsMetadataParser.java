/*
 * Copyright (c) 2025-2026 SUSE LLC
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 */
package com.suse.maven.obs.repository;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.suse.maven.obs.common.SafeXml;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import javax.inject.Named;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * A StAX-based parser for OBS (Open Build Service) repository metadata.
 * <p>
 * This class handles the low-level XML parsing of the YUM/RPM-MD repository structure and it's
 * designed for memory efficiency using streaming APIs (StAX) and reusable object builders.
 */
@Named
public class ObsMetadataParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObsMetadataParser.class);

    private static final String DATA_PATH = "/repomd/data";

    private static final String PACKAGE_PATH = "/metadata/package";

    /**
     * Parses the repository index (repomd.xml) to find a specific metadata entry.
     * @param dataStream the input stream of the {@code repomd.xml} file.
     * @param dataType the type of metadata to retrieve (e.g., "primary", "filelists").
     * @return the matching {@link MetadataEntry}, or {@code null} if not found.
     * @throws XMLStreamException if the XML is malformed or parsing fails.
     */
    @Nullable
    public MetadataEntry getMetadata(@NotNull InputStream dataStream, @NotNull String dataType)
        throws XMLStreamException {
        XMLStreamReader reader = SafeXml.newStreamReader(
            Objects.requireNonNull(dataStream, "dataStream must not be null")
        );

        MetadataEntry.Builder builder = new MetadataEntry.Builder();
        XmlPathTracker tracker = new XmlPathTracker(3);

        while (reader.hasNext()) {
            int next = reader.next();

            if (reader.isStartElement()) {
                tracker.push(reader.getLocalName());
            }

            boolean correctType = dataType.equals(builder.type());
            String path = tracker.getPath();
            int depth = tracker.getDepth();

            if (next == XMLStreamConstants.START_ELEMENT && path.startsWith(DATA_PATH) && depth <= 3) {
                updateMetadataEntryBuilder(reader, builder);
            } else if (next == XMLStreamConstants.END_ELEMENT && correctType && path.equals(DATA_PATH)) {
                if (builder.isComplete()) {
                    return builder.build();
                }

                LOGGER.error("Missing metadata for {}: {}", dataType, builder);
            }

            if (reader.isEndElement()) {
                tracker.pop();
            }
        }

        return null;
    }

    // Process the tags of repomd.xml updating the object builder
    private static void updateMetadataEntryBuilder(XMLStreamReader reader, MetadataEntry.Builder builder)
        throws XMLStreamException {
        switch (reader.getLocalName()) {
            case "data":
                builder.reset();
                builder.type(reader.getAttributeValue(null, "type"));
                break;

            case "checksum":
                String algorithm = reader.getAttributeValue(null, "type");
                String hash = reader.getElementText();

                builder.checksum(algorithm, hash);
                break;

            case "location":
                builder.location(reader.getAttributeValue(null, "href"));
                break;

            case "size":
                long size = Long.parseLong(reader.getElementText());
                builder.size(size);
                break;

            case "timestamp":
                long seconds = Long.parseLong(reader.getElementText());
                builder.timestamp(Instant.ofEpochSecond(seconds));
                break;

            default:
                // nothing to do on other tags
                break;
        }
    }

    /**
     * Parses the primary package index (primary.xml) to extract the information of all the packages
     * matching the specified filter.
     * @param primaryStream the input stream of the {@code primary.xml} file.
     * @param packageFilter a predicate to filter which packages should be retained.
     * @return a list of fully populated {@link PackageEntry} objects.
     * @throws XMLStreamException if the XML is malformed or parsing fails.
     */
    @NotNull
    public List<PackageEntry> processPrimary(@NotNull InputStream primaryStream,
                                             @NotNull Predicate<PackageEntry> packageFilter)
        throws XMLStreamException {
        XMLStreamReader reader = SafeXml.newStreamReader(
            Objects.requireNonNull(primaryStream, "primaryStream must not be null")
        );

        PackageEntry.Builder builder = new PackageEntry.Builder();
        List<PackageEntry> resultList = new ArrayList<>();
        XmlPathTracker tracker = new XmlPathTracker(2);

        while (reader.hasNext()) {
            int next = reader.next();

            if (reader.isStartElement()) {
                tracker.push(reader.getLocalName());
            }

            String path = tracker.getPath();
            int depth = tracker.getDepth();

            if (next == XMLStreamConstants.START_ELEMENT && path.startsWith(PACKAGE_PATH) && depth <= 3) {
                updatePackageEntryBuilder(reader, builder);
            } else if (next == XMLStreamConstants.END_ELEMENT && path.equals(PACKAGE_PATH)) {
                buildPackageEntry(builder)
                    .filter(packageFilter)
                    .ifPresent(resultList::add);
            }

            if (reader.isEndElement()) {
                tracker.pop();
            }
        }

        return resultList;
    }

    // Process the tags of primary.xml updating the object builder
    private static void updatePackageEntryBuilder(XMLStreamReader reader, PackageEntry.Builder builder)
        throws XMLStreamException {
        switch (reader.getLocalName()) {
            case "package":
                builder.reset();
                break;
            case "name":
                builder.name(reader.getElementText());
                break;
            case "arch":
                builder.arch(reader.getElementText());
                break;
            case "version":
                String epoch = reader.getAttributeValue(null, "epoch");
                String ver = reader.getAttributeValue(null, "ver");
                String rel = reader.getAttributeValue(null, "rel");

                builder.version(epoch, ver, rel);
                break;
            case "location":
                builder.location(reader.getAttributeValue(null, "href"));
                break;
            case "checksum":
                String algorithm = reader.getAttributeValue(null, "type");
                String hash = reader.getElementText();

                builder.checksum(algorithm, hash);
                break;

            default:
                // nothing to do on other tags
                break;
        }
    }

    // Finalize the processing and build the package entry if possible
    private static Optional<PackageEntry> buildPackageEntry(PackageEntry.Builder builder) {
        if (!builder.isComplete()) {
            LOGGER.error("Ignoring package with incomplete data: {}", builder);
            return Optional.empty();
        }

        return Optional.of(builder.build());
    }
}
