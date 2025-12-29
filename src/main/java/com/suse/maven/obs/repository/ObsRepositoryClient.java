package com.suse.maven.obs.repository;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.zip.GZIPInputStream;

import javax.inject.Named;

import com.suse.maven.obs.model.ObsRepository;

import org.jetbrains.annotations.NotNull;

/**
 * A client for interacting with Open Build Service (OBS) repositories over HTTP.
 */
@Named
public class ObsRepositoryClient {

    private static final String USER_AGENT = "obs-maven-plugin/1.0.0";

    private static final String REPO_MD_LOCATION = "repodata/repomd.xml";

    /**
     * Fetches the main repository index (repomd.xml).
     * @param repository the repository configuration.
     * @return an input stream containing the XML data.
     * @throws IOException if the connection fails or the resource cannot be found.
     */
    @NotNull
    public InputStream fetchRepositoryIndex(@NotNull ObsRepository repository) throws IOException {
        URI uri = resolve(repository.getDownloadUrl(), REPO_MD_LOCATION);
        return openStream(uri);
    }


    /**
     * Fetches a specific metadata resource (e.g., primary.xml.gz).
     * <p>
     * If the metadata location indicates a compressed file (ending in .gz), this method automatically
     * wraps the stream to decompress it, allowing the caller to consume standard XML.
     * @param repository the repository configuration.
     * @param metadata the metadata entry to fetch.
     * @return an input stream containing the XML data.
     * @throws IOException if the connection fails.
     */
    @NotNull
    public InputStream fetchMetadata(@NotNull ObsRepository repository, @NotNull MetadataEntry metadata)
        throws IOException {
        URI uri = resolve(repository.getDownloadUrl(), metadata.getLocation());
        InputStream metadataStream = openStream(uri);

        if (metadata.getLocation().endsWith(".gz")) {
            return new GZIPInputStream(metadataStream);
        }

        return metadataStream;
    }

    /**
     * Downloads an RPM package to a temporary location.
     * @param repository the repository configuration.
     * @param rpmPackage the package entry to download.
     * @return the path to the downloaded temporary file.
     * @throws IOException if the download fails.
     */
    @NotNull
    public Path downloadPackage(@NotNull ObsRepository repository, @NotNull PackageEntry rpmPackage)
        throws IOException {
        Path downloadLocation = Files.createTempFile(
            repository.getName(),
            rpmPackage.getName(),
            PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"))
        );

        URI uri = resolve(repository.getDownloadUrl(), rpmPackage.getLocation());

        try (InputStream rpmStream = openStream(uri)) {
            Files.copy(rpmStream, downloadLocation, StandardCopyOption.REPLACE_EXISTING);
        }

        return downloadLocation;
    }

    // Opens an HTTP connection to the specified URI.
    private static InputStream openStream(URI location) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) location.toURL().openConnection();

        // Set timeouts to prevent build freezes
        connection.setConnectTimeout(5_000); // 5 seconds to connect
        connection.setReadTimeout(30_000); // 30 seconds to read data
        connection.setInstanceFollowRedirects(true);

        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Accept-Encoding", "gzip");

        int status = connection.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            throw new IOException(String.format(
                "Failed to fetch %s. HTTP %s: %s", location, status, connection.getResponseMessage()
            ));
        }

        String responseEncoding = connection.getContentEncoding();
        if ("gzip".equalsIgnoreCase(responseEncoding)) {
            return new GZIPInputStream(connection.getInputStream());
        }

        return connection.getInputStream();
    }

    // Resolves a relative path against a base URL safely.
    private static URI resolve(String baseUrl, String path) throws IOException {
        try {
            if (!baseUrl.endsWith("/")) {
                baseUrl += "/";
            }

            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            return new URI(baseUrl).resolve(path);
        } catch (URISyntaxException e) {
            throw new IOException("Invalid repository URL configuration: " + baseUrl + ", " + path, e);
        }
    }
}
