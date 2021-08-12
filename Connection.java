import java.net.InetAddress;
import java.net.Socket;

public class Connection {

    private Socket socket;
    private InetAddress address;
    private String name;

    public Connection (Socket socket, InetAddress address, String name) {
        this.socket = socket;
        this.address = address;
        this.name = name;
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
}
