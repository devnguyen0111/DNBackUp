package org.dnplugins.dnbackup.client;

public class ClientBackupState {
    private static float progress = -1.0f;
    private static String status = "";
    private static int processedFiles = 0;
    private static int totalFiles = 0;
    private static long totalSize = 0L;
    private static long lastUpdateTime = 0;

    public static synchronized void setProgress(float newProgress, String newStatus, int newProcessedFiles, int newTotalFiles, long newTotalSize) {
        progress = newProgress;
        status = newStatus;
        processedFiles = newProcessedFiles;
        totalFiles = newTotalFiles;
        totalSize = newTotalSize;
        lastUpdateTime = System.currentTimeMillis();
    }

    public static synchronized float getProgress() {
        return progress;
    }

    public static synchronized String getStatus() {
        return status;
    }

    public static synchronized int getProcessedFiles() {
        return processedFiles;
    }

    public static synchronized int getTotalFiles() {
        return totalFiles;
    }

    public static synchronized long getTotalSize() {
        return totalSize;
    }

    public static synchronized long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public static synchronized void setIdle() {
        progress = -1.0f;
        status = "";
        processedFiles = 0;
        totalFiles = 0;
        totalSize = 0L;
    }
}
