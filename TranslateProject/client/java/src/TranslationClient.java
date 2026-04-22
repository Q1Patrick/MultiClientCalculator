import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;

// Lớp chính kế thừa JFrame để tạo một cửa sổ ứng dụng Desktop
public class TranslationClient extends JFrame {
    
    // --- KHAI BÁO CÁC BIẾN GIAO DIỆN CẦN TRUY CẬP XUYÊN SUỐT ---
    private JComboBox<String> cbLang; // Hộp thoại thả xuống chọn ngôn ngữ
    private JTextArea txtLog;         // Khu vực hiển thị nhật ký (log)
    private File selectedFile;        // Lưu trữ file người dùng đã chọn
    private JLabel lblFileName;       // Nhãn hiển thị tên file đã chọn

    // --- BẢNG MÀU GIAO DIỆN (DEEP PURPLE PALETTE) ---
    // Khai báo sẵn các mã màu tĩnh (final) để dễ dàng tái sử dụng và đồng bộ thiết kế
    private final Color BG_MAIN = new Color(18, 11, 28);          // Nền tím đen ngoài cùng
    private final Color BG_PANEL = new Color(26, 18, 38);         // Nền panel sáng hơn một chút
    private final Color ACCENT_COLOR = new Color(168, 85, 247);   // Màu tím sáng làm điểm nhấn (nút bấm, tiêu đề)
    private final Color TEXT_MAIN = new Color(56, 56, 56);     // Chữ trắng xám dễ đọc
    private final Color TEXT_MUTED = new Color(156, 163, 175);    // Chữ xám mờ cho thông tin phụ
    private final Color BORDER_COLOR = new Color(60, 46, 85);     // Viền màu tím mờ cho các thành phần
    private final Color LOG_BG = new Color(10, 6, 15);            // Nền log console cực tối để làm nổi bật chữ
    private final Color INPUT_BG = new Color(20, 14, 30);         // Nền cho ô nhập liệu (ComboBox)

    // --- HÀM KHỞI TẠO (CONSTRUCTOR) ---
    // Được gọi đầu tiên khi tạo đối tượng TranslationClient mới
    public TranslationClient() {
        setupTheme(); // Cài đặt giao diện hệ thống trước
        
        // Cấu hình các thuộc tính cơ bản của cửa sổ
        setTitle("Translation Proxy | v2.0 - Stable"); // Tiêu đề cửa sổ
        setSize(600, 500);                             // Kích thước (Rộng x Cao)
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Tắt chương trình hoàn toàn khi bấm dấu X
        setLocationRelativeTo(null);                   // Hiển thị cửa sổ ở chính giữa màn hình
        getContentPane().setBackground(BG_MAIN);       // Đặt màu nền chính cho cửa sổ
        
        initComponents(); // Gọi hàm khởi tạo và sắp xếp các thành phần giao diện bên trong
    }

    // --- HÀM CÀI ĐẶT THEME ---
    private void setupTheme() {
        try {
            // Cố gắng sử dụng giao diện mặc định của hệ điều hành (Windows/macOS/Linux)
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            // Ghi đè màu chữ và màu nền mặc định của một số thành phần Swing
            UIManager.put("Label.foreground", TEXT_MAIN);
            UIManager.put("Panel.background", BG_MAIN);

            // Đổi màu nền của hộp thoại
            UIManager.put("OptionPane.background", BG_MAIN);
            // Đổi màu chữ của thông báo thành màu trắng xám
            UIManager.put("OptionPane.messageForeground", TEXT_MAIN); 
        
            // (Tùy chọn) Sửa luôn màu của nút "OK" cho đồng bộ thay vì màu trắng toát
            UIManager.put("Button.background", new Color(45, 35, 60));
            UIManager.put("Button.foreground", TEXT_MAIN);
        } catch (Exception ignored) {
            // Bỏ qua lỗi nếu không thể thiết lập LookAndFeel
        }
    }

    // --- HÀM DỰNG GIAO DIỆN CHÍNH ---
    private void initComponents() {
        // Sử dụng BorderLayout để chia cửa sổ thành các vùng: BẮC (Trên), GIỮA, NAM (Dưới)
        setLayout(new BorderLayout(15, 15)); // Khoảng cách giữa các vùng là 15px
        // Thêm khoảng đệm (padding) 20px xung quanh toàn bộ nội dung cửa sổ
        ((JPanel)getContentPane()).setBorder(new EmptyBorder(20, 20, 20, 20));

        // --- 1. KHU VỰC TIÊU ĐỀ (TOP HEADER) ---
        JLabel title = new JLabel("NETWORK TRANSLATOR SYSTEM", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22)); // Đổi font chữ, in đậm, cỡ 22
        title.setForeground(ACCENT_COLOR);                  // Đổi màu chữ thành màu tím sáng
        add(title, BorderLayout.NORTH);                     // Đặt ở phía trên cùng của cửa sổ

        // --- 2. KHU VỰC NỘI DUNG CHÍNH (CENTER CONTENT) ---
        JPanel mainPanel = new JPanel(new GridBagLayout()); // Dùng GridBagLayout để dễ căn giữa linh hoạt
        mainPanel.setOpaque(false); // Làm trong suốt panel để lộ màu nền phía sau
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL; // Cho phép thành phần giãn nở theo chiều ngang
        gbc.insets = new Insets(10, 0, 10, 0);    // Khoảng cách trên dưới là 10px
        gbc.weightx = 1;

        // Tạo Panel chứa mục chọn Ngôn ngữ và File
        JPanel pnlSource = createModernPanel("DỮ LIỆU ĐẦU VÀO");
        pnlSource.setLayout(new BorderLayout(10, 10));
        
        // Cấu hình ComboBox (Danh sách sổ xuống) chọn ngôn ngữ
        cbLang = new JComboBox<>(new String[]{"Vietnamese (vi)", "Japanese (ja)", "English (en)", "Spanish (es)"});
        cbLang.setBackground(INPUT_BG);
        cbLang.setForeground(TEXT_MAIN);
        cbLang.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        // Cấu hình nút chọn file và nhãn hiển thị tên file
        JButton btnFile = createModernButton("CHỌN FILE", new Color(45, 35, 60)); 
        lblFileName = new JLabel("Chưa chọn file...");
        lblFileName.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        lblFileName.setForeground(TEXT_MUTED);
        
        // Gom nút và nhãn file vào chung một panel nằm ngang
        JPanel fileBox = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fileBox.setOpaque(false);
        fileBox.add(btnFile);
        fileBox.add(lblFileName);
        
        // Đưa ComboBox và khu vực chọn file vào Panel Đầu vào
        pnlSource.add(cbLang, BorderLayout.NORTH);
        pnlSource.add(fileBox, BorderLayout.CENTER);

        // Thêm Panel Đầu vào vào giữa màn hình
        gbc.gridy = 0; mainPanel.add(pnlSource, gbc);
        add(mainPanel, BorderLayout.CENTER);

        // --- 3. KHU VỰC BÊN DƯỚI (LOG & NÚT GỬI) ---
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setOpaque(false);

        // Nút Gửi Yêu Cầu nổi bật
        JButton btnSend = createModernButton("GỬI YÊU CẦU DỊCH", ACCENT_COLOR);
        btnSend.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnSend.setPreferredSize(new Dimension(0, 45)); // Chiều cao nút là 45px

        // Cấu hình khu vực hiển thị Log
        txtLog = new JTextArea(8, 0); // Hiện 8 dòng
        txtLog.setEditable(false);    // Không cho người dùng gõ chữ vào log
        txtLog.setBackground(LOG_BG);
        txtLog.setForeground(Color.WHITE);
        txtLog.setFont(new Font("Consolas", Font.PLAIN, 13)); // Font code dễ nhìn
        txtLog.setBorder(new EmptyBorder(10, 10, 10, 10)); // Đệm bên trong khung text
        
        // Thêm thanh cuộn cho khu vực Log
        JScrollPane spLog = new JScrollPane(txtLog);
        // Tạo viền có tiêu đề "NHẬT KÝ HOẠT ĐỘNG"
        spLog.setBorder(new TitledBorder(new LineBorder(BORDER_COLOR), "NHẬT KÝ HOẠT ĐỘNG (LOG)", TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 12), TEXT_MAIN));
        spLog.getVerticalScrollBar().setBackground(BG_PANEL);

        bottomPanel.add(btnSend, BorderLayout.NORTH);
        bottomPanel.add(spLog, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH); // Đặt ở phía dưới cùng cửa sổ

        // --- 4. KHAI BÁO CÁC SỰ KIỆN (EVENTS) ---
        
        // Sự kiện khi bấm nút "CHỌN FILE"
        btnFile.addActionListener(e -> {
            JFileChooser jfc = new JFileChooser(); // Mở hộp thoại chọn file của Windows
            if (jfc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                selectedFile = jfc.getSelectedFile(); // Lưu file được chọn
                lblFileName.setText(selectedFile.getName()); // Cập nhật tên file lên màn hình
                log("[INFO] Đã tải file: " + selectedFile.getName()); // Ghi log
            }
        });

        // Sự kiện khi bấm nút "GỬI YÊU CẦU DỊCH"
        btnSend.addActionListener(e -> {
            // Kiểm tra xem đã chọn file chưa
            if (selectedFile == null) {
                JOptionPane.showMessageDialog(this, "Hãy chọn file trước!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return; // Dừng lại không làm tiếp nếu chưa có file
            }
            // Tạo một luồng (Thread) mới để chạy xử lý mạng. 
            // KHÔNG chạy mạng trên luồng giao diện chính để tránh treo ứng dụng
            new Thread(this::executeTask).start();
        });
    }

    // --- HÀM XỬ LÝ MẠNG VÀ GỬI DỮ LIỆU CHÍNH ---
    private void executeTask() {
        // Cấu hình địa chỉ IP và Cổng của Server dịch thuật (Hardcode)
        String ip = "127.0.0.1"; // Localhost (Máy hiện tại)
        int port = 9999;
        
        // Lấy mã ngôn ngữ từ ComboBox (Ví dụ: từ "Vietnamese (vi)" lấy ra chữ "vi")
        String lang = ((String)cbLang.getSelectedItem()).split("\\(")[1].replace(")", "");

        log(">>> [SOCKET] Đang kết nối tới " + ip + ":" + port);
        
        // Bắt đầu kết nối Socket (try-with-resources tự động đóng Socket khi xong)
        try (Socket s = new Socket(ip, port)) {
            // Tạo luồng dữ liệu vào/ra chuẩn
            DataOutputStream out = new DataOutputStream(s.getOutputStream());
            DataInputStream in = new DataInputStream(s.getInputStream());

            // 1. Đóng gói Header (Thông tin cấu hình) dưới dạng JSON
            String json = String.format("{\"lang\":\"%s\",\"filename\":\"%s\"}", lang, selectedFile.getName());
            log(">>> [SEND] Dữ liệu Metadata (" + json.length() + " bytes)");
            sendBlock(out, json.getBytes("UTF-8")); // Gửi Header đi

            // 2. Đọc toàn bộ file đã chọn thành mảng byte và gửi đi
            byte[] data = Files.readAllBytes(selectedFile.toPath());
            log(">>> [SEND] Dữ liệu File (" + data.length + " bytes)");
            sendBlock(out, data); // Gửi File đi

            // 3. Chờ phản hồi từ Server
            log("<<< [WAIT] Server đang xử lý...");
            
            // Đọc Header phản hồi từ Server (ví dụ: thông báo thành công/thất bại)
            byte[] headRes = readBlock(in);
            log("<<< [RECV] Phản hồi: " + new String(headRes));
            
            // Đọc nội dung file đã được dịch từ Server
            byte[] bodyRes = readBlock(in);
            
            // 4. Tạo file mới để lưu kết quả, tiền tố "AI_TRANS_"
            File outF = new File(selectedFile.getParent(), "AI_TRANS_" + selectedFile.getName());
            Files.write(outF.toPath(), bodyRes); // Ghi dữ liệu ra file
            
            log("[SUCCESS] Đã lưu kết quả tại: " + outF.getName());
            
            // Hiển thị thông báo (Popup) thành công cho người dùng
            JLabel successMsg = new JLabel("Dịch thành công!");
            successMsg.setForeground(Color.WHITE); // Ép màu chữ thành trắng
            successMsg.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            JOptionPane.showMessageDialog(this, successMsg, "Hoàn tất", JOptionPane.PLAIN_MESSAGE);

        } catch (Exception ex) {
            // Bắt và in lỗi nếu không kết nối được hoặc lỗi trong quá trình gửi/nhận
            log("[ERR] Lỗi kết nối: " + ex.getMessage());
        }
    }

    // --- CÁC HÀM TIỆN ÍCH HỖ TRỢ (HELPERS) ---

    // Hàm tiện ích để tạo một Panel có viền bo góc và tiêu đề đẹp mắt
    private JPanel createModernPanel(String title) {
        JPanel p = new JPanel();
        p.setBackground(BG_PANEL);
        p.setBorder(new CompoundBorder(
            // Viền ngoài: có tiêu đề, bo góc, màu tím
            new TitledBorder(new LineBorder(BORDER_COLOR, 1, true), title, TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 12), ACCENT_COLOR),
            // Viền trong: padding để nội dung không sát viền
            new EmptyBorder(5, 10, 10, 10)
        ));
        return p;
    }

    // Hàm tiện ích để tạo Nút bấm theo phong cách Modern phẳng
    private JButton createModernButton(String text, Color bgColor) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setFocusPainted(false); // Bỏ viền chấm gạch khi click
        b.setBackground(bgColor);
        b.setForeground(TEXT_MAIN);
        b.setBorder(new CompoundBorder(new LineBorder(BORDER_COLOR), new EmptyBorder(8, 15, 8, 15)));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR)); // Đổi con trỏ chuột thành hình bàn tay khi di chuột qua
        
        // Thêm hiệu ứng hover (đổi màu sáng lên một chút khi đưa chuột vào)
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { 
                b.setBackground(bgColor.brighter()); 
            }
            public void mouseExited(java.awt.event.MouseEvent e) { 
                b.setBackground(bgColor); 
            }
        });
        return b;
    }

    // Hàm tiện ích gửi một khối dữ liệu qua mạng an toàn
    // Giao thức: Gửi ĐỘ DÀI cục dữ liệu trước (4 bytes int), sau đó mới gửi DỮ LIỆU
    private void sendBlock(DataOutputStream out, byte[] d) throws IOException {
        out.writeInt(d.length);
        out.write(d);
        out.flush(); // Bắt buộc xả bộ đệm để đẩy dữ liệu đi ngay
    }

    // Hàm tiện ích đọc một khối dữ liệu từ mạng
    // Giao thức: Đọc ĐỘ DÀI cục dữ liệu trước, cấp phát mảng, rồi đọc đủ số byte đó
    private byte[] readBlock(DataInputStream in) throws IOException {
        int len = in.readInt();
        byte[] d = new byte[len];
        in.readFully(d); // Đảm bảo đọc đủ byte chứ không đọc thiếu
        return d;
    }

    // Hàm tiện ích dùng để ghi chữ lên màn hình Log (Khu vực txtLog)
    private void log(String m) {
        // Đảm bảo việc thay đổi giao diện phải được thực hiện trên luồng Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            txtLog.append(" " + m + "\n"); // Thêm chữ và xuống dòng
            // Tự động cuộn xuống dòng cuối cùng
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }

    // --- HÀM MAIN ---
    // Điểm bắt đầu của chương trình
    public static void main(String[] args) {
        // Chạy giao diện an toàn thông qua SwingUtilities
        SwingUtilities.invokeLater(() -> new TranslationClient().setVisible(true));
    }
}