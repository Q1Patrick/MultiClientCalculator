import json
import os
from config import UPLOAD_DIR, DOWNLOAD_DIR, LANG_NAMES
from translator import translate_text
from network import recv_block, send_block
import socket
from docx_helper import process_docx_translation

os.makedirs(UPLOAD_DIR, exist_ok=True)
os.makedirs(DOWNLOAD_DIR, exist_ok=True)

# ================= HANDLE CLIENT =================
def handle_client(conn: socket.socket, addr: tuple):  # tuple ->  example : ("192.0.0.0",54321)
    client_id = f"{addr[0]}:{addr[1]}"
    print(f"\n{'='*50}")
    print(f"[+] Client kết nối: {client_id}")

    try:
        # BƯỚC 1: Nhận HEADER JSON
        header_bytes = recv_block(conn)
        if not header_bytes: return
        header = json.loads(header_bytes.decode("utf-8"))
        target_lang = header.get("lang", "vi")
        orig_filename = header.get("filename", "file.txt")
        ext = os.path.splitext(orig_filename)[1].lower()

        # BƯỚC 2: Nhận nội dung file gốc
        file_bytes = recv_block(conn)
        if not file_bytes: return
        upload_path = os.path.join(UPLOAD_DIR, orig_filename)
        with open(upload_path, "wb") as f:
            f.write(file_bytes)

        # BƯỚC 3: Xử lý theo định dạng
        base_name = os.path.splitext(orig_filename)[0]
        
        if ext == ".docx":
            out_filename = f"AI_TRANS_{base_name}{ext}" # Giữ nguyên đuôi .docx
            translated_path = os.path.join(DOWNLOAD_DIR, out_filename)
            print(f"[⟳] Đang dịch file Word giữ định dạng...")
            process_docx_translation(upload_path, translated_path, target_lang)
            
        elif ext == ".txt":
            out_filename = f"AI_TRANS_{base_name}.txt"
            translated_path = os.path.join(DOWNLOAD_DIR, out_filename)
            print(f"[⟳] Đang dịch file Text...")
            with open(upload_path, "r", encoding="utf-8") as f:
                text = f.read()
            translated_text = translate_text(text, target_lang)
            with open(translated_path, "w", encoding="utf-8") as f:
                f.write(translated_text)
        else:
            raise ValueError(f"Dinh dang {ext} khong duoc ho tro (Chi nhan .txt, .docx)")

        # BƯỚC 4: Gửi phản hồi
        with open(translated_path, "rb") as f:
            content = f.read()
            
        response_header = json.dumps({
            "status": "ok",
            "filename": out_filename, # Báo cho client tên file mới kèm extension đúng
            "char_count": len(content)
         }, ensure_ascii=False).encode("utf-8")
        
        send_block(conn, response_header)
        send_block(conn, content)
        print(f"[✓] Đã gửi lại file: {out_filename}")

    except Exception as e:
        print(f"[!] Lỗi: {e}")
        _send_error(conn, str(e))
    finally:
        conn.close()

def _send_error(conn: socket.socket, message: str):
    """Gửi thông báo lỗi về client."""
    try:
        err = json.dumps({"status": "error", "message": message}).encode("utf-8")
        send_block(conn, err)
        # Gửi file rỗng để client không bị treo ở recv
        send_block(conn, b"")
    except Exception:
        pass