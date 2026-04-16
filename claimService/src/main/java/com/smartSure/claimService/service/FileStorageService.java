package com.smartSure.claimService.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Handles file storage to local filesystem.
 * Files are stored at: ${claim.file.storage.path}/{claimId}/{type}_{uuid}_{originalName}
 *
 * For production: replace with S3/MinIO implementation by changing
 * only this service — ClaimService stays untouched.
 */
@Slf4j
@Service
public class FileStorageService {

    @Value("${claim.file.storage.path:./claim-files}")
    private String storagePath;

    /**
     * Saves a file to disk and returns the full file path.
     * Path format: {storagePath}/{claimId}/{fileType}_{uuid}_{originalName}
     */
    public String saveFile(MultipartFile file, Long claimId,
                           String fileType) throws IOException {

        // Create directory for this claim if it doesn't exist
        Path claimDir = Paths.get(storagePath, String.valueOf(claimId));
        Files.createDirectories(claimDir);

        // Generate unique filename to prevent overwrites
        String uniqueFileName = fileType + "_"
                + UUID.randomUUID().toString().substring(0, 8)
                + "_" + file.getOriginalFilename();

        Path targetPath = claimDir.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), targetPath,
                StandardCopyOption.REPLACE_EXISTING);

        log.info("File saved — claimId={}, type={}, path={}",
                claimId, fileType, targetPath);

        return targetPath.toString();
    }

    /**
     * Reads file bytes from disk given the stored file path.
     */
    public byte[] readFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("File not found at path: " + filePath);
        }
        return Files.readAllBytes(path);
    }

    /**
     * Deletes all files for a claim (called when claim is deleted).
     */
    public void deleteClaimFiles(Long claimId) {
        try {
            Path claimDir = Paths.get(storagePath, String.valueOf(claimId));
            if (Files.exists(claimDir)) {
                Files.walk(claimDir)
                        .sorted(java.util.Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException e) {
                                log.warn("Could not delete file: {}", p);
                            }
                        });
                log.info("Deleted all files for claimId={}", claimId);
            }
        } catch (IOException e) {
            log.error("Failed to delete files for claimId={}: {}",
                    claimId, e.getMessage());
        }
    }
}