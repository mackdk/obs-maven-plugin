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
package com.suse.maven.obs.rpm;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Represents an RPM package file and provides methods to traverse its contents.
 * @see <a href="https://rpm-software-management.github.io/rpm/manual/format_v4.html">V4 Package format</a>
 */
public class RpmPackage {

    private static final int RPM_LEAD_SIZE = 96;

    private static final byte[] RPM_LEAD_MAGIC = { (byte) 0xED, (byte) 0xAB, (byte) 0xEE, (byte) 0xDB };

    private static final byte[] RPM_HEADER_MAGIC = { (byte) 0x8E, (byte) 0xAD, (byte) 0xE8, (byte) 1, 0, 0, 0, 0 };

    private static final short RPM_TYPE_SOURCE = 1;

    private static final int MAX_HEADER_INDEX_COUNT = 100_000;

    private static final int MAX_HEADER_DATA_SIZE = 64 * 1024 * 1024; // 64 MB

    private static final long MAX_TOTAL_HEADER_SIZE = 128L * 1024 * 1024; // 128 MB

    private final Path archivePath;

    private Predicate<RpmFile> filter;

    /**
     * Creates a new RpmPackage instance for the specified file.
     * @param archivePath the path to the RPM file.
     */
    public RpmPackage(@NotNull Path archivePath) {
        this.archivePath = Objects.requireNonNull(archivePath);
        this.filter = v -> true;
    }

    /**
     * Applies a filter to this package.
     * <p>
     * Subsequent calls to {@link #forEach(RpmFileConsumer)} or {@link #list()} will
     * only process entries that match the given predicate.
     * @param predicate the predicate to apply to each entry.
     * @return this instance (fluent API).
     */
    @NotNull
    public RpmPackage filter(@NotNull Predicate<RpmFile> predicate) {
        this.filter = Objects.requireNonNull(predicate);
        return this;
    }

    /**
     * Iterates over the entries in the RPM package, executing the provided consumer
     * for each entry that matches the current filter.
     * @param consumer the action to perform on each matching entry.
     * @throws IOException if the RPM file is invalid, unsupported, or if an I/O error occurs.
     */
    public void forEach(@NotNull RpmFileConsumer consumer) throws IOException {
        try (DataInputStream rpmStream = new DataInputStream(
            new BufferedInputStream(Files.newInputStream(archivePath)))) {
            verifyLead(rpmStream);

            processSignature(rpmStream);
            processHeader(rpmStream);

            processPayload(rpmStream, Objects.requireNonNull(consumer));
        }
    }

    /**
     * Returns a list of all {@link RpmFile} entries in the package that match the current filter.
     * @return a list of matching file entries.
     * @throws IOException if an error occurs reading the RPM.
     */
    @NotNull
    public List<RpmFile> list() throws IOException {
        List<RpmFile> fileList = new ArrayList<>();

        this.forEach((file, inputStream) -> fileList.add(file));
        return fileList;
    }

    private void processPayload(InputStream rpmStream, RpmFileConsumer consumer) throws IOException {
        try (InputStream compressedStream = new CompressorStreamFactory().createCompressorInputStream(rpmStream);
             CpioArchiveInputStream cpioStream = new CpioArchiveInputStream(compressedStream)) {

            CpioArchiveEntry entry = cpioStream.getNextEntry();
            while (entry != null) {
                RpmFile file = new RpmFileCpioEntryWrapper(entry);

                if (filter.test(file)) {
                    consumer.accept(file, CloseShieldInputStream.wrap(cpioStream));
                }

                entry = cpioStream.getNextEntry();
            }
        }
    }

    private static void verifyLead(DataInputStream rpmStream) throws IOException {
        // Check if the file is actually an RPM
        byte[] magic = new byte[4];
        rpmStream.readFully(magic);
        if (!Arrays.equals(magic, RPM_LEAD_MAGIC)) {
            throw new IOException("Invalid RPM file: unexpected byte sequence");
        }

        // Verify it's a supported version
        int major = rpmStream.readUnsignedByte();
        if (major > 4) {
            throw new IOException("Invalid RPM file: unsupported version " + major);
        }

        // Skip minor version
        rpmStream.readByte();

        // Exclude source RPM
        short type = rpmStream.readShort();
        if (type == RPM_TYPE_SOURCE) {
            throw new IOException("Invalid RPM file: Source RPMs are not supported");
        }

        // Safely skip the reset of the lead
        rpmStream.readFully(new byte[RPM_LEAD_SIZE - 8]);
    }

    private static void processSignature(DataInputStream rpmStream) throws IOException {
        processDataStructure(rpmStream, true);
    }

    private static void processHeader(DataInputStream rpmStream) throws IOException {
        processDataStructure(rpmStream, false);
    }

    private static void processDataStructure(DataInputStream rpmStream, boolean alignTo8Bytes) throws IOException {
        // Header Intro is 16 bytes
        byte[] magic = new byte[8];
        rpmStream.readFully(magic);

        // Ensure the magic number matches
        if (!Arrays.equals(magic, RPM_HEADER_MAGIC)) {
            throw new IOException("Invalid RPM header magic bytes");
        }

        int indexCount = rpmStream.readInt();
        // Simple sanity check on the number of indexes
        if (indexCount < 0 || indexCount > MAX_HEADER_INDEX_COUNT) {
            throw new IOException("Invalid RPM header: unreasonable index count " + indexCount);
        }

        int dataSize = rpmStream.readInt();
        // Simple safeguard on the data size of the header
        if (dataSize < 0 || dataSize > MAX_HEADER_DATA_SIZE) {
            throw new IOException("Invalid RPM header: unreasonable data size " + dataSize);
        }

        // Calculate total size of the header data: Each index entry is 16 bytes
        long totalHeaderSize = getTotalHeaderSize(indexCount, dataSize);

        // Skip the data
        IOUtils.skipFully(rpmStream, totalHeaderSize);

        // Check if the data structure is padded to 8-bytes
        if (alignTo8Bytes) {
            long totalRead = 16 + totalHeaderSize;
            long remainder = totalRead % 8;
            if (remainder != 0) {
                IOUtils.skipFully(rpmStream, 8 - remainder);
            }
        }
    }

    private static long getTotalHeaderSize(int indexCount, int dataSize) throws IOException {
        long totalHeaderSize;

        try {
            // Use Math.addExact to be overflow-safe
            totalHeaderSize = Math.addExact(16L * indexCount, dataSize);
            // Defensive upper bound in case individual limits change in the future
            if (totalHeaderSize > MAX_TOTAL_HEADER_SIZE) {
                throw new IOException("Invalid RPM header: total header size too large (" + totalHeaderSize + ")");
            }
        } catch (ArithmeticException ex) {
            throw new IOException("Invalid RPM header: size overflow", ex);
        }
        return totalHeaderSize;
    }
}
