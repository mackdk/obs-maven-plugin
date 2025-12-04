package com.suse.maven.obs.rpm;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CloseShieldInputStream;

/**
 * This class extract a specified artifact from an RPM package
 */
public class RpmPackage {

    private static final int RPM_LEAD_SIZE = 96;

    private static final byte[] RPM_LEAD_MAGIC = { (byte) 0xED, (byte) 0xAB, (byte) 0xEE, (byte) 0xDB };

    private static final byte[] RPM_HEADER_MAGIC = { (byte) 0x8E, (byte) 0xAD, (byte) 0xE8, (byte) 1, 0, 0, 0, 0 };

    private static final short RPM_TYPE_SOURCE = 1;

    private final Path archivePath;

    private Predicate<RpmFile> filter;

    public RpmPackage(Path archivePath) {
        this.archivePath = archivePath;
        this.filter = v -> true;
    }

    public RpmPackage filter(Predicate<RpmFile> predicate) {
        this.filter = predicate;
        return this;
    }

    public void forEach(RpmFileConsumer consumer) throws IOException {
        try (DataInputStream rpmStream = new DataInputStream(
                new BufferedInputStream(Files.newInputStream(archivePath)))) {
            verifyLead(rpmStream);

            processSignature(rpmStream);
            processHeader(rpmStream);

            processPayload(rpmStream, consumer);
        }
    }

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
            throw new IOException("Invalid RPM file: Source RPMS are not supported");
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

        // Simple sanity check
        if (!Arrays.equals(magic, RPM_HEADER_MAGIC)) {
            throw new IOException("Invalid RPM header magic bytes");
        }

        int indexCount = rpmStream.readInt();
        int dataSize = rpmStream.readInt();

        // Calculate total size of the header data: Each index entry is 16 bytes
        long totalHeaderSize = (16L * indexCount) + dataSize;

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
}
