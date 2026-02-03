/*
 * Copyright (c) 2026 SUSE LLC
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 */
package com.suse.maven.obs.resolution;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.maven.model.Dependency;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.suse.maven.obs.common.Checksum;
import com.suse.maven.obs.common.SafeXml;
import com.suse.maven.obs.common.ThrowingSupplier;
import com.suse.maven.obs.repository.MetadataEntry;

/**
 * Persists the state of downloaded repository metadata and artifacts to disk.
 * <p>
 * This cache acts as a state manager to prevent re-downloading large XML metadata files or
 * artifacts if the server-side checksum has not changed since the last build.
 */
@Named
@Singleton
public class ObsStateCache {

    private static final String ROOT_TAG = "checksums";

    private static final String ENTRY_TAG = "entry";

    private static final String CACHE_FILE = "cache.xml";

    private static final Logger LOGGER = LoggerFactory.getLogger(ObsStateCache.class);

    private final Path cacheLocation;

    private final Map<String, Checksum> checksumMap;

    /**
     * Default constructor.
     * <p>
     * Initializes the cache in the user's home directory under {@code ~/.cache/obs-maven-plugin}.
     */
    public ObsStateCache() {
        this(Paths.get(System.getProperty("user.home"), ".cache", "obs-maven-plugin"));
    }

    /**
     * Constructor for testing only.
     * @param cacheLocation the directory path where cache files should be stored.
     */
    ObsStateCache(@NotNull Path cacheLocation) {
        this.cacheLocation = ensureValidLocation(cacheLocation);
        this.checksumMap = loadCache(cacheLocation.resolve(CACHE_FILE));
    }

    /**
     * Retrieves the local path to a metadata file, downloading it only if the remote checksum differs.
     * <p>
     * This method implements a "Check-then-Act" logic:
     * <ol>
     * <li>Checks if the metadata file exists locally.</li>
     * <li>Compares the provided remote checksum against the stored local checksum.</li>
     * <li>If they match, returns the local path (Cache Hit).</li>
     * <li>If they differ, executes the {@code dataSupplier} to download the stream, saves it to disk,
     * and updates the cache (Cache Miss).</li>
     * </ol>
     * @param repositoryName the unique name of the repository (used for namespacing).
     * @param metadata the metadata entry containing the remote checksum and type.
     * @param dataSupplier a supplier that opens the download stream. Only called on cache miss.
     * @return the path to the valid local file.
     * @throws IOException if the file creation or download fails.
     */
    @NotNull
    public Path getOrStoreMetadata(@NotNull String repositoryName, @NotNull MetadataEntry metadata,
                                   @NotNull ThrowingSupplier<InputStream, IOException> dataSupplier)
        throws IOException {
        String key = KeyGenerator.forIndex(repositoryName, metadata);

        Path indexLocation = cacheLocation.resolve(String.format("%s_%s.xml", repositoryName, metadata.getType()));

        // HIT: Return local file if checksum matches and file actually exists on disk
        Checksum cachedChecksum = checksumMap.get(key);
        if (cachedChecksum != null && Files.isReadable(indexLocation)
            && cachedChecksum.equals(metadata.getChecksum())) {
            return indexLocation;
        }

        // MISS: Download and update cache
        // Download to a temp file first to prevent corruption to never expose a partial file
        Path tempPath = Files.createTempFile(indexLocation.getParent(), "obs-download-", ".tmp");
        try (InputStream in = dataSupplier.get()) {
            Files.copy(in, tempPath, StandardCopyOption.REPLACE_EXISTING);
            Files.move(tempPath, indexLocation, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            checksumMap.put(key, metadata.getChecksum());
        } catch (IOException e) {
            // Clean up temp file on failure
            Files.deleteIfExists(tempPath);
            throw e;
        }

        return indexLocation;
    }

    /**
     * Checks if a dependency is already downloaded and up-to-date.
     * @param dependency the maven {@link Dependency}
     * @param checksum the remote checksum to verify against.
     * @return {@code true} if the dependency is cached and matches the checksum; {@code false} otherwise.
     */
    public boolean isDependencyUpToDate(@NotNull Dependency dependency, @NotNull Checksum checksum) {
        Checksum cachedChecksum = checksumMap.get(KeyGenerator.forDependency(dependency));

        return cachedChecksum != null && cachedChecksum.equals(checksum);
    }

    /**
     * Registers a successfully downloaded dependency in the cache.
     * @param dependency the maven {@link Dependency}
     * @param checksum the checksum of the downloaded file.
     */
    public void registerDependency(@NotNull Dependency dependency, @NotNull Checksum checksum) {
        checksumMap.put(KeyGenerator.forDependency(dependency), checksum);
    }

    /**
     * Persists the in-memory cache state to the `cache.xml` file on disk.
     * <p>
     * This method needs to be called at the end of the build or execution cycle to ensure that the
     * state is saved for future runs. It is synchronized to prevent file corruption in parallel builds
     * ({@code mvn -T}).
     */
    public synchronized void persist() {
        saveCache(cacheLocation.resolve(CACHE_FILE), checksumMap);
    }

    // Checks that the specified location for the cache exists and it's writeable
    private static Path ensureValidLocation(Path cacheLocation) {
        try {
            if (!Files.exists(cacheLocation)) {
                Files.createDirectories(cacheLocation);
                return cacheLocation;
            }

            // Verify it's a directory
            if (!Files.isDirectory(cacheLocation)) {
                throw new IOException("Given path is not a directory");
            }

            // Check if files can be created
            Path probe = Files.createTempFile(cacheLocation, ".write-test-", null);
            Files.delete(probe);

            return cacheLocation;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Invalid location for cache: " + cacheLocation, ex);
        }
    }

    // Loads the cache from a previous run. Implementation uses {@link ConcurrentHashMap} for in-memory
    // storage to achieve thread-safety in parallel Maven builds ({@code mvn -T})
    private static Map<String, Checksum> loadCache(Path cacheFilePath) {
        if (!Files.isReadable(cacheFilePath)) {
            return new ConcurrentHashMap<>();
        }

        try (Reader reader = Files.newBufferedReader(cacheFilePath, StandardCharsets.UTF_8)) {
            DocumentBuilder documentBuilder = SafeXml.newDocumentBuilder(false);

            Document document = documentBuilder.parse(new InputSource(reader));
            NodeList rootNodes = document.getChildNodes();

            int rootCount = rootNodes.getLength();
            if (rootCount != 1) {
                throw new IOException("Invalid xml format, expected one root, got " + rootCount);
            }

            Node root = rootNodes.item(0);
            if (!ROOT_TAG.equals(root.getNodeName())) {
                throw new IOException("Invalid xml format, expected " + ROOT_TAG + ", got " + root.getNodeName());
            }

            return parseMapFromDom(root);
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            LOGGER.warn("Unable to restore state cache, using empty cache", ex);
            return new ConcurrentHashMap<>();
        }
    }

    // Parse the XML DOM and creates the data map
    private static Map<String, Checksum> parseMapFromDom(Node root) {
        Map<String, Checksum> dataMap = new ConcurrentHashMap<>();

        NodeList children = root.getChildNodes();
        IntStream.range(0, children.getLength())
            .mapToObj(children::item)
            .filter(node -> ENTRY_TAG.equals(node.getNodeName()))
            .map(Node::getAttributes)
            .forEach(attributes -> {
                String key = attributes.getNamedItem("key").getNodeValue();
                String algorithm = attributes.getNamedItem("type").getNodeValue();
                String hash = attributes.getNamedItem("hash").getNodeValue();

                if (ObjectUtils.allNotNull(key, algorithm, hash)) {
                    dataMap.put(key, new Checksum(algorithm, hash));
                }
            });

        return dataMap;
    }

    // Save the current cache in an XML format.
    private static void saveCache(Path path, Map<String, Checksum> dataMap) {
        if (Files.exists(path) && !Files.isWritable(path)) {
            return;
        }

        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            DocumentBuilder documentBuilder = SafeXml.newDocumentBuilder(false);
            Transformer transformer = SafeXml.newTransformer();

            Document document = documentBuilder.newDocument();
            transformer.transform(new DOMSource(createDomFromMap(document, dataMap)), new StreamResult(writer));
        } catch (ParserConfigurationException | TransformerException | IOException ex) {
            LOGGER.error("Unable to write state cache, next execution might use outdated data", ex);
        }
    }

    // Create the XML DOM from a map
    private static Document createDomFromMap(Document document, Map<String, Checksum> dataMap) {
        Element root = document.createElement(ROOT_TAG);
        document.appendChild(root);

        dataMap.entrySet().stream()
            .sorted(Entry.comparingByKey())
            .map(entry -> {
                Element child = document.createElement(ENTRY_TAG);
                child.setAttribute("key", entry.getKey());
                child.setAttribute("type", entry.getValue().getAlgorithm());
                child.setAttribute("hash", entry.getValue().getHash());
                return child;
            })
            .forEach(root::appendChild);

        return document;
    }

    // Internal class for generating the cache keys
    private static class KeyGenerator {
        private enum CacheType {
            INDEX, DEPENDENCY
        }

        private KeyGenerator() {
            // Prevent instantiation
        }

        public static String forDependency(@NotNull Dependency dependency) {
            String coordinates = dependency.getManagementKey();
            String key = String.format("%s:%s", CacheType.DEPENDENCY.name().toLowerCase(), coordinates);
            LOGGER.debug("Generated key {} for artifact {}", key, dependency);
            return key;
        }

        public static String forIndex(@NotNull String repository, @NotNull MetadataEntry metadata) {
            String indexCoordinates = String.join(":", repository, metadata.getType());
            String key = String.format("%s:%s", CacheType.INDEX.name().toLowerCase(), indexCoordinates);
            LOGGER.debug("Generated key {} for repository {} with metadata {}", key, repository, metadata);
            return key;
        }
    }
}
