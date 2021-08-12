import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ChatBot extends Client {

	// Private field which stores the string which is to be broadcasted.
	// It is not a local variable and is hence declared here, as the method which requires it is potentially called many times.
	private String toBroadcast;

	private ChatBot () {
		super();
	}

	public static void main (String[] args) {
		// Create an instance of the ChatBot class and start the process by calling the startProcess() method and passing on command line args.
		ChatBot bot = new ChatBot();
		bot.startProcess(args);
	}

	@Override
	protected void startProcess(String[] args) {
		// Calls the startProcess() method in the superclass and passes on the command line args passed on to this method as arguments.
		// The handleServerInput() method is then called.
		super.startProcess(args);
		handleServerInput();
	}

	private void handleServerInput () {
		// Creates a new instance of the BufferedReader, which allows the bot to receive messages from the server.
		// The server's input is constantly stored in the serverInput variable, and the processServerInput() method is called, ..-
		// -.. which takes the serverInput variable as an argument. This is done in an infinite while true loop.
		// If there is an IO exception, it suggests that the server has shut down. The user is notified that input is no longer ..-
		// -.. being received from the server, and hence the program is going to exit by calling the exit() method in the superclass.

		// try statement with automatic resource management - closes the BufferedReader as soon as the program is done with it.
		try (BufferedReader serverInputStream =
					 new BufferedReader(new InputStreamReader(super.getServerSocket().getInputStream()))) {
			while (true) {
				String serverInput = serverInputStream.readLine();
				processServerInput(serverInput);
			}
		} catch (IOException exception) {
			System.out.println("Unable to receive input from the server.");
		}
		super.exit();
	}

	private void processServerInput (String serverInput) {
		// Split the server input into words by setting the separator to any white space.
		// This creates a string array, stored in the inputArray variable.
		// Check if the second element of inputArray is equal to "bot", as the first element is always "Client:"
		// If that is the case, call the processBotCommand() method and pass on the third element of the array in its lowercase form as an argument.
		// If there is an ArrayIndexOutOfBounds exception, ignore it as it simply means the user has not entered enough words..-
		// -.. which could possibly form a command (minimum 2 - e.g "Client: bot help").
		try {
			String[] inputArray = serverInput.split("\\s");
			if (inputArray[1].equalsIgnoreCase("bot")) {
				processBotCommand(inputArray[2].toLowerCase());
			}
		} catch (ArrayIndexOutOfBoundsException ignored) {
		}
	}

	private void processBotCommand (String command) {
		// Takes a string as a parameter, which it checks to see if it matches any of the preset cases.
		// If the command matches a preset string, then set the toBroadcast string variable accordingly.
		// Otherwise, set the toBroadcast string variable to a string dictating that an invalid command has been entered, ..-
		// -.. providing the client with enough information and an additional tip.
		// At the end, regardless of the outcome of the previous procedure, the broadcast() method is called, ..-
		// -.. which takes the toBroadcast string as an argument.
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

	private void broadcast (String toBroadcast) {
		// Adds a "[BOT] " prefix to the string that was set already, and then sends it to the server.
		toBroadcast = "[BOT] " + toBroadcast;
		super.getBroadcaster().println(toBroadcast);
	}

}
