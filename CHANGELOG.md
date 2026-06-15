# Changelog - DNBackUp

## [0.3] - 2026-06-15

### 🚀 Added
- **Backup on Shutdown:** Added a configuration option `backupOnShutdown` (default `true`) to automatically back up the world when the server is stopping, gracefully waiting for completion before shutting down.
- **Chat Announcements:** Implemented chat announcements for backup start, success, and failure, controlled by the `silent` config option.

### 🐛 Fixed
- **Infinite Scheduled Backup Loop:** Fixed a critical bug where auto-backups loop indefinitely if `timerIntervalMinutes` is set to 0 or negative.
- **Garbage Temp Files:** Fixed a bug where `.tmp` files are left behind on backup failure.
- **File Lock Warnings:** Excluded `session.lock` and other `.lock`/`.lck` files from backups to avoid noisy JVM file locking errors on Windows.

### ⚙️ Changed & Optimized
- **Reduced Networking Overhead:** Throttled progress packet transmissions to update every 1MB of compressed bytes instead of 8KB, drastically reducing server queue overhead.
- **Thread Safety:** Marked `lastBackupTime` as `volatile` to prevent visibility issues across threads.
- **Robustness:** Added null-safety checks when obtaining the parent directory of the world.

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
