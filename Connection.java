import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class Connection {

    private final Socket socket;
    private final InetAddress address;
    private final String name;
    private final PrintWriter broadcaster;
    private final BufferedReader clientInputStream;
    private final ChatServer chatServer;

    public Connection(Socket socket, InetAddress address, String name, PrintWriter broadcaster, BufferedReader clientInputStream, ChatServer chatServer) {
        this.socket = socket;
        this.address = address;
        this.name = name;
        this.broadcaster = broadcaster;
        this.clientInputStream = clientInputStream;
        this.chatServer = chatServer;
    }

    public Socket getSocket() {
        return this.socket;
    }

    public InetAddress getAddress() {
        return this.address;
    }

    public String getName() {
        return this.name;
    }

    public PrintWriter getBroadcaster() {
        return this.broadcaster;
    }

    public BufferedReader getClientInputStream() {
        return this.clientInputStream;
    }

    public ChatServer getChatServer() {
        return this.chatServer;
    }
}
