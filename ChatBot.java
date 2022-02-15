import java.io.BufferedReader;
import java.io.IOException;

public class ChatBot extends Client {

    // Private field which stores the string which is to be broadcast.
    private String toBroadcast;

    private ChatBot() {
        super();
    }

    public static void main(String[] args) {
        // Create an instance of the ChatBot class and start the process by calling the startProcess() method and passing on command line args.
        ChatBot bot = new ChatBot();
        bot.startProcess(args);
    }

    @Override
    protected void startProcess(String[] args) {
        // Calls the necessary methods to start the process of connecting to the server.
        super.startProcess(args);
        handleServerInput();
    }

    private void handleServerInput() {
        /* The server's input is constantly stored in the serverInput variable, and the processServerInput() method is called, ..-
         * -.. which takes the serverInput variable as an argument. This is done in an infinite loop.
         * If there is an IO exception, it suggests that the server has shut down. The user is notified that the server has shut down, ..-
         * -.. and the program shuts down.
         */

        // try statement with automatic resource management - closes the BufferedReader as soon as the program is done with it.
        BufferedReader serverInputStream = super.getServerInputReader();
        try {
            while (true) {
                String serverInput = serverInputStream.readLine();
                processServerInput(serverInput);
            }
        } catch (IOException exception) {
            System.out.println("Server has shut down.");
        }
        super.exit();
    }

    private void processServerInput(String serverInput) {
        /* Split the server input into words by setting the separator to any white space.
         * This creates a string array, stored in the inputArray variable.
         * Check if the third element of inputArray is equal to "bot", as the first two words are always "[number] [Client]:"
         * If that is the case, call the processBotCommand() method and pass on the fourth element of the array in its lowercase form as an argument.
         * If there is an ArrayIndexOutOfBounds exception, ignore it as it simply means the user has not entered enough words..-
         * -.. which could possibly form a command (minimum 3 - e.g "[0] John: bot help").
         */
        try {
            String[] inputArray = serverInput.split("\\s");
            if (inputArray[2].equalsIgnoreCase("bot")) {
                processBotCommand(inputArray[3].toLowerCase());
            }
        } catch (ArrayIndexOutOfBoundsException ignored) {
        }
    }

    private void processBotCommand(String command) {
        /* Takes a string as a parameter, which it checks to see if it matches any of the preset cases.
         * If the command matches a preset string, then set the toBroadcast string variable accordingly.
         * Otherwise, set the toBroadcast string variable to a string dictating that an invalid command has been entered, ..-
         * -.. providing the client with enough information and an additional tip.
         * At the end, regardless of the outcome of the previous procedure, the broadcast() method is called, ..-
         * -.. which takes the toBroadcast string as an argument.
         */
        switch (command) {
            case ("help"):
                this.toBroadcast = ("Hey! You can get responses from me by typing \"bot <msg>\" (without the <> and speech marks).\n" +
                        "Commands available:\n" +
                        "\"bot help\" - I will send this message.\n" +
                        "\"bot hello\" - I will say hello back!\n" +
                        "\"bot server_details\" - I will give you information about the server.");
                break;
            case ("hello"):
                this.toBroadcast = ("Hello! How's your day been so far? :)");
                break;
            case ("server_details"):
                this.toBroadcast = ("Name of the host: " + super.getHostAddress().getHostName() + "\n" +
                        "IP address of the server: " + (super.getHostAddress().getHostAddress()) + "\n" +
                        "The port you are connected to: " + super.getServerPort());
                break;
            default:
                this.toBroadcast = "Invalid command. Type in \"bot help\" without the speech marks to see a list of available commands.";
                break;
        }
        broadcast(this.toBroadcast);
    }

    private void broadcast(String toBroadcast) {
        // Adds a "[BOT] " prefix to the string being broadcast, and then sends it to the server.
        toBroadcast = "[BOT] " + toBroadcast;
        super.getBroadcaster().println(toBroadcast);
    }

}
