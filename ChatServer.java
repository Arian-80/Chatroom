import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class ChatServer {

    // List of all available ports
    private final List<Integer> portsList = new ArrayList<>(Arrays.asList(14001, 14002, 14003, 14004, 14005, 14006, 14007, 14008, 14009, 14010));
    // List of all sockets connected to the server
    private final Map<Integer, Connection> connectionsMap = new HashMap<>();
    // List of all bad words from a text file - singleton instance
    private static List<String> badWordsList = null;
    // Number of anonymous users
    private int anonymousUsers = 0;
    // Holds the port number of the server
    private int portNumber;
    // Holds the server socket
    private ServerSocket serverSocket;
    // Holds an instance of the server output Handler.
    private final ServerOutputHandler serverOutputHandler;

    private ChatServer() {
        // Initialise the fields to their default values
        this.portNumber = 14001;
        this.serverOutputHandler = new ServerOutputHandler();
    }

    public static void main(String[] args) {
        // Make an instance of the ChatServer class and start the process by calling the startProcess() method and passing on command line args
        ChatServer server = new ChatServer();
        server.startProcess(args);
    }

    // Getter method for the list of connections, which returns a list of all sockets connected to the server.
    public Map<Integer, Connection> getConnectionsMap() {
        return this.connectionsMap;
    }

    // Getter method for the list of default server ports
    private List<Integer> getPortsList() {
        return this.portsList;
    }

    // Getter method for the server output handler.
    protected ServerOutputHandler getServerOutputHandler () {
        return this.serverOutputHandler;
    }

    private void setServerSocket(int head) {
        // Creates a new server socket and assigns it to the serverSocket field.
        // This method also allows the socket to be bound even if a previous connection is in the timeout state.
        // The program shuts down if the server socket is not successfully set up.
        // If the user enters a port number outside the legal range, the default port value is set and they are informed;..-
        // -.. then this method is called again
        if (head > (getPortsList().size() - 1)) {
            System.out.println("Maximum number of concurrent servers running reached. (10)\nExiting...");
            System.exit(0);
        }
        try {
            this.serverSocket = new ServerSocket(getPortNumber());
            this.serverSocket.setReuseAddress(true);
            System.out.println("Socket setup successful! Port: " + getPortNumber());
        } catch (IOException exception) {
            setPortNumber(getPortsList().get(head));
            setServerSocket(++head);
        } catch (IllegalArgumentException exception) {
            System.out.println("Port outside of range (0 to 65535). Setting port automatically.");
            setPortNumber(getPortsList().get(0));
            setServerSocket(0);
        }
    }

    // Getter method for the port number
    private int getPortNumber() {
        return this.portNumber;
    }

    // Setter method for the port number
    private void setPortNumber(int portNumber) {
        this.portNumber = portNumber;
    }

    // Getter method for the server socket
    private ServerSocket getServerSocket() {
        return this.serverSocket;
    }

    // Getter method for the number of anonymous users
    private int getAnonymousUsers() {
        return this.anonymousUsers;
    }

    // Getter method for the list of bad words.
    public static List<String> getBadWordsList() {
        return ChatServer.badWordsList;
    }

    // Setter method for the list of bad words.
    private void setListOfBadWords (String startDeclaration, String endDeclaration) {
        try {
            // Text file containing an inappropriate word on each line - should be located in the same file as the application.
			/* Text file downloaded from:
			https://www.freewebheaders.com/full-list-of-bad-words-banned-by-google/
			"Full List of Bad Words in English (Text File – One word per line)"
			By James Parker
			Edited slightly to fit the standards of this program.
			 */
            BufferedReader bufferedReader = new BufferedReader(new FileReader("bad_words.txt"));
            createListOfBadWords(bufferedReader, ChatServer.badWordsList, startDeclaration, endDeclaration);
        } catch (FileNotFoundException e) {
            getServerOutputHandler().broadcastToAdmin("\033[0;31mbad_words text file not found. Shutting down.\033[0m");
            exit();
        }
    }

    private void createListOfBadWords (BufferedReader bufferedReader, List<String> list, String startDeclaration, String endDeclaration) {
        String word;
        try {
            while (true) {
                if (bufferedReader.readLine().startsWith(startDeclaration)) {
                    break;
                }
            }
            while ((word = bufferedReader.readLine()) != null && !(word.startsWith(endDeclaration))) {
                if (word.length() < 3) {
                    continue;
                }
                list.add(word.toLowerCase());
            }
        } catch (IOException exception) {
            getServerOutputHandler().broadcastToAdmin("\033[0;31mCouldn't complete setting list of all inappropriate words. Shutting down.\033[0m");
            exit();
        }
    }

    // Increments the anonymousUsers field when an anonymous user joins
    private void increaseAnonCount() {
        this.anonymousUsers++;
    }

    private void startProcess(String[] args) {
        // Run a set of methods in order.
        checkArgs(args);
        setServerSocket(0);
        if (getBadWordsList() == null) {
            ChatServer.badWordsList = new ArrayList<>();
            setListOfBadWords("-----------", "-----------");
        }
        issueConnections();
    }

    // Checks the string array passed on to it - only the command line args are passed as arguments to this method in this program.
    private void checkArgs(String[] args) {
        // Checks if the first element is "-csp", and if so, attempts to parse the next element as an integer.
        // If there is no elements found after the first or if it can not be parsed as an integer, the user notified..-
        // -.. and hence the value of the port number remains unchanged from the default value.
        // If the command is not "-csp", the user is told that the command they entered is an unknown argument.
        try {
            if (args[0].equals("-csp")) {
                try {
                    setPortNumber(Integer.parseInt(args[1]));
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException exception) {
                    getServerOutputHandler().broadcastToAdmin("Illegal arguments. Default value has been set.");
                }
            } else {
                getServerOutputHandler().broadcastToAdmin("Unknown argument: " + args[0]);
            }
        } catch (ArrayIndexOutOfBoundsException ignored) {
            // Exception occurs if the user has not entered any further arguments, hence ignored.
        }
    }

    private void issueConnections() {
        // This runs on the main thread.

        // Make an instance of the ServerInputHandler inner class, and start a thread that runs that instance.
        // The program goes through a loop which continues as long as the serverRunning boolean flag is set to true.
        // Make an instance of the ConnectionHandler inner class, and start a thread that runs that instance.
        // The function of this class is to process any connections once clients connect.
        // If the server runs out of memory, the program notifies the user and then calls the exit() method, which shuts the server down.
        // The program runs through the loop again if the server has not been shut down.
        ServerInputHandler serverInputHandler = new ServerInputHandler();
        Thread serverInputHandlerThread = new Thread(serverInputHandler, "s_serverInputHandler");
        serverInputHandlerThread.start();
        while (true) {
            ConnectionHandler connectionHandler = new ConnectionHandler();
            try {
                Thread connectionHandlerThread = new Thread(connectionHandler, "s_clientConnectionHandler");
                connectionHandlerThread.start();
            } catch (OutOfMemoryError error) {
                getServerOutputHandler().broadcastToAdmin("Out of memory or process/resource limits have been reached. Shutting server down.");
                exit();
            }
        }
    }

    // This method is run when the server wants to shut down.
    private void exit() {
        // Notifies the user that the server is successfully shut down.
        var connections = getConnectionsMap().values();
        getServerOutputHandler().globalServerBroadcast(connections, "Server has shut down.");
        synchronized (getConnectionsMap()) {
            Connection.disconnectAllConnections(connections);
        }
        ResourceCloser.closeCloseables(List.of(getServerSocket()));
        getServerOutputHandler().broadcastToAdmin("Server successfully shut down.");
        System.exit(0);
    }

    private class ServerInputHandler implements Runnable {
        // This runs on a separate thread.

        // Constantly takes the user's input in an infinite while loop, and if it equals to exit, the method returns false..-
        // -.. after closing the BufferedReader.
        // If there's an IO exception, the method also returns false.
        private boolean isServerRunning() {
            BufferedReader serverInputStream = new BufferedReader(new InputStreamReader(System.in));
            String serverInput;
            try {
                while (!(serverInput = serverInputStream.readLine()).equalsIgnoreCase("/servershutdown")) {
                    handleServerInput(serverInput);
                }
            } catch (IOException exception) {
                getServerOutputHandler().broadcastToAdmin("Connection failed. Please try again later.");
            } finally {
                ResourceCloser.closeCloseables(List.of(serverInputStream));
            }
            return false;
        }

        private void handleServerInput (String input) {
            String[] inputWords = input.split("\\s+");
            String startingWord = inputWords[0];
            int targetID;
            var serverOutputHandler = ChatServer.this.getServerOutputHandler();
            switch (startingWord) {
                case "/apm":
                    if (inputWords.length < 2 || (targetID = findTargetByID(inputWords[1])) == -1) {
                        serverOutputHandler.broadcastToAdmin("Incorrect usage of /apm. Type \"/apm <ID> <msg>\".");
                        return;
                    }
                    if (ChatServer.this.getConnectionsMap().containsKey(targetID)) {
                        serverOutputHandler.adminPrivateMessage(ChatServer.this.getConnectionsMap().get(targetID),
                                String.join(" ", Arrays.copyOfRange(inputWords, 2, inputWords.length)));
                        return;
                    } else {
                        serverOutputHandler.broadcastToAdmin("User not found.");
                    }
                    break;
                case "/warn":
                    if (inputWords.length < 3 || (targetID = findTargetByID(inputWords[1])) == -1) {
                        serverOutputHandler.broadcastToAdmin(
                                "Incorrect usage of /warn. Type \"/warn <userID> <reason> | <number_of_warnings> <reason>\".");
                        return;
                    }
                    if (ChatServer.this.getConnectionsMap().containsKey(targetID)) {
                        Connection targetConnection = ChatServer.this.getConnectionsMap().get(targetID);
                        try {
                            targetConnection.warn(Integer.parseInt(inputWords[2]),
                                    String.join(" ", Arrays.copyOfRange(inputWords, 3, inputWords.length)));
                        } catch (ArrayIndexOutOfBoundsException indexOutOfBoundsException) {
                            serverOutputHandler.broadcastToAdmin(
                                    "Incorrect usage of /warn. Type \"/warn <userID> <reason> | <number_of_warnings> <reason>\".");
                            return;
                        } catch (NumberFormatException numberFormatException) {
                            targetConnection.warn(String.join(" ", Arrays.copyOfRange(inputWords, 2, inputWords.length)));
                        }
                    } else {
                        serverOutputHandler.broadcastToAdmin("User not found.");
                    }
                    break;
                case "/getlist":
                    ChatServer.this.getConnectionsMap().values().stream().map(Connection::getPublicIdentity).forEach(serverOutputHandler::broadcastToAdmin);
                    break;
                default:
                    try {
                        if (startingWord.charAt(0) == '/') {
                            serverOutputHandler.broadcastToAdmin("Command " + startingWord.subSequence(1, startingWord.length()) + " not found.");
                        } else {
                            serverOutputHandler.adminBroadcast(getConnectionsMap().values(),
                                    String.join(" ", inputWords));
                        }
                    } catch (StringIndexOutOfBoundsException ignored) {
                    }
            }
        }

        private int findTargetByID (String potentialTarget) {
            try {
                return Integer.parseInt(potentialTarget);
            } catch (NumberFormatException exception) {
                return -1;
            }
        }

        // If the method returns false, the exit() method is called, which shuts the server down.
        public void run() {
            if (!isServerRunning()) {
                ChatServer.this.exit();
            }
        }

    }

    private class ConnectionHandler implements Runnable {
        // This runs on a separate thread.

        // Private field which holds the client's socket.
        private Socket clientSocket;
        // Private field which holds the client's connection
        private Connection clientConnection;
        // Private field which holds the broadcaster to the client
        private PrintWriter broadcaster;
        // Private field which holds the client's input stream
        private BufferedReader clientInputStream;

        private ConnectionHandler() {
            // Looks for a new connection and accepts it as soon as there is a connection attempt.
            // The exceptions thrown may be ignored since they only stop the client from connecting, and that is handled..-
            // -.. on the client side.
            try {
                this.clientSocket = ChatServer.this.getServerSocket().accept();
            } catch (IOException | SecurityException ignored) {
                // Connection is not established with the client, so no need (not possible either) to inform them about this event..-
                // -.. from the server side.
            }
        }

        private Socket getClientSocket() {
            return this.clientSocket;
        }

        private Connection getClientConnection() {
            return this.clientConnection;
        }

        private boolean setClientInputStream() {
            try {
                this.clientInputStream = new BufferedReader(new InputStreamReader(getClientSocket().getInputStream()));
                return true;
            } catch (IOException exception) {
                ResourceCloser.closeCloseables(List.of(getBroadcaster()));
                return false;
            }
        }

        private BufferedReader getClientInputStream() {
            return this.clientInputStream;
        }

        private boolean setBroadcaster() {
            try {
                this.broadcaster = new PrintWriter(getClientSocket().getOutputStream(), true);
                return true;
            } catch (IOException exception) {
                return false;
            }
        }

        private PrintWriter getBroadcaster() {
            return this.broadcaster;
        }


        // The client's socket (connection) is added to the list of connections accepted by the server.
        // The server is notified that the connection is accepted, along with some extra details.
        // The handleInput() method is then called, which takes the client's socket as an argument.
        private void processConnection() {
            Map<Integer, Connection> mapOfConnections = ChatServer.this.getConnectionsMap();
            Socket clientSocket = getClientSocket();
            String name = getName();
            ServerOutputHandler serverOutputHandler = getServerOutputHandler();
            if (name == null) {
                return;
            }
            this.clientConnection = new Connection(clientSocket, clientSocket.getInetAddress(), name, getBroadcaster(),
                    getClientInputStream(), ChatServer.this);
            mapOfConnections.put(getClientConnection().getUniqueID(), getClientConnection());
            serverOutputHandler.globalServerBroadcast(mapOfConnections.values(),
                    getClientConnection().getPublicIdentity() + " has connected! Online users: " +
                    mapOfConnections.size());
            getInformationalMessages().forEach(message -> serverOutputHandler.serverBroadcast(this.clientConnection, message));
            handleInput();
        }

        private List<String> getInformationalMessages() {
            List<String> messages = new ArrayList<>();
            messages.add("Welcome to the server!");
            messages.add("Type \"/serverpop\" without the speech marks to view the population of the server!");
            messages.add("Type \"/pm <ID>\" without the speech marks and <> to PM another user!");
            messages.add("Type \"exit\" without the speech marks to exit the program.");
            return messages;
        }

        private String getName() {
            String name;
            BufferedReader clientInputStream = getClientInputStream();
            PrintWriter broadcaster = getBroadcaster();
            try {
                broadcaster.println("Please enter a name to proceed with (min 2 characters, max 20):");
                while (!isLegalName((name = clientInputStream.readLine().trim()))){
                    broadcaster.println("Name is illegal/already taken. Please choose another name (min 2 characters, max 20):");
                }
                broadcaster.println("Name successfully chosen!");
                // If not null, return name. Otherwise, return anonymous format.
                // Format of anonymous name is so that users can't maliciously/unintentionally impersonate other anonymous users.
                // return Objects.requireNonNullElseGet(name, () -> ("Anonymous " + ChatServer.this.getAnonymousUsers()));
                if (name.equals("")) {
                    ChatServer.this.increaseAnonCount();
                    return ("Anonymous " + ChatServer.this.getAnonymousUsers());
                }
                return name;
            } catch (IOException e) {
                broadcaster.println("Unable to proceed.");
                ResourceCloser.closeCloseables(List.of(getClientSocket(), broadcaster, clientInputStream));
                return null;
            }
        }

        private boolean isLegalName(String name) {
            if (name.equals("")) return true;
            else if (name.length() > 20 || name.length() < 2) return false;
            else if (name.contains("admin") || name.contains("server")) return false;
            return ChatServer.this.getConnectionsMap().values().stream().noneMatch(connection -> connection.getName().equalsIgnoreCase(name));
        }

        private void handleInput() {
            // An instance of the HandleClientInput class is made.
            // A thread is created and started which runs that instance.
            ClientInputHandler inputHandler = new ClientInputHandler(getClientConnection());
            Thread inputHandlerThread = new Thread(inputHandler, "s_clientInputHandler");
            getClientConnection().setInputHandlerThread(inputHandlerThread);
            inputHandlerThread.start();
        }

        public void run() {
            // Sets up the broadcaster and the client's input stream. If either return false, return as it means they were not set up correctly.
            if (!(setBroadcaster() && setClientInputStream())) return;
            // Executes the processConnection() method.
            processConnection();
        }

    }

}
