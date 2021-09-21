import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;

public class Connection {

    private final Socket socket;
    private final InetAddress address;
    private final String name;
    private final PrintWriter broadcaster;
    private final BufferedReader clientInputStream;
    private final ChatServer chatServer;
    private int maxWarnings;
    private int warnings;

    public Connection(Socket socket, InetAddress address, String name, PrintWriter broadcaster, BufferedReader clientInputStream, ChatServer chatServer) {
        this.socket = socket;
        this.address = address;
        this.name = name;
        this.broadcaster = broadcaster;
        this.clientInputStream = clientInputStream;
        this.chatServer = chatServer;
        this.maxWarnings = 3;
        this.warnings = 0;
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

    public int getWarnings() {
        return this.warnings;
    }

    public void warn(int warnings) {
        if (warnings < 1) {
            return;
        }
        this.warnings += warnings;
        HandleClientInput.HandleServerOutput.serverBroadcast(getSocket(), "You've been warned. Current warnings: " + getWarnings() +
                " out of " + getMaxWarnings());
        this.processWarnings();
    }

    private int getMaxWarnings () {
        return this.maxWarnings;
    }

    public void setMaxWarnings (int maxWarnings) {
        this.maxWarnings = maxWarnings;
    }

    public void warn() {
        this.warnings++;
        HandleClientInput.HandleServerOutput.serverBroadcast(getSocket(), "You've been warned. Current warnings: " + getWarnings() +
                " out of " + getMaxWarnings());
        this.processWarnings();
    }

    public boolean hasReachedMaxWarnings () {
        return (this.getWarnings() >= this.getMaxWarnings());
    }

    private void processWarnings() {
        if (this.hasReachedMaxWarnings()) {
            // Find a way to IP ban for short period.
            // Perhaps get connection's address and store in a blacklist. Upon establishing connection with client, check blacklist
            // Allow server admin to ban for custom amount of time.
            HandleClientInput.HandleServerOutput.serverBroadcast(getSocket(), "Max warnings reached. Disconnecting user..");
            this.disconnectClient();
        }
    }

    public void disconnectClient () {
        this.getChatServer().getListOfConnections().remove(this);
        ResourceCloser.closeCloseables(List.of(getClientInputStream(), getBroadcaster(), getSocket()));
    }

}