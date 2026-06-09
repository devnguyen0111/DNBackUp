# DNBackUp - Minecraft Fabric Mod Backup

**DNBackUp** là một mod Fabric dành cho Minecraft (hỗ trợ phiên bản 1.21.x và Java 25) giúp tự động hoặc thủ công sao lưu (backup) thế giới (world) game của bạn thành file nén `.zip`. Mod này cực kỳ hữu ích cho các máy chủ hardcore hoặc thế giới chơi đơn, giúp tránh mất mát dữ liệu do các lỗi hệ thống, crash hoặc hỏng file world.

---

## ✨ Các Tính Năng Nổi Bật

- **💾 Sao lưu an toàn & Không đồng bộ (Async Backup):**
  - Tạm thời vô hiệu hóa tính năng tự động lưu (`save-off`) và thực hiện lưu toàn bộ (`save-all`) trước khi sao lưu để đảm bảo tính nhất quán dữ liệu.
  - Nén file zip trên một luồng phụ (background thread) riêng biệt để tránh gây giật lag (lag spike) hoặc đứng máy chủ khi thế giới có dung lượng lớn.
  - Tự động bật lại tính năng tự động lưu (`save-on`) sau khi hoàn thành.
- **🕒 Tự động sao lưu định kỳ:** Tự động sao lưu theo thời gian thiết lập trước (tính bằng phút). Hỗ trợ tuỳ chọn chỉ sao lưu khi có người chơi trực tuyến để tiết kiệm tài nguyên.
- **🖥️ Thanh tiến trình trực quan trên HUD:**
  - Gửi gói tin mạng tùy chỉnh từ server tới client (`BackupProgressPayload`) để hiển thị một thanh tiến trình HUD mượt mà ở góc dưới bên phải màn hình.
  - Hiển thị trực quan trạng thái: **Đang nén (Compressing - màu Xanh dương)**, **Hoàn thành (Completed - màu Xanh lá)**, hoặc **Thất bại (Failed - màu Đỏ)**.
- **⚙️ Quản lý dung lượng & Số lượng sao lưu (Retention Limits):**
  - Giới hạn số lượng file backup tối đa giữ lại.
  - Giới hạn tổng dung lượng lưu trữ tối đa (MB) của các bản sao lưu.
  - Tự động quét và xóa các file backup cũ nhất khi vượt quá giới hạn.
- **📂 Sao lưu tệp bổ sung (Extra Files):** Cho phép cấu hình sao lưu thêm các file hoặc thư mục bên ngoài thư mục world (như file cấu hình, log server, mod khác...).
- **🤐 Chế độ Yên lặng (Silent Mode):** Cho phép tắt các thông báo tin nhắn trong khung chat của server.

---

## 🛠️ Lệnh Hệ Thống (Commands)

Các lệnh dưới đây yêu cầu quyền quản trị viên (OP hoặc Permission Level 4) trên server:

| Lệnh | Mô tả |
| :--- | :--- |
| `/dnbackup start` | Khởi chạy tiến trình sao lưu thủ công ngay lập tức. |
| `/dnbackup reload` | Tải lại cấu hình từ file `config/dnbackup.json`. |
| `/dnbackup status` | Xem trạng thái hoạt động hiện tại (Trạng thái nén, thời gian sao lưu gần nhất, cấu hình hiện tại...). |

---

## ⚙️ Cấu Hình (Configuration)

File cấu hình được tự động tạo lần đầu tại đường dẫn: `config/dnbackup.json` sau khi khởi chạy game/server có chứa mod.

### Các thuộc tính cấu hình chi tiết:

| Thuộc tính | Kiểu dữ liệu | Giá trị mặc định | Mô tả |
| :--- | :---: | :---: | :--- |
| `maxBackupsToKeep` | `int` | `10` | Số lượng bản sao lưu tối đa được lưu giữ. Đặt `0` để vô hiệu hóa giới hạn này. |
| `maxStorageMb` | `long` | `5000` | Tổng dung lượng tối đa (MB) của thư mục `backups/`. Đặt `0` để vô hiệu hóa giới hạn này. |
| `timerIntervalMinutes` | `int` | `30` | Chu kỳ tự động sao lưu định kỳ (tính bằng phút). |
| `compressionLevel` | `int` | `5` | Cấp độ nén của tệp zip (`0` đến `9`, với `0` là không nén và `9` là nén tối đa). |
| `onlyWhenPlayersOnline` | `boolean` | `true` | Nếu đặt là `true`, mod sẽ chỉ tự động sao lưu khi có ít nhất một người chơi online. |
| `backupOnStartup` | `boolean` | `true` | Nếu đặt là `true`, mod sẽ tự động sao lưu ngay khi máy chủ/thế giới được khởi chạy. |
| `silent` | `boolean` | `true` | Nếu đặt là `true`, tắt các thông báo chat hệ thống (tiến trình vẫn hiển thị qua HUD của client). |
| `extraFiles` | `List<String>` | `[]` | Danh sách đường dẫn tương đối (tính từ thư mục gốc của game) trỏ tới các file hoặc thư mục khác muốn sao lưu kèm theo world. |

*Ví dụ về file `config/dnbackup.json`:*
```json
{
  "maxBackupsToKeep": 10,
  "maxStorageMb": 5000,
  "timerIntervalMinutes": 30,
  "compressionLevel": 5,
  "onlyWhenPlayersOnline": true,
  "backupOnStartup": true,
  "extraFiles": [
    "config/important_mod_config.json",
    "server.properties"
  ],
  "silent": true
}
```

---

## 🚀 Hướng Dẫn Cài Đặt & Biên Dịch (Build)

### Yêu cầu hệ thống:
- **Java Development Kit (JDK):** Yêu cầu phiên bản **JDK 25** trở lên (phù hợp với cấu hình Minecraft của mod).

### Biên dịch dự án:
Mở Terminal hoặc Command Prompt tại thư mục gốc của dự án và chạy lệnh sau:

- **Trên Windows:**
  ```cmd
  gradlew.bat build
  ```
- **Trên Linux/macOS:**
  ```bash
  ./gradlew build
  ```

Sau khi quá trình biên dịch hoàn tất, tệp jar của mod sẽ nằm trong thư mục:
`build/libs/dnbackup-<phiên-bản>.jar`

### Cài đặt:
1. Copy tệp `.jar` đã biên dịch vào thư mục `mods/` của server Minecraft chạy Fabric.
2. (Tùy chọn) Để người chơi có thể nhìn thấy thanh tiến trình HUD mượt mà dưới góc màn hình, người chơi cũng cần cài đặt mod này trên Client của mình.

---

> [!IMPORTANT]
> Dự án được cấu hình bằng Fabric Loom và chia tách rõ rệt môi trường Client/Server để tối ưu hóa hiệu năng render HUD trên Client và xử lý dữ liệu sao lưu trên Server.
