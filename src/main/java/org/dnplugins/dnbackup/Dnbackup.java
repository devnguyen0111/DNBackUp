package org.dnplugins.dnbackup;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
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
    private static long nextBackupTime = 0;

    public static void updateNextBackupTime() {
        int interval = ConfigManager.getConfig().getTimerIntervalMinutes();
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
                while (BackupManager.isBackupRunning()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                LOGGER.info("Backup completed. Proceeding with shutdown.");
            } else if (BackupManager.isBackupRunning()) {
                LOGGER.warn("A backup is currently running! Waiting for it to complete to prevent corruption before shutting down...");
                while (BackupManager.isBackupRunning()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });

        // Register tick event for scheduled backups
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int interval = ConfigManager.getConfig().getTimerIntervalMinutes();
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
                            context.getSource().sendFailure(Component.literal("§cBackup is already running!"));
                            return 0;
                        }
                        context.getSource().sendSuccess(() -> Component.literal("§aStarting manual backup..."), true);
                        BackupManager.startBackup(context.getSource().getServer());
                        return 1;
                    }))
                .then(Commands.literal("reload")
                    .executes(context -> {
                        ConfigManager.reload();
                        updateNextBackupTime();
                        context.getSource().sendSuccess(() -> Component.literal("§aDNBackUp configuration reloaded!"), true);
                        return 1;
                    }))
                .then(Commands.literal("status")
                    .executes(context -> {
                        ModConfig config = ConfigManager.getConfig();
                        boolean running = BackupManager.isBackupRunning();
                        long last = BackupManager.getLastBackupTime();
                        String lastStr = last == 0 ? "Never" : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(last));
                        
                        context.getSource().sendSuccess(() -> Component.literal("§e=== DNBackUp Status ==="), false);
                        context.getSource().sendSuccess(() -> Component.literal("§fBackup Running: " + (running ? "§aYes" : "§cNo")), false);
                        context.getSource().sendSuccess(() -> Component.literal("§fLast Backup: §b" + lastStr), false);
                        context.getSource().sendSuccess(() -> Component.literal("§fInterval: §b" + config.getTimerIntervalMinutes() + " minutes"), false);
                        context.getSource().sendSuccess(() -> Component.literal("§fMax Backups to Keep: §b" + config.getMaxBackupsToKeep()), false);
                        context.getSource().sendSuccess(() -> Component.literal("§fMax Storage: §b" + config.getMaxStorageMb() + " MB"), false);
                        context.getSource().sendSuccess(() -> Component.literal("§fZip Compression Level: §b" + config.getCompressionLevel()), false);
                        context.getSource().sendSuccess(() -> Component.literal("§fOnly When Players Online: §b" + config.isOnlyWhenPlayersOnline()), false);
                        context.getSource().sendSuccess(() -> Component.literal("§fBackup on Startup: " + (config.isBackupOnStartup() ? "§aYes" : "§cNo")), false);
                        context.getSource().sendSuccess(() -> Component.literal("§fBackup on Shutdown: " + (config.isBackupOnShutdown() ? "§aYes" : "§cNo")), false);
                        context.getSource().sendSuccess(() -> Component.literal("§fSilent (No chat announcements): " + (config.isSilent() ? "§aYes" : "§cNo")), false);
                        context.getSource().sendSuccess(() -> Component.literal("§fExtra Files to Backup: §b" + config.getExtraFiles().size() + " items"), false);
                        return 1;
                    }))
            );
        });
    }
}
