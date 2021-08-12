import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class ChatServer {

    // List of all available ports
    private final List<Integer> listOfPorts = new ArrayList<>(Arrays.asList(14001, 14002, 14003, 14004, 14005, 14006, 14007, 14008, 14009, 14010));
    // List of all sockets connected to the server
    private final List<Connection> listOfConnections = new ArrayList<>();
    // Number of anonymous users
    private int anonymousUsers = 0;
    // Boolean flag which dictates whether the server is running
    private boolean serverRunning;
    // Holds the port number of the server
    private int portNumber;
    // Holds the server socket
    private ServerSocket serverSocket;

    private ChatServer() {
        // Initialise the fields to their default values
        this.portNumber = 14001;
        this.serverRunning = true;
    }

    public static void main(String[] args) {
        // Make an instance of the ChatServer class and start the process by calling the startProcess() method and passing on command line args
        ChatServer server = new ChatServer();
        server.startProcess(args);
    }

    // Getter method for the list of connections, which returns a list of all sockets connected to the server.
    public List<Connection> getListOfConnections() {
        return this.listOfConnections;
    }

    private void setServerSocket(int head) {
        // Creates a new server socket and assigns it to the serverSocket field.
        // This method also allows the socket to be bound even if a previous connection is in the timeout state.
        // The program shuts down if the server socket is not successfully set up.
        // If the user enters a port number outside the legal range, the default port value is set and they are informed;..-
        // -.. then this method is called again
        if (head > 9) {
            System.out.println("Maximum number of concurrent servers running reached. (10)\nExiting...");
            System.exit(0);
        }
        try {
            this.serverSocket = new ServerSocket(getPortNumber());
            this.serverSocket.setReuseAddress(true);
            System.out.println("Socket setup successful! Port: " + getPortNumber());
        } catch (IOException exception) {
            setPortNumber(listOfPorts.get(head));
            setServerSocket(++head);
        } catch (IllegalArgumentException exception) {
            System.out.println("Port outside of range (0 to 65535). Setting port automatically.");
            setPortNumber(0);
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

    // Getter method for the serverRunning boolean flag
    private boolean isServerRunning() {
        return this.serverRunning;
    }

    // Sets the serverRunning boolean flag to false.
    private void stopServerProcesses() {
        this.serverRunning = false;
    }

    private void startProcess(String[] args) {
        // Run a set of methods in order.
        checkArgs(args);
        setServerSocket(0);
        // Add the current instance to the 'LIST_SERVERS' field.
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
                    System.out.println("Illegal arguments. Default value has been set.");
                }
            } else {
                System.out.println("Unknown argument: " + args[0]);
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
        Thread serverInputHandlerThread = new Thread(serverInputHandler);
        serverInputHandlerThread.start();
        while (isServerRunning()) {
            ConnectionHandler connectionHandler = new ConnectionHandler();
            try {
                Thread connectionHandlerThread = new Thread(connectionHandler);
                connectionHandlerThread.start();
            } catch (OutOfMemoryError error) {
                System.out.println("Out of memory or process/resource limits have been reached. Shutting server down.");
                exit();
            }
        }
    }

    // This method is run when the server wants to shut down.
    private void exit() {
        // Runs the stopServerProcesses() method which causes the boolean flag serverRunning to be set to false.
        // Notifies the user that the server is successfully shut down.
        stopServerProcesses();
        try {
            // Attempt to close the server socket.
            // If unsuccessful and an IO exception occurs, ignore the exception and exit the system anyway with status 0..-
            // -.. as that is the purpose of this method.
            getServerSocket().close();
        } catch (IOException ignored) {
        }
        System.out.println("Server successfully shut down.");
        System.exit(0);
    }

    private class ServerInputHandler implements Runnable {
        // This runs on a separate thread.

        // Constantly takes the user's input in an infinite while loop, and if it equals to exit, the method returns false..-
        // -.. after closing the BufferedReader.
        // If there's an IO exception, the method also returns false.
        private boolean isServerRunning() {
            BufferedReader serverInputStream = new BufferedReader(new InputStreamReader(System.in));
            try {
                while (true) {
                    if (serverInputStream.readLine().equalsIgnoreCase("exit")) {
                        serverInputStream.close();
                        return false;
                    }
                }
            } catch (IOException exception) {
                System.out.println("Connection failed. Please try again later.");
                return false;
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

        // The client's socket (connection) is added to the list of connections accepted by the server.
        // The server is notified that the connection is accepted, along with some extra details.
        // The handleInput() method is then called, which takes the client's socket as an argument.
        private void processConnection() {
            ChatServer.this.getListOfConnections().add(new Connection(this.clientSocket, this.clientSocket.getInetAddress(), getName()));
            System.out.println("Connection accepted on ports: " + ChatServer.this.getServerSocket().getLocalPort() + " ; "
                    + this.clientSocket.getPort());
            handleInput(this.clientSocket);
        }

        private String getName() {
            String name;
            try (BufferedReader clientInputStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                do {
                    name = clientInputStream.readLine();
                } while (!isLegalName(name));
                return Objects.requireNonNullElseGet(name, () -> ("Anonymous " + anonymousUsers));
            } catch (IOException e) {
                System.out.println("Unable to proceed. Please try again.");
                exit();
            }
            return null;
        }

        private boolean isLegalName(String name) {
            for (Connection connection : listOfConnections) {
                if (connection.getName().equalsIgnoreCase(name)) {
                    return false;
                }
            }
            return true;
        }

        private void handleInput(Socket clientSocket) {
            // An instance of the HandleClientInput class is made.
            // A thread is created and started which runs that instance.
            HandleClientInput inputHandler = new HandleClientInput(clientSocket, ChatServer.this);
            Thread inputHandlerThread = new Thread(inputHandler);
            inputHandlerThread.start();
        }

        public void run() {
            // Executes the processConnection() method.
            processConnection();
        }

    }

}
