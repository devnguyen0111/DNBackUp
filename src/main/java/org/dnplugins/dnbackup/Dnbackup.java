package org.dnplugins.dnbackup;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.dnplugins.dnbackup.backup.BackupManager;
import org.dnplugins.dnbackup.config.ConfigManager;
import org.dnplugins.dnbackup.config.ModConfig;
import org.dnplugins.dnbackup.network.BackupProgressPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Dnbackup implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("DNBackUp");
    private static volatile long nextBackupTime = 0;
    private static volatile int cachedTimerIntervalMinutes = 30;

    public static void updateNextBackupTime() {
        int interval = ConfigManager.getConfig().getTimerIntervalMinutes();
        cachedTimerIntervalMinutes = interval;
        if (interval <= 0) {
            nextBackupTime = Long.MAX_VALUE;
            return;
        }
        nextBackupTime = System.currentTimeMillis() + (interval * 60L * 1000L);
        LOGGER.info("Next backup scheduled in {} minutes.", interval);
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing DNBackUp mod...");

        // Load configuration
        ConfigManager.load();

        // Register custom networking payload (S2C)
        PayloadTypeRegistry.clientboundPlay().register(BackupProgressPayload.TYPE, BackupProgressPayload.CODEC);

        // Register server lifecycle events
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            updateNextBackupTime();
            if (ConfigManager.getConfig().isBackupOnStartup()) {
                LOGGER.info("Starting auto-backup on world join/server startup...");
                BackupManager.startBackup(server);
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            ModConfig config = ConfigManager.getConfig();
            if (config.isBackupOnShutdown()) {
                LOGGER.info("Server is stopping. Triggering backup before shutdown...");
                if (!BackupManager.isBackupRunning()) {
                    BackupManager.startBackup(server);
                }
                long deadline = System.currentTimeMillis() + 5 * 60 * 1000L; // 5 minutes max timeout
                while (BackupManager.isBackupRunning() && System.currentTimeMillis() < deadline) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                if (BackupManager.isBackupRunning()) {
                    LOGGER.error("Backup timed out during shutdown! Proceeding to prevent server hang.");
                } else {
                    LOGGER.info("Backup completed. Proceeding with shutdown.");
                }
            } else if (BackupManager.isBackupRunning()) {
                LOGGER.warn("A backup is currently running! Waiting for it to complete to prevent corruption before shutting down...");
                long deadline = System.currentTimeMillis() + 5 * 60 * 1000L; // 5 minutes max timeout
                while (BackupManager.isBackupRunning() && System.currentTimeMillis() < deadline) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                if (BackupManager.isBackupRunning()) {
                    LOGGER.error("Running backup did not complete in time during shutdown. Proceeding with shutdown.");
                }
            }
        });

        // Register tick event for scheduled backups
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int interval = cachedTimerIntervalMinutes;
            if (interval <= 0) {
                return;
            }

            long now = System.currentTimeMillis();
            if (nextBackupTime == 0) {
                updateNextBackupTime();
            }

            if (now >= nextBackupTime) {
                // Instantly update timer to prevent double-firing
                updateNextBackupTime();

                boolean playersOnline = server.getPlayerCount() > 0;
                if (!ConfigManager.getConfig().isOnlyWhenPlayersOnline() || playersOnline) {
                    LOGGER.info("Starting scheduled backup...");
                    BackupManager.startBackup(server);
                } else {
                    LOGGER.info("Skipping scheduled backup: No players online.");
                }
            }
        });

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("dnbackup")
                .requires(Commands.hasPermission(Commands.LEVEL_OWNERS))
                .then(Commands.literal("start")
                    .executes(context -> {
                        if (BackupManager.isBackupRunning()) {
                            context.getSource().sendFailure(Component.literal("Backup is already running!").withStyle(ChatFormatting.RED));
                            return 0;
                        }
                        context.getSource().sendSuccess(() -> Component.literal("Starting manual backup...").withStyle(ChatFormatting.GREEN), true);
                        BackupManager.startBackup(context.getSource().getServer());
                        return 1;
                    }))
                .then(Commands.literal("reload")
                    .executes(context -> {
                        ConfigManager.reload();
                        updateNextBackupTime();
                        context.getSource().sendSuccess(() -> Component.literal("DNBackUp configuration reloaded!").withStyle(ChatFormatting.GREEN), true);
                        return 1;
                    }))
                .then(Commands.literal("status")
                    .executes(context -> {
                        ModConfig config = ConfigManager.getConfig();
                        boolean running = BackupManager.isBackupRunning();
                        long last = BackupManager.getLastBackupTime();
                        String lastStr = last == 0 ? "Never" : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(last));
                        
                        context.getSource().sendSuccess(() -> Component.literal("=== DNBackUp Status ===").withStyle(ChatFormatting.YELLOW), false);
                        context.getSource().sendSuccess(() -> Component.literal("Backup Running: ").withStyle(ChatFormatting.WHITE)
                            .append(Component.literal(running ? "Yes" : "No").withStyle(running ? ChatFormatting.GREEN : ChatFormatting.RED)), false);
                        context.getSource().sendSuccess(() -> Component.literal("Last Backup: ").withStyle(ChatFormatting.WHITE)
                            .append(Component.literal(lastStr).withStyle(ChatFormatting.AQUA)), false);
                        context.getSource().sendSuccess(() -> Component.literal("Interval: ").withStyle(ChatFormatting.WHITE)
                            .append(Component.literal(config.getTimerIntervalMinutes() + " minutes").withStyle(ChatFormatting.AQUA)), false);
                        context.getSource().sendSuccess(() -> Component.literal("Max Backups to Keep: ").withStyle(ChatFormatting.WHITE)
                            .append(Component.literal(String.valueOf(config.getMaxBackupsToKeep())).withStyle(ChatFormatting.AQUA)), false);
                        context.getSource().sendSuccess(() -> Component.literal("Max Storage: ").withStyle(ChatFormatting.WHITE)
                            .append(Component.literal(config.getMaxStorageMb() + " MB").withStyle(ChatFormatting.AQUA)), false);
                        context.getSource().sendSuccess(() -> Component.literal("Zip Compression Level: ").withStyle(ChatFormatting.WHITE)
                            .append(Component.literal(String.valueOf(config.getCompressionLevel())).withStyle(ChatFormatting.AQUA)), false);
                        context.getSource().sendSuccess(() -> Component.literal("Only When Players Online: ").withStyle(ChatFormatting.WHITE)
                            .append(Component.literal(String.valueOf(config.isOnlyWhenPlayersOnline())).withStyle(ChatFormatting.AQUA)), false);
                        context.getSource().sendSuccess(() -> Component.literal("Backup on Startup: ").withStyle(ChatFormatting.WHITE)
                            .append(Component.literal(config.isBackupOnStartup() ? "Yes" : "No").withStyle(config.isBackupOnStartup() ? ChatFormatting.GREEN : ChatFormatting.RED)), false);
                        context.getSource().sendSuccess(() -> Component.literal("Backup on Shutdown: ").withStyle(ChatFormatting.WHITE)
                            .append(Component.literal(config.isBackupOnShutdown() ? "Yes" : "No").withStyle(config.isBackupOnShutdown() ? ChatFormatting.GREEN : ChatFormatting.RED)), false);
                        context.getSource().sendSuccess(() -> Component.literal("Silent (No chat announcements): ").withStyle(ChatFormatting.WHITE)
                            .append(Component.literal(config.isSilent() ? "Yes" : "No").withStyle(config.isSilent() ? ChatFormatting.GREEN : ChatFormatting.RED)), false);
                        context.getSource().sendSuccess(() -> Component.literal("Extra Files to Backup: ").withStyle(ChatFormatting.WHITE)
                            .append(Component.literal(config.getExtraFiles().size() + " items").withStyle(ChatFormatting.AQUA)), false);
                        return 1;
                    }))
            );
        });
    }
}
