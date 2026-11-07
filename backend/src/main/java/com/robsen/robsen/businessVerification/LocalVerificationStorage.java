package com.robsen.robsen.businessVerification;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalVerificationStorage implements VerificationStorage {
    private final Path root;
    public LocalVerificationStorage(@Value("${app.documents.directory:./private-documents}") String directory) {
        this.root = Path.of(directory).toAbsolutePath().normalize();
    }
    @Override public String store(InputStream stream) throws IOException {
        Files.createDirectories(root);
        String key = UUID.randomUUID().toString();
        Files.copy(stream, root.resolve(key), StandardCopyOption.REPLACE_EXISTING);
        return key;
    }
    @Override public Path resolve(String key) throws IOException {
        if (key == null || !key.matches("[0-9a-fA-F-]{36}")) throw new IOException("Invalid storage key.");
        Path resolved = root.resolve(key).normalize();
        if (!resolved.startsWith(root) || !Files.isRegularFile(resolved)) throw new IOException("Document not found.");
        return resolved;
    }
}
