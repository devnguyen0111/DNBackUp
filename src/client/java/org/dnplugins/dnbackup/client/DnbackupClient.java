package org.dnplugins.dnbackup.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.resources.Identifier;
import org.dnplugins.dnbackup.network.BackupProgressPayload;

public class DnbackupClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Register S2C packet receiver
        ClientPlayNetworking.registerGlobalReceiver(BackupProgressPayload.TYPE, (payload, context) -> {
            ClientBackupState.setProgress(
                payload.progress(), 
                payload.status(),
                payload.processedFiles(),
                payload.totalFiles(),
                payload.totalSize()
            );
        });

        // Register HUD rendering element using the new HudElement API
        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath("dnbackup", "progress_hud"),
            (guiGraphicsExtractor, deltaTracker) -> {
                float progress = ClientBackupState.getProgress();
                if (progress == -1.0f) return;

                long elapsed = System.currentTimeMillis() - ClientBackupState.getLastUpdateTime();
                
                // If finished or failed, keep on screen for 3 seconds
                if (progress == 2.0f || progress == -2.0f) {
                    if (elapsed > 3000) {
                        ClientBackupState.setIdle();
                        return;
                    }
                }

                Minecraft client = Minecraft.getInstance();
                if (client.options.hideGui) return;

                // Positioning in the bottom-right corner
                int screenWidth = guiGraphicsExtractor.guiWidth();
                int screenHeight = guiGraphicsExtractor.guiHeight();

                int width = 200;
                int height = 48;
                int x = screenWidth - width - 10;
                int y = screenHeight - height - 10;

                int accentColor;
                int progressColor;
                String title;
                float progressVal = progress;

                if (progress == 2.0f) {
                    title = "Backup Completed";
                    accentColor = 0xFF00FF88; // Emerald green
                    progressColor = 0xFF00FF88;
                    progressVal = 1.0f; // full bar
                } else if (progress == -2.0f) {
                    title = "Backup Failed";
                    accentColor = 0xFFFF3333; // Red
                    progressColor = 0xFFFF3333;
                    progressVal = 0.0f; // empty bar
                } else {
                    title = "Backup in Progress";
                    accentColor = 0xFF00AAFF; // Cyan blue
                    progressColor = 0xFF00AAFF;
                }

                // Draw translucent dark background
                guiGraphicsExtractor.fill(x, y, x + width, y + height, 0xD0121212);

                // Draw left border accent line
                guiGraphicsExtractor.fill(x, y, x + 3, y + height, accentColor);

                // Draw title text (using fully opaque white)
                guiGraphicsExtractor.text(client.font, title, x + 10, y + 5, 0xFFFFFFFF, true);

                // Draw status text (truncated if necessary, using fully opaque light gray)
                String status = ClientBackupState.getStatus();
                if (status.length() > 28) {
                    status = status.substring(0, 25) + "...";
                }
                guiGraphicsExtractor.text(client.font, status, x + 10, y + 16, 0xFFAAAAAA, true);

                // Draw files progress & total size
                int totalFiles = ClientBackupState.getTotalFiles();
                int processedFiles = ClientBackupState.getProcessedFiles();
                long totalSize = ClientBackupState.getTotalSize();

                if (progress != -1.0f && totalFiles > 0) {
                    String sizeStr;
                    if (totalSize < 1024 * 1024) {
                        sizeStr = String.format("%.1f KB", (double) totalSize / 1024);
                    } else {
                        sizeStr = String.format("%.1f MB", (double) totalSize / (1024 * 1024));
                    }
                    String stats = "Files: " + processedFiles + "/" + totalFiles + " (" + sizeStr + ")";
                    guiGraphicsExtractor.text(client.font, stats, x + 10, y + 26, 0xFF888888, true);
                }

                // Draw progress bar if active
                if (progressVal >= 0.0f && progressVal <= 1.0f) {
                    // Background bar
                    guiGraphicsExtractor.fill(x + 10, y + 38, x + width - 10, y + 41, 0xFF333333);

                    // Progress fill
                    int barWidth = width - 20;
                    int fillWidth = (int) (barWidth * progressVal);
                    guiGraphicsExtractor.fill(x + 10, y + 38, x + 10 + fillWidth, y + 41, progressColor);
                }
            }
        );
    }
}
