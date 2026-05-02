import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public final class RconClient {
    private RconClient() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("Usage: java RconClient <host> <port> <password> <command...>");
            System.exit(2);
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String password = args[2];
        String command = String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length));
        try (Socket socket = new Socket(host, port)) {
            socket.setSoTimeout(5000);
            DataInputStream in = new DataInputStream(socket.getInputStream());
            OutputStream out = socket.getOutputStream();
            writePacket(out, 1, 3, password);
            Packet auth = readPacket(in);
            if (auth.id == -1) throw new IOException("RCON auth failed");
            writePacket(out, 2, 2, command);
            Packet response = readPacket(in);
            System.out.write(response.body.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void writePacket(OutputStream out, int id, int type, String body) throws IOException {
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        int length = 4 + 4 + payload.length + 2;
        ByteBuffer buffer = ByteBuffer.allocate(4 + length).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(length);
        buffer.putInt(id);
        buffer.putInt(type);
        buffer.put(payload);
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        out.write(buffer.array());
        out.flush();
    }

    private static Packet readPacket(DataInputStream in) throws IOException {
        int length = Integer.reverseBytes(in.readInt());
        byte[] data = in.readNBytes(length);
        if (data.length != length) throw new IOException("Short RCON packet");
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        int id = buffer.getInt();
        int type = buffer.getInt();
        int bodyLength = Math.max(0, length - 10);
        byte[] body = new byte[bodyLength];
        buffer.get(body);
        return new Packet(id, type, new String(body, StandardCharsets.UTF_8));
    }

    private record Packet(int id, int type, String body) {
    }
}
