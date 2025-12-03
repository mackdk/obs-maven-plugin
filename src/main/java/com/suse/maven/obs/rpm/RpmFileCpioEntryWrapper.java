package com.suse.maven.obs.rpm;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;

class RpmFileCpioEntryWrapper implements RpmFile {

    private final CpioArchiveEntry entry;

    RpmFileCpioEntryWrapper(CpioArchiveEntry entry) {
        this.entry = entry;
    }

    @Override
    public Path getPath() {
        return Paths.get(sanitizeFileName(entry.getName()));
    }

    @Override
    public long getSize() {
        return entry.getSize();
    }

    @Override
    public boolean isDirectory() {
        return entry.isDirectory();
    }

    @Override
    public boolean isRegularFile() {
        return entry.isRegularFile();
    }

    @Override
    public boolean isSymbolicLink() {
        return entry.isSymbolicLink();
    }

    @Override
    public LocalDateTime getLastModified() {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(entry.getTime()), ZoneId.systemDefault());
    }
    
    // Removes leading dot (e.g. "./usr/lib" -> "/usr/lib")
    private static String sanitizeFileName(String rpmPath) {
        if (rpmPath.startsWith(".")) {
            return rpmPath.substring(1);
        }

        return rpmPath;
    }
}
