package org.dnplugins.dnbackup.backup;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.dnplugins.dnbackup.config.ConfigManager;
import org.dnplugins.dnbackup.config.ModConfig;
import org.dnplugins.dnbackup.network.BackupProgressPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BackupManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("DNBackUp");
    private static boolean isBackupRunning = false;
    private static long lastBackupTime = 0;

    public static synchronized boolean isBackupRunning() {
        return isBackupRunning;
    }

    public static long getLastBackupTime() {
        return lastBackupTime;
    }

    public static void startBackup(MinecraftServer server) {
        synchronized (BackupManager.class) {
            if (isBackupRunning) {
                LOGGER.warn("A backup is already running.");
                return;
            }
            isBackupRunning = true;
        }

        ModConfig config = ConfigManager.getConfig();

        // 1. Run save-off and save-all on main thread
        server.execute(() -> {
            try {
                LOGGER.info("Starting backup process. Pausing autosave...");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "save-off");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "save-all");

                // Send initial progress to players
                sendProgressToAll(server, 0.0f, "Preparing backup...", 0, 0, 0L);

                // 2. Start asynchronous zipping thread
                new Thread(() -> runAsyncBackup(server, config), "Backup-Thread").start();

            } catch (Exception e) {
                LOGGER.error("Failed to initialize backup: ", e);
                cleanupFailedBackup(server);
            }
        });
    }

    private static void runAsyncBackup(MinecraftServer server, ModConfig config) {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        Path worldDir = server.getWorldPath(LevelResource.ROOT);
        Path backupsDir = gameDir.resolve("backups");

        try {
            Files.createDirectories(backupsDir);

            // 1. Gather all files to back up
            List<Path> filesToBackup = new ArrayList<>();
            
            // Walk world directory
            try (var stream = Files.walk(worldDir)) {
                stream.filter(Files::isRegularFile)
                      .filter(p -> !p.startsWith(backupsDir)) // Exclude backups folder just in case
                      .forEach(filesToBackup::add);
            }

            // Walk extra files
            List<Path> extraFiles = resolvePaths(gameDir, config.getExtraFiles());
            filesToBackup.addAll(extraFiles);

            // Calculate total size
            long totalBytes = 0;
            for (Path path : filesToBackup) {
                try {
                    totalBytes += Files.size(path);
                } catch (IOException ignored) {}
            }

            int totalFiles = filesToBackup.size();
            sendProgressToAll(server, 0.0f, "Compressing...", 0, totalFiles, totalBytes);

            // 2. Prepare Zip file
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String zipFileName = "backup_" + timestamp + ".zip";
            Path tempZipPath = backupsDir.resolve(zipFileName + ".tmp");
            Path finalZipPath = backupsDir.resolve(zipFileName);

            long bytesWritten = 0;
            byte[] buffer = new byte[8192];
            int processedFiles = 0;

            long lastSentTime = 0;
            int lastSentPercent = -1;

            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempZipPath.toFile()))) {
                zos.setLevel(config.getCompressionLevel());

                for (Path file : filesToBackup) {
                    if (Files.isDirectory(file)) continue;

                    // Determine relative zip entry name
                    String entryName;
                    if (file.startsWith(worldDir)) {
                        // Keep world directory name in zip (e.g. world/level.dat)
                        entryName = worldDir.getParent().relativize(file).toString().replace('\\', '/');
                    } else {
                        // Relativize against game directory for extra files
                        entryName = gameDir.relativize(file).toString().replace('\\', '/');
                    }

                    try {
                        ZipEntry zipEntry = new ZipEntry(entryName);
                        zos.putNextEntry(zipEntry);

                        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file.toFile()))) {
                            int bytesRead;
                            while ((bytesRead = bis.read(buffer)) != -1) {
                                zos.write(buffer, 0, bytesRead);
                                bytesWritten += bytesRead;

                                // Send progress updates
                                float progress = totalBytes > 0 ? (float) bytesWritten / totalBytes : 0f;
                                // Clamp progress below 1.0f during active zipping
                                progress = Math.max(0f, Math.min(0.99f, progress));
                                
                                int percent = (int) (progress * 100);
                                long now = System.currentTimeMillis();
                                if (percent != lastSentPercent || now - lastSentTime > 100) {
                                    sendProgressToAll(server, progress, "Compressing...", processedFiles, totalFiles, totalBytes);
                                    lastSentPercent = percent;
                                    lastSentTime = now;
                                }
                            }
                        }
                        zos.closeEntry();
                    } catch (IOException e) {
                        LOGGER.warn("Failed to back up file: " + file + ", skipping. Error: " + e.getMessage());
                    }
                    processedFiles++;

                    // Send an update after zipping each file to ensure file count stays accurate
                    float progress = totalBytes > 0 ? (float) bytesWritten / totalBytes : 0f;
                    progress = Math.max(0f, Math.min(0.99f, progress));
                    sendProgressToAll(server, progress, "Compressing...", processedFiles, totalFiles, totalBytes);
                    lastSentPercent = (int) (progress * 100);
                    lastSentTime = System.currentTimeMillis();
                }
            }

            // Rename temp file to final zip
            Files.move(tempZipPath, finalZipPath, StandardCopyOption.REPLACE_EXISTING);
            lastBackupTime = System.currentTimeMillis();
            LOGGER.info("Backup successfully saved as: {}", zipFileName);

            // Send success packet (progress = 2.0f triggers client success HUD)
            sendProgressToAll(server, 2.0f, "Backup completed successfully!", totalFiles, totalFiles, totalBytes);

            // Run cleanups on the files
            runCleanup(backupsDir, config);

        } catch (Exception e) {
            LOGGER.error("Error during asynchronous backup: ", e);
            sendProgressToAll(server, -2.0f, "Backup failed: " + e.getMessage(), 0, 0, 0L);
        } finally {
            // Re-enable autosave on server thread
            server.execute(() -> {
                LOGGER.info("Resuming autosave...");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "save-on");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "save-all");
                synchronized (BackupManager.class) {
                    isBackupRunning = false;
                }
            });
        }
    }

    private static void runCleanup(Path backupsDir, ModConfig config) {
        File[] files = backupsDir.toFile().listFiles((dir, name) -> name.startsWith("backup_") && name.endsWith(".zip"));
        if (files == null) return;

        // Sort oldest first
        Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        List<File> backupsList = new ArrayList<>(Arrays.asList(files));

        // 1. Keep count constraint
        int maxKeep = config.getMaxBackupsToKeep();
        if (maxKeep > 0) {
            while (backupsList.size() > maxKeep) {
                File oldest = backupsList.remove(0);
                LOGGER.info("Deleting old backup (count limit): {}", oldest.getName());
                if (!oldest.delete()) {
                    LOGGER.warn("Failed to delete backup file: {}", oldest.getName());
                }
            }
        }

        // 2. Storage size constraint
        long maxStorageBytes = config.getMaxStorageMb() * 1024L * 1024L;
        if (maxStorageBytes > 0) {
            long totalSizeBytes = 0;
            for (File file : backupsList) {
                totalSizeBytes += file.length();
            }

            while (totalSizeBytes > maxStorageBytes && !backupsList.isEmpty()) {
                File oldest = backupsList.remove(0);
                long size = oldest.length();
                LOGGER.info("Deleting old backup (size limit {} MB exceeded): {}", config.getMaxStorageMb(), oldest.getName());
                if (oldest.delete()) {
                    totalSizeBytes -= size;
                } else {
                    LOGGER.warn("Failed to delete backup file: {}", oldest.getName());
                    // break loop on fail to avoid infinite loop
                    break;
                }
            }
        }
    }

    private static List<Path> resolvePaths(Path rootDir, List<String> relativePaths) {
        List<Path> resolved = new ArrayList<>();
        if (relativePaths == null) return resolved;

        for (String rel : relativePaths) {
            if (rel == null || rel.trim().isEmpty()) continue;
            Path p = rootDir.resolve(rel.trim()).normalize();
            if (Files.exists(p)) {
                if (Files.isDirectory(p)) {
                    try (var stream = Files.walk(p)) {
                        stream.filter(Files::isRegularFile).forEach(resolved::add);
                    } catch (IOException e) {
                        LOGGER.error("Failed to walk directory: " + p, e);
                    }
                } else {
                    resolved.add(p);
                }
            } else {
                LOGGER.warn("Configured extra file path does not exist: {}", rel);
            }
        }
        return resolved;
    }

    private static void cleanupFailedBackup(MinecraftServer server) {
        synchronized (BackupManager.class) {
            isBackupRunning = false;
        }
        server.execute(() -> {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "save-on");
        });
    }

    private static void sendProgressToAll(MinecraftServer server, float progress, String status, int processedFiles, int totalFiles, long totalSize) {
        BackupProgressPayload payload = new BackupProgressPayload(progress, status, processedFiles, totalFiles, totalSize);
        server.execute(() -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (ServerPlayNetworking.canSend(player, BackupProgressPayload.TYPE)) {
                    ServerPlayNetworking.send(player, payload);
                }
            }
        });
    }
}
