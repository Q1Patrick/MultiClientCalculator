# Hệ thống dịch văn bản qua giao thức TCP sử dụng AI

> **Môn học:** Lập trình Mạng  
> **Lớp:** 012012301302

---

##  Thành viên nhóm

|     Họ tên        |     MSSV      |                     Vai trò                           |
|-------------------|---------------|-------------------------------------------------------|
|Châu Thành Phát    | 091206011060  | Server (Python) — TCP Server, tích hợp AI, xử lý file |
|Đào Duy Quân       | 092206002937  | Java Swing (Client)  —  Gửi/nhận file qua TCP               |

---

## Mô tả đề tài

Xây dựng hệ thống dịch văn bản qua giao thức **TCP Socket** theo mô hình **Client – Server**, trong đó:

- **Server** viết bằng **Python** — nhận file văn bản từ client, dịch nội dung sang tiếng Việt bằng AI, trả kết quả về
- **Client** viết bằng **Client/Java Swing** — giao diện người dùng, cho phép chọn file, gửi lên server và nhận file đã dịch

---

##  Chức năng chính

### 3.1 Thiết lập kết nối TCP
- Server Python lắng nghe kết nối từ Client trên địa chỉ IP và port xác định
- Client Java Swing kết nối đến Server thông qua `TcpClient`
- Sử dụng giao thức **TCP (SOCK_STREAM)** đảm bảo truyền dữ liệu tin cậy, đúng thứ tự, không mất gói

### 3.2 Gửi và nhận dữ liệu
- Client gửi file văn bản (`.txt`) đến Server
- Dữ liệu được đóng gói theo định dạng **Length-Prefixed**:
  ```
  ┌─────────────────┬──────────────────────────────┐
  │  4 byte (int)   │  N byte nội dung (UTF-8)      │
  │  = độ dài N     │  = văn bản / JSON header      │
  └─────────────────┴──────────────────────────────┘
  ```
- Server đọc đúng số byte bằng hàm `recv_exact()` — tránh mất gói do TCP stream

### 3.3 Xử lý và lưu trữ dữ liệu
- Server lưu nội dung nhận được vào file tạm `uploads/<tên_file>`
- Chuẩn bị dữ liệu cho bước xử lý AI

### 3.4 Dịch văn bản bằng AI
- Tích hợp **Google Translate** qua thư viện `deep-translator`
- Hỗ trợ dịch sang 4 ngôn ngữ: Tiếng Việt, Tiếng Nhật, Tiếng Anh, Tiếng Tây Ban Nha
- Tự động cắt đoạn 4000 ký tự để không vượt giới hạn API
- Giữ nguyên cấu trúc văn bản gốc

### 3.5 Trả kết quả về Client
- Server gửi JSON response + nội dung đã dịch về Client (cùng định dạng 4-byte header)
- File kết quả được đặt tên tự động: `<tên_gốc>_<ngôn_ngữ>.txt`  
  Ví dụ: `report.txt` → `report_vi.txt`
- Client hiển thị kết quả và tự động lưu file về máy

### 3.6 Xử lý lỗi
- Kiểm tra kết nối client trước khi xử lý
- Xử lý mất kết nối đột ngột (`ConnectionResetError`)
- Hàm `recv_exact()` đảm bảo dữ liệu không bị thiếu
- Server không crash khi 1 client lỗi — các client khác vẫn tiếp tục hoạt động
- Client hiển thị thông báo lỗi rõ ràng khi server chưa chạy

---

##  Cấu trúc thư mục

```
TranslateServer/
└── TranslateProject/
    ├── client/
    │   └── java/
    │       ├── TranslationClient.java
    │       └── README.md
    │
    ├── server/
    │   ├── server.py
    │   ├── handler.py
    │   ├── network.py
    │   ├── translator.py
    │   └── config.py
    │
    ├── data/
    │   ├── input/
    │   └── output/
    │
    ├── docs/
    │   ├── API.md
    │   ├── ARCHITECTURE.md
    │   └── USER_GUIDE.md
    │
    └── README.md

```
##
Yêu cầu cài đặt

### Server (Python)
| Yêu cầu | Phiên bản |
|---------|-----------|
| Python | 3.10+ |
| deep-translator | 1.11.4+ |
```
bash
pip install deep-translator
macOS
pip3 install deep-translator
```
### Client (Java)
| Yêu cầu   | Phiên bản |
|-----------|-----------|
| Java JDK  |    8+     |
| OS | Windows / macOS / Linux |

Tải Java tại: https://www.oracle.com/java/technologies/downloads/
```
##  Hướng dẫn chạy
### Bước 1 — Chạy Server

bash
cd TranslateProject
cd server
python server.py
```

Output mong đợi:
```
Server chạy tại 0.0.0.0:9999
```

### Bước 2 — Chạy Client

Mở thư mục `Client/` bằng VSCode → nhấn **F5**

Hoặc chạy bằng terminal:
```bash
cd TranslateProject
cd client/java/src
javac TranslationClient.java
java TranslationClient
```

### Bước 3 — Sử dụng

1. Nhấn ** Chọn file** → chọn file `.txt` cần dịch
2. Chọn ngôn ngữ đích từ dropdown
3. Nhấn ** Dịch**
4. Đợi server xử lý (~5–10 giây tuỳ độ dài văn bản)
5. File đã dịch tự động lưu cùng thư mục với file gốc

---

## Giao thức truyền dữ liệu

Giao thức **Length-Prefixed TCP** — mỗi lần gửi gồm 2 phần:

[4 byte big-endian integer = N] [N byte UTF-8 content]

**Client → Server:**

Block 1: JSON header  →  {"lang": "vi", "filename": "doc.txt"}
Block 2: Nội dung file .txt

**Server → Client:**
Block 1: JSON response  →  {"status": "ok", "filename": "doc_vi.txt", "lang_name": "Vietnamese"}
Block 2: Nội dung đã dịch


**Tại sao cần 4-byte header?**  
TCP là stream protocol — `recv()` không đảm bảo trả đủ dữ liệu 1 lần. Header cho biết cần đọc bao nhiêu byte, hàm `recv_exact()` loop cho đến khi đủ.

---

##  Kiến trúc hệ thống

```
┌──────────────────┐         TCP :9999           ┌──────────────────┐
│   Java Swing     │ ──── [header][file] ──────► │  Python Server   │
│     Client       │                             │                  │
│                  │ ◄─── [header][dịch] ──────  │ + deep-translator│
└──────────────────┘                             └──────────────────┘
        │                                                │
   Người dùng                                    uploads/ & translated/
   chọn file .txt                                lưu file gốc + kết quả
```

**Multi-client:** Server dùng `threading.Thread` — mỗi client chạy trên thread riêng, hỗ trợ nhiều người dùng đồng thời.

---

##  Test thử nhanh

Tạo file `test_en.txt` với nội dung:
```
Hello! This is a test.
The weather today is very nice.
I want to translate this to Vietnamese.
```

Chạy server → chạy client → chọn file trên → chọn **Tiếng Việt** → nhấn Dịch.

Kết quả mong đợi trong `test_en_vi.txt`:
```
Xin chào! Đây là một bài kiểm tra.
Thời tiết hôm nay rất đẹp.
Tôi muốn dịch cái này sang tiếng Việt.
```

---


