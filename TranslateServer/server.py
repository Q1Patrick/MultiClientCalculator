import socket
import threading 
import os
import json
from deep_translator import GoogleTranslator

HOST = "0.0.0.0"
PORT = 9999
BUFFER_SIZE = 4096

UPLOAD_DIR = "uploads"
DOWNLOAD_DIR = "downloads"


# ================= AI TRANSLATE =================
LANG_NAMES = {
    "vi": "Vietnamese",
    "ja": "Japanese",
    "en": "English",
    "es": "Spanish"
}

def translate_text(text: str, target_lang: str) -> str:
    """
    Dịch text sang ngôn ngữ đích.
    Tự động cắt đoạn nếu quá 4000 ký tự (giới hạn Google Translate).
    
    target_lang: "vi" | "ja" | "en" | "es"
    """
    if target_lang not in LANG_NAMES:
        raise ValueError(f"Ngôn ngữ không hỗ trợ: '{target_lang}'. "
                         f"Chọn trong: {list(LANG_NAMES.keys())}")

    # Cắt thành đoạn 4000 ký tự
    chunks = [text[i:i+4000] for i in range(0, len(text), 4000)]

    translated_chunks = []
    for i, chunk in enumerate(chunks):
        print(f"    Đang dịch đoạn {i+1}/{len(chunks)}...")
        result = GoogleTranslator(source="auto", target=target_lang).translate(chunk)
        translated_chunks.append(result)

    return "\n".join(translated_chunks)


# ================= TCP HELPER =================
def recv_exact(conn: socket.socket, n: int) -> bytes | None:
    """
    TCP là stream — recv() không đảm bảo trả đủ N byte 1 lần.
    Hàm này loop cho đến khi đủ N byte hoặc client ngắt.
    """
    data = b""
    while len(data) < n:
        chunk = conn.recv(min(BUFFER_SIZE, n - len(data)))
        if not chunk:
            return None   # client ngắt kết nối
        data += chunk
    return data
# ================= Đọc đúng N byte từ socket (tránh mất gói) ==================
def recv_block(conn: socket.socket) -> bytes | None:
    """
    Đọc 1 block dữ liệu theo giao thức [4-byte length][content].
    Trả về phần content, hoặc None nếu lỗi.
    """
    raw_len = recv_exact(conn, 4)
    if raw_len is None:
        return None
    length = int.from_bytes(raw_len, "big")
    return recv_exact(conn, length)


def send_block(conn: socket.socket, data: bytes):
    """
    Gửi 1 block theo giao thức [4-byte length][content].
    """
    conn.sendall(len(data).to_bytes(4, "big") + data)

# ================= HANDLE CLIENT =================
def handle_client(conn: socket.socket, addr: tuple):
    client_id = f"{addr[0]}:{addr[1]}"
    print(f"\n{'='*50}")
    print(f"[+] Client kết nối: {client_id}")

    try:
        # --------------------------------------------------------
        # BƯỚC 1: Nhận HEADER JSON
        # { "lang": "vi", "filename": "myfile.txt" }
        # --------------------------------------------------------
        header_bytes = recv_block(conn)
        if header_bytes is None:
            print(f"[-] {client_id}: Không nhận được header.")
            return

        header = json.loads(header_bytes.decode("utf-8"))
        target_lang = header.get("lang", "vi")
        orig_filename = header.get("filename", "file.txt")

        print(f"[→] File    : {orig_filename}")
        print(f"[→] Dịch sang: {LANG_NAMES.get(target_lang, target_lang)}")

        # --------------------------------------------------------
        # BƯỚC 2: Nhận nội dung file gốc
        # --------------------------------------------------------
        file_bytes = recv_block(conn)
        if file_bytes is None:
            print(f"[-] {client_id}: Không nhận được nội dung file.")
            return

        original_text = file_bytes.decode("utf-8")
        print(f"[→] Nhận xong: {len(file_bytes)} bytes ({len(original_text)} ký tự)")

        # Lưu file gốc vào thư mục uploads/
        upload_path = os.path.join(UPLOAD_DIR, orig_filename)
        with open(upload_path, "w", encoding="utf-8") as f:
            f.write(original_text)
        print(f"[✓] Lưu file gốc: {upload_path}")

        # --------------------------------------------------------
        # BƯỚC 3: Dịch nội dung
        # --------------------------------------------------------
        print(f"[⟳] Đang dịch...")
        translated_text = translate_text(original_text, target_lang)
        print(f"[✓] Dịch xong: {len(translated_text)} ký tự")

        # --------------------------------------------------------
        # BƯỚC 4: Lưu file đã dịch + gửi về Client
        # --------------------------------------------------------
        # Đặt tên file output: "report_vi.txt", "report_ja.txt", ...
        base_name = os.path.splitext(orig_filename)[0]   # "report"
        out_filename = f"{base_name}_{target_lang}.txt"  # "report_vi.txt"

        translated_path = os.path.join(DOWNLOAD_DIR, out_filename)
        with open(translated_path, "w", encoding="utf-8") as f:
            f.write(translated_text)
        print(f"[✓] Lưu file dịch: {translated_path}")

        # Gửi response JSON trước
        response_header = json.dumps({
            "status"   : "ok",
            "filename" : out_filename,
            "lang"     : target_lang,
            "lang_name": LANG_NAMES.get(target_lang, target_lang),
            "char_count": len(translated_text),
        }).encode("utf-8")
        send_block(conn, response_header)

        # Gửi nội dung file đã dịch
        translated_bytes = translated_text.encode("utf-8")
        send_block(conn, translated_bytes)

        print(f"[←] Đã gửi file '{out_filename}' ({len(translated_bytes)} bytes) về {client_id}")

    except json.JSONDecodeError as e:
        print(f"[!] Lỗi JSON header từ {client_id}: {e}")
        _send_error(conn, "Header JSON không hợp lệ")

    except ValueError as e:
        print(f"[!] Lỗi ngôn ngữ từ {client_id}: {e}")
        _send_error(conn, str(e))

    except Exception as e:
        print(f"[!] Lỗi xử lý client {client_id}: {e}")
        _send_error(conn, f"Server error: {e}")

    finally:
        conn.close()
        print(f"[-] Đóng kết nối: {client_id}")


def _send_error(conn: socket.socket, message: str):
    """Gửi thông báo lỗi về client."""
    try:
        err = json.dumps({"status": "error", "message": message}).encode("utf-8")
        send_block(conn, err)
        # Gửi file rỗng để client không bị treo ở recv
        send_block(conn, b"")
    except Exception:
        pass




# ================= START SERVER =================
def start_server():
    server_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server_sock.bind((HOST, PORT))
    server_sock.listen(5)

    print("=" * 50)
    print("   TCP File Translation Server")
    print(f"   Port    : {PORT}")
    print(f"   Ngôn ngữ: {', '.join(f'{k}={v}' for k, v in LANG_NAMES.items())}")
    print(f"   Upload  : ./{UPLOAD_DIR}/")
    print(f"   Output  : ./{DOWNLOAD_DIR}/")
    print("   Ctrl+C  : Dừng server")
    print("=" * 50)

    try:
        while True:
            conn, addr = server_sock.accept()

            # Mỗi client chạy trên 1 thread riêng → hỗ trợ nhiều client cùng lúc
            t = threading.Thread(
                target=handle_client,
                args=(conn, addr),
                daemon=True  # thread tự tắt khi main program tắt
            )
            t.start()
            print(f"[i] Đang phục vụ {threading.active_count()-1} client(s)")

    except KeyboardInterrupt:
        print("\n[!] Server dừng.")
    finally:
        server_sock.close()


if __name__ == "__main__":
    start_server()