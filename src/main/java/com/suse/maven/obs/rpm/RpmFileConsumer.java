package com.suse.maven.obs.rpm;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

@FunctionalInterface
public interface RpmFileConsumer {

    void accept(RpmFile var1, InputStream var2) throws IOException;

    default RpmFileConsumer andThen(RpmFileConsumer after) {
        Objects.requireNonNull(after);
        return (l, r) -> {
            this.accept(l, r);
            after.accept(l, r);
        };
    }
}
