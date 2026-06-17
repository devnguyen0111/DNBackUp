# Changelog - DNBackUp

## [0.3.1] - 2026-06-15

### 🚀 Added
- **Backup on Shutdown:** Added a configuration option `backupOnShutdown` (default `true`) to automatically back up the world when the server is stopping, gracefully waiting for completion before shutting down.
- **Chat Announcements:** Implemented chat announcements for backup start, success, and failure, controlled by the `silent` config option.
- **Modern Text Formatting:** Migrated all in-game messages from outdated `§` style codes to modern Minecraft `ChatFormatting` styling components to prevent message truncation.

### ⚙️ Changed & Optimized
- **Smart Network Throttling:** Throttled progress packet transmissions to client HUD to update only when the progress percentage changes, every `250ms`, or upon file completion. This prevents networking queue flooding on servers.
- **Performance Tuning:** Increased zip compression buffer size from `8KB` to `64KB`, improving backup speed and CPU efficiency.
- **Tick Optimization:** Cached config checks inside the main scheduler loop to avoid calling config reads 20 times per second.
- **Clean Mod Weight:** Removed dead generator and empty mixin configurations to speed up mod loading times.

### 🐛 Fixed
- **Shutdown Thread Hang Protection:** Added a 5-minute safety timeout to the shutdown hook to ensure the server never hangs indefinitely if a thread error occurs during shutdown.
- **Backup State Thread Safety:** Configured `isBackupRunning` and scheduling variables to be thread-safe (`volatile` / synchronized locks) to avoid race conditions.
- **Autosave Recovery:** Fixed a bug where a failed backup initialization could leave the server with `save-off` (autosave disabled) forever.
- **Thread-safe Configs:** Synchronized configuration load, save, and reload routines to prevent partial reads or writing conflicts.
- **Infinite Scheduled Backup Loop:** Fixed a critical bug where auto-backups loop indefinitely if `timerIntervalMinutes` is set to 0 or negative.
- **Garbage Temp Files:** Fixed a bug where `.tmp` files are left behind on backup failure.
- **File Lock Warnings:** Excluded `session.lock` and other `.lock`/`.lck` files from backups to avoid noisy JVM file locking errors on Windows.

---

## [0.2] - 2026-06-10

### 🚀 Added
- **Asynchronous Backup System:** Automatic and manual backup management for Minecraft worlds without causing in-game lag.
- **Progress HUD:** A sleek real-time rendering HUD showing the backup progress status, positioned perfectly above the game chat box.
- **Enhanced Metadata:** Added professional description, structured entrypoints, and standard `Java >= 25` runtime dependency configuration in `fabric.mod.json`.

### 🐛 Fixed
- **Mod Icon Display:** Fixed a rendering issue where Mod Menu showed a default question mark (`?`) icon instead of the custom mod icon.
  - *Details:* The icon image has been re-encoded from an invalid JPEG format to a true 32-bit PNG file, and optimized to a standard 512x512 resolution.

---

## [0.1] - 2026-06-10
- Initial release of the mod structure and base skeleton.
