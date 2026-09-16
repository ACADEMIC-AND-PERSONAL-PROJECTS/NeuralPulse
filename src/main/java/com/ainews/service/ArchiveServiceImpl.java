package com.ainews.service;

import com.ainews.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ArchiveServiceImpl implements ArchiveService {

    private static final Logger log = LoggerFactory.getLogger(ArchiveServiceImpl.class);

    private final AppConfig appConfig;

    public ArchiveServiceImpl(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @Override
    public String archiveWeeklyDigest(String content, String weekDate) {
        String filename = "week-" + weekDate + ".md";
        Path archiveDir = Paths.get(appConfig.getArchiveDir());
        Path filePath = archiveDir.resolve(filename);

        try {
            Files.createDirectories(archiveDir);
            Files.writeString(filePath, content);
            log.info("Archived digest to: {}", filePath.toAbsolutePath());
            return filePath.toString();
        } catch (IOException e) {
            log.error("Failed to archive digest: {}", e.getMessage());
            return null;
        }
    }
}
