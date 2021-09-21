import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class ChatClient extends Client {

	// Boolean flag to dictate whether the user has requested to exit the program.
	private boolean exitActivated;

	private ChatClient () {
		super();
		// Set the boolean flag to false, which is its default value.
		this.exitActivated = false;
	}


	public static void main (String[] args) {
		// Create an instance of the ChatClient class and start the process by calling the startProcess() method and passing on command line args
		ChatClient client = new ChatClient();
		client.startProcess(args);
	}

	private void setExitActivated () {
		// Sets the exitActivated boolean flag to true
		this.exitActivated = true;
	}

	private boolean isExitActivated () {
		// Returns the state of the boolean flag exitActivated
		return this.exitActivated;
	}

	@Override
	protected void startProcess(String[] args) {
		// Calls the startProcess() method in the superclass and passes on the command line args passed on to this method as arguments.
		// The monitorServerInput() method is then called, followed by a call to the processClientInput() method.
		super.startProcess(args);
		monitorServerInput();
		processClientInput();
	}

	private void monitorServerInput () {
		// Makes an instance of the ServerInputHandler private class and starts a thread which runs that instance.
		ServerInputHandler serverInputHandler = new ServerInputHandler();
		Thread serverInputHandlerThread = new Thread(serverInputHandler);
		serverInputHandlerThread.start();
	}

	private void processClientInput () {
		// This runs on the main thread.

		// The client's input from the command line is constantly monitored in a while loop as long as the "exit conditions" have not been activated.
		// If there is an IO exception, the client is notified and the loop breaks, advancing on to the process of shutting the program down.
		// If the client enters "exit", then the loop breaks, advancing on to the process of shutting the program down.
		// Otherwise, the broadcaster set up in the superclass is used to send the client's input to the server.
		String userInput;
		while (!isExitActivated()) {
			try {
				if ((userInput = super.getUserInputReader().readLine()).equalsIgnoreCase("exit")) break;
				super.getBroadcaster().println(userInput);
			} catch (IOException exception) {
				System.out.println("Error occurred with processing input. Please try again.");
				break;
			}
		}
		// As the loop is broken, the exit() method is called, which shuts the program down.
		exit();
	}

	@Override
	protected void exit () {
		// Sets the status of the boolean flag exitActivated to true before calling the exit() method in the superclass.
		setExitActivated();
		super.exit();
	}

	private class ServerInputHandler implements Runnable {
		// This runs on a separate thread.

		private void processServerInput () {
			BufferedReader serverInputStream = ChatClient.super.getServerInputReader();
			// In an infinite while loop, the server's messages are constantly being received and printed.
			try {
				while (true) {
					String serverInput = serverInputStream.readLine();
					if (serverInput != null) {
						System.out.println(serverInput);
						if (serverInput.startsWith("[SERVER]") && serverInput.contains("Disconnecting user")) {
							ChatClient.this.exit();
						}
					}
				}
				// If there is an IO exception, the program checks whether the exitActivated boolean flag is set to true.
				// If it is, this indicates that the user entered "exit" on their command line. The shutdown procedure takes place..-
				// -.. in processClientInput(), as that is where the client's input from the command line is monitored.
				// If exitActivated is still set to false, then this indicates that the server has shut down and hence the client is notified.
				// The program then calls the exit() method, which shuts the program down.
				// Finally, try to close the serverInputStream regardless of the outcome of the previous procedure.
				// If an IO exception occurs in the process of closing the BufferedReader, ignore it as it is redundant to try and handle that.
			} catch (IOException exception) {
				if (!ChatClient.this.isExitActivated()) {
					System.out.println("\033[0;31mServer has shut down.\033[0m");
					ChatClient.this.exit();
				}
			}
		}

		public void run () {
			// Runs the processServerInput() method.
			processServerInput();
		}

	}

}
