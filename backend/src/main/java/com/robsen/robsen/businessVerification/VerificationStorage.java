package com.robsen.robsen.businessVerification;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public interface VerificationStorage {
    String store(InputStream stream) throws IOException;
    Path resolve(String key) throws IOException;
}
