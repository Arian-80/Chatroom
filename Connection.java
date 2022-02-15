import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Connection {

    private static int idCounter = 0;
    private final Socket socket;
    private final InetAddress address;
    private final String name;
    private final PrintWriter broadcaster;
    private final BufferedReader clientInputStream;
    private final ChatServer chatServer;
    private final int uniqueID;
    private final String publicIdentity;
    private Thread inputHandlerThread;
    private int maxWarnings;
    private int warnings;

    public Connection(Socket socket, InetAddress address, String name, PrintWriter broadcaster, BufferedReader clientInputStream, ChatServer chatServer) {
        this.uniqueID = Connection.idCounter;
        Connection.idCounter++;
        this.socket = socket;
        this.address = address;
        this.name = name;
        this.broadcaster = broadcaster;
        this.clientInputStream = clientInputStream;
        this.chatServer = chatServer;
        this.publicIdentity = getName().concat("(" + getUniqueID() + ")");
        this.inputHandlerThread = null;
        this.maxWarnings = 3;
        this.warnings = 0;
    }

    public int getUniqueID() {
        return this.uniqueID;
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

    public String getPublicIdentity() {
        return this.publicIdentity;
    }

    public Thread getInputHandlerThread() {
        return this.inputHandlerThread;
    }

    public void setInputHandlerThread(Thread inputHandlerThread) {
        this.inputHandlerThread = inputHandlerThread;
    }

    public int getWarnings() {
        return this.warnings;
    }

    private int getMaxWarnings() {
        return this.maxWarnings;
    }

    public void setMaxWarnings(int maxWarnings) {
        this.maxWarnings = maxWarnings;
    }

    public void warn(int warnings, String reason) {
        if (warnings < 1) {
            getChatServer().getServerOutputHandler().broadcastToAdmin("Can't warn for less than 1 point.");
            return;
        }
        this.warnings += warnings;
        processWarn(reason);
    }

    public void warn(String word) {
        this.warnings++;
        processWarn(word);
    }

    private void processWarn(String word) {
        ServerOutputHandler serverOutputHandler = getChatServer().getServerOutputHandler();
        serverOutputHandler.serverBroadcast(this, "You've been warned. Current warnings: " + getWarnings() +
                " out of " + getMaxWarnings() + "\nReason: Inappropriate word detected: " + word +
                ". Please raise a ticket by typing \"/ticket <msg>\" without the speech marks and the <> if you believe this is an error.");
        serverOutputHandler.broadcastToAdmin(getPublicIdentity() + " was warned for inappropriate word usage: "
                + word + ". Current warnings: " + getWarnings());
        this.processWarnings(serverOutputHandler);
    }

    public void setWarnings(int warnings) {
        this.warnings = warnings;
        this.processWarnings(getChatServer().getServerOutputHandler());
    }

    public boolean hasReachedMaxWarnings() {
        return (this.getWarnings() >= this.getMaxWarnings());
    }

    private void processWarnings(ServerOutputHandler serverOutputHandler) {
        if (this.hasReachedMaxWarnings()) {
            /* Find a way to IP ban for short period.
             * Perhaps get connection's address and store in a blacklist. Upon establishing connection with client, check blacklist.
             * Allow server admin to ban for custom amount of time.
             */
            serverOutputHandler.serverBroadcast(this, "Max warnings reached. Disconnecting user..");
            this.disconnectConnection();
            serverOutputHandler.broadcastToAdmin(getPublicIdentity() + " was kicked for having " + this.getWarnings() +
                    " out of " + getMaxWarnings() + " warnings.");
        }
    }

    public static void disconnectAllConnections(Collection<Connection> connections) {
        var toRemove = new ArrayList<Connection>();
        connections.forEach(connection -> {
            ResourceCloser.closeCloseables(List.of(connection.getSocket(), connection.getClientInputStream(), connection.getBroadcaster()));
            toRemove.add(connection);
        });
        connections.removeAll(toRemove);
    }

    public void disconnectConnection() {
        var mapOfConnections = getChatServer().getConnectionsMap();
        synchronized (getChatServer().getConnectionsMap()) {
            mapOfConnections.remove(getUniqueID(), this);
        }
        getChatServer().getServerOutputHandler().globalServerBroadcast(mapOfConnections.values(), getPublicIdentity() + " just disconnected.");
        ResourceCloser.closeCloseables(List.of(getSocket(), getClientInputStream(), getBroadcaster()));
    }

}