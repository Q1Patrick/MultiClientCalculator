using System.Net;
using System.Net.Sockets;
using System.Security.Cryptography;
using System.Text;
using System.Data;
namespace MultiClientCalculatorClient
{
    internal class Program
    {
        const int Port = 5000;
        static async Task Main(string[] args)
        {
            TcpListener listener = new TcpListener(IPAddress.Any, Port);
            listener.Start();
            Console.WriteLine($"[SERVER] Calculator Server started on port {Port}...");
            Console.WriteLine("[INFO] The system is ready to handle multi-threading(Multiple Clients).");
            while (true)
            {
                try
                {
                    // Chấp nhận kết nối mới
                    TcpClient client = await listener.AcceptTcpClientAsync();
                    Console.WriteLine($"[{DateTime.Now:HH:mm:ss}] [CONNECTED] Client {client.Client.RemoteEndPoint} connected.");

                    // Đẩy việc xử lý Client vào một Task riêng để không chặn vòng lặp chính
                    _ = Task.Run(() => HandleClientAsync(client));
                }
                catch (Exception ex) {
                    Console.WriteLine($"[SYSTEM ERROR] {ex.Message}");
                }
            }
        }

        private static async Task HandleClientAsync(TcpClient client)
        {
            using (client) // Sử dụng khối 'using' để tự động đóng kết nối khi xử lý xong
            using (NetworkStream stream = client.GetStream())
            {
                byte[] buffer = new byte[2048];
                while (true)
                {
                    try
                    {
                        int bytesRead = await stream.ReadAsync(buffer, 0, buffer.Length);
                        if (bytesRead == 0) break; // Client ngắt kết nối

                        string request = Encoding.UTF8.GetString(buffer, 0, bytesRead);
                        Console.WriteLine($"[REQUEST] From {client.Client.RemoteEndPoint}: {request}");

                        Console.ForegroundColor = ConsoleColor.Yellow;
                        Console.WriteLine($"[REQUEST] From {client.Client.RemoteEndPoint}: {request}");
                        Console.ResetColor();

                        // Xử lý logic tính toán
                        string result = ProcessCalculation(request);

                        byte[] response = Encoding.UTF8.GetBytes(result);
                        await stream.WriteAsync(response, 0, response.Length);
                    }
                    catch (Exception ex)
                    {
                        Console.WriteLine($"[ERROR] {ex.Message}");
                        break;
                    }
                }
            }
            Console.WriteLine("[DISCONNECTED] Client disconnected.");
        }

        private static string ProcessCalculation(string input)
        {
            try
            {
                // 1. Kỹ thuật Parsing: Chuẩn hóa chuỗi (Xóa khoảng trắng, đổi chữ hoa)
                string cleanedInput = input.Replace(" ", "").ToUpper();

                // 2. Kỹ thuật Parsing: Nhận diện hằng số đặc biệt (Số Pi)
                if (cleanedInput.Contains("PI"))
                    cleanedInput = cleanedInput.Replace("PI", Math.PI.ToString());

                // 3. Nếu bạn vẫn muốn dùng cấu trúc lệnh CMD|A|B nhưng linh hoạt hơn
                string[] parts = cleanedInput.Split('|', ',', ';'); // Hỗ trợ nhiều loại dấu ngăn cách

                if (parts.Length < 1) return "Error";

                string command = parts[0];

                // Parsing số hạng
                double a = parts.Length > 1 ? double.Parse(parts[1]) : 0;
                double b = parts.Length > 2 ? double.Parse(parts[2]) : 0;

                // 4. Xử lý Logic Toán học
                return command switch
                {
                    // --- PHÉP TOÁN CƠ BẢN ---
                    "ADD" or "+" => (a + b).ToString(),
                    "SUB" or "-" => (a - b).ToString(),
                    "MUL" or "*" => (a * b).ToString(),
                    "DIV" or "/" => b != 0 ? (a / b).ToString() : "Error: Div by 0",
                    "MOD" or "%" => (a % b).ToString(),

                    // --- PHÉP TOÁN NÂNG CAO ---
                    "POW" or "^" => Math.Pow(a, b).ToString(),                      // a mũ b
                    "SQR" => Math.Pow(a, 2).ToString(),                      // a bình phương
                    "SQRT" => a >= 0 ? Math.Sqrt(a).ToString() : "Error",     // Căn bậc 2
                    "ROOT" => Math.Pow(a, 1.0 / b).ToString(),                // Căn bậc n (a√b)
                    "ABS" => Math.Abs(a).ToString(),                         // Trị tuyệt đối

                    // --- LƯỢNG GIÁC (Mặc định đổi từ Độ sang Radian) ---
                    "SIN" => Math.Sin(DegreeToRadian(a)).ToString(),
                    "COS" => Math.Cos(DegreeToRadian(a)).ToString(),
                    "TAN" => Math.Tan(DegreeToRadian(a)).ToString(),

                    // --- LOGARIT ---
                    "LOG" => a > 0 ? Math.Log10(a).ToString() : "Error",            // Log cơ số 10
                    "LN" => a > 0 ? Math.Log(a).ToString() : "Error",              // Log tự nhiên (Cơ số e)


                    // --- HẰNG SỐ ---
                    "PI" => Math.PI.ToString(),

                    _ => "Error: Unknown Command"
                };
            }
            catch (FormatException)
            {
                return "Error: Numeric values only";
            }
            catch (Exception ex)
            {
                return $"Error: {ex.Message}";
            }
        }

        // Hàm bổ trợ để tăng tính chuyên nghiệp
        private static double DegreeToRadian(double angle) => Math.PI * angle / 180.0;
    }
}
