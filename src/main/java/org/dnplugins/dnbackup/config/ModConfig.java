package org.dnplugins.dnbackup.config;

import java.util.ArrayList;
import java.util.List;

public class ModConfig {
    private int maxBackupsToKeep = 10;
    private long maxStorageMb = 5000;
    private int timerIntervalMinutes = 30;
    private int compressionLevel = 5;
    private boolean onlyWhenPlayersOnline = true;
    private List<String> extraFiles = new ArrayList<>();
    private boolean silent = true;
    private boolean backupOnStartup = true;

    // Getters
    public int getMaxBackupsToKeep() {
        return maxBackupsToKeep;
    }

    public long getMaxStorageMb() {
        return maxStorageMb;
    }

    public int getTimerIntervalMinutes() {
        return timerIntervalMinutes;
    }

    public int getCompressionLevel() {
        // Clamp between 0 and 9
        return Math.max(0, Math.min(9, compressionLevel));
    }

    public boolean isOnlyWhenPlayersOnline() {
        return onlyWhenPlayersOnline;
    }

    public List<String> getExtraFiles() {
        return extraFiles == null ? new ArrayList<>() : extraFiles;
    }

    public boolean isSilent() {
        return silent;
    }

    public boolean isBackupOnStartup() {
        return backupOnStartup;
    }

    // Setters (useful for defaults/commands)
    public void setMaxBackupsToKeep(int maxBackupsToKeep) {
        this.maxBackupsToKeep = maxBackupsToKeep;
    }

    public void setMaxStorageMb(long maxStorageMb) {
        this.maxStorageMb = maxStorageMb;
    }

    public void setTimerIntervalMinutes(int timerIntervalMinutes) {
        this.timerIntervalMinutes = timerIntervalMinutes;
    }

    public void setCompressionLevel(int compressionLevel) {
        this.compressionLevel = compressionLevel;
    }

    public void setOnlyWhenPlayersOnline(boolean onlyWhenPlayersOnline) {
        this.onlyWhenPlayersOnline = onlyWhenPlayersOnline;
    }

    public void setExtraFiles(List<String> extraFiles) {
        this.extraFiles = extraFiles;
    }

    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    public void setBackupOnStartup(boolean backupOnStartup) {
        this.backupOnStartup = backupOnStartup;
    }
}
