package com.suse.maven.obs.rpm;

import java.nio.file.Path;
import java.time.LocalDateTime;

public interface RpmFile {

    Path getPath();

    long getSize();

    boolean isDirectory();

    boolean isRegularFile();

    boolean isSymbolicLink();

    LocalDateTime getLastModified();
}
