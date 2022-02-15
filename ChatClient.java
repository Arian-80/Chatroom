import java.io.BufferedReader;
import java.io.IOException;

public class ChatClient extends Client {

    private ChatClient() {
        super();
    }


    public static void main(String[] args) {
        // Create an instance of the ChatClient class and start the process by calling the startProcess() method and passing on command line args
        ChatClient client = new ChatClient();
        client.startProcess(args);
    }

    @Override
    protected void startProcess(String[] args) {
        // Calls the necessary methods to start the process of connecting to the server.
        super.startProcess(args);
        monitorServerInput();
        processClientInput();
    }

    private void monitorServerInput() {
        // Makes an instance of the ServerInputHandler private class and starts a thread which runs that instance.
        ServerInputHandler serverInputHandler = new ServerInputHandler();
        Thread serverInputHandlerThread = new Thread(serverInputHandler, "c_serverInputHandler");
        serverInputHandlerThread.start();
    }

    private void processClientInput() {
        // This runs on the main thread.

        /* The client's input from the command line is constantly monitored in a while loop as long as the "exit conditions" have not been activated.
         * If there is an IO exception, the client is notified, the loop breaks and the program shuts down.
         * If the client enters "exit", the loop breaks and the program shuts down.
         */
        String userInput;
        try {
            while (!(userInput = super.getUserInputReader().readLine()).equalsIgnoreCase("exit")) {
                super.getBroadcaster().println(userInput);
            }
        } catch (IOException exception) {
            System.out.println("Error occurred with processing input. Please try again.");
        }
        // As the loop is broken, the exit() method is called, which shuts the program down.
        super.exit();
    }


    private class ServerInputHandler implements Runnable {
        // This runs on a separate thread.

        private void processServerInput() {
            BufferedReader serverInputStream = ChatClient.super.getServerInputReader();
            // In an infinite while loop, the server's messages are constantly being received and printed.
            try {
                String serverInput;
                while (true) {
                    serverInput = serverInputStream.readLine();
                    if (!serverInput.isBlank()) {
                        System.out.println(serverInput);
                    }
                }
            } catch (IOException | NullPointerException exception) {
                ChatClient.super.exit();
            }
        }

        public void run() {
            processServerInput();
        }

    }

}
