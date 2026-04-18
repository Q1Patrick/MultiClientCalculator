using System.Net.Sockets;
using System.Text;
using System.Data;
namespace MultiClientCalculatorServer
{
    internal class Program
    {
        static async Task Main(string[] args)
        {
            try
            {
                using TcpClient client = new TcpClient("127.0.0.1", 5000);
                using NetworkStream stream = client.GetStream();
                Console.WriteLine("--- CHƯƠNG TRÌNH MÁY TÍNH TCP ---");
                Console.WriteLine("Cú pháp: [Lệnh]|[Số A]|[Số B]");
                Console.WriteLine("Lệnh hỗ trợ: ADD, SUB, MUL, DIV, POW, SQRT, MOD, SQR, ABS, SIN, PI, SIN, COS, TAN, ROOT");

                while (true)
                {
                    Console.Write("\nNhập yêu cầu (ví dụ: POW|2|3): ");
                    string msg = Console.ReadLine();
                    if (string.IsNullOrEmpty(msg)) break;

                    byte[] data = Encoding.UTF8.GetBytes(msg);
                    await stream.WriteAsync(data, 0, data.Length);

                    byte[] buffer = new byte[1024];
                    int bytesRead = await stream.ReadAsync(buffer, 0, buffer.Length);
                    string response = Encoding.UTF8.GetString(buffer, 0, bytesRead);

                    Console.WriteLine($"=> Kết quả từ Server: {response}");
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine("Lỗi kết nối: " + ex.Message);
            }
        }
    }
}
