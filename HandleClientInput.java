import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class HandleClientInput implements Runnable {
	// This runs on a separate thread.

	// Private final field which holds the client's socket.
	private final Socket clientSocket;

	// Private final field which holds an object of the HandleServerOutput inner class, "serverOutputHandler".
	private final HandleServerOutput serverOutputHandler;

	// Private final field which holds the instance of the server it was created through.
	private final ChatServer server;

	public HandleClientInput (Socket clientSocket, ChatServer server) {
		// Set the value of the clientSocket field to the 'clientSocket' argument passed on as this object is created
		this.clientSocket = clientSocket;
		// Set the value of the serverOutputHandler field to a new instance of the HandleServerOutput class.
		this.serverOutputHandler = new HandleServerOutput();
		// Set the value of the server field to the 'server' argument passed on as this object is created
		this.server = server;
	}

	private Socket getClientSocket () {
		// Returns the client's socket.
		return this.clientSocket;
	}

	private HandleServerOutput getServerOutputHandler () {
		// Returns the HandleServerOutput object "serverOutputHandler".
		return this.serverOutputHandler;
	}

	private ChatServer getServer () {
		// Returns the ChatServer object "server".
		return this.server;
	}

	private void handleInput (Socket clientSocket) {
		// Creates a new instance of the BufferedReader which allows the server to receive messages from the client.
		// Constantly check the user's input and call the startBroadcastProcess() method in the HandleServerOutput class..-
		// -.. via the serverOutputHandler object, as long as the input isn't "exit".
		// This is because the user can exit the program if they wish by entering "exit".
		// If there is an IO or a NullPointer exception, inform the user about this error and let them know their connection is being closed.
		// If this is the case, close the client's connection by closing their socket and ignoring the IO exception that may occur.
		// Finally, try to close the BufferedReader object and if an IO exception occurs there, ..-
		// -.. ignore it as it is redundant to try and handle it.
		try (BufferedReader clientInputStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
			String clientInput;
			while (!(clientInput = clientInputStream.readLine()).equalsIgnoreCase("exit")) {
				getServerOutputHandler().startBroadcastProcess(clientInput);
			}
		} catch (IOException | NullPointerException exception) {
			try {
				String toBroadcast = "Server: [ERROR] Failed to continue process. Closing connection.";
				getServerOutputHandler().broadcast(getClientSocket(), toBroadcast);
				getClientSocket().close();
			} catch (IOException ignored) {
			}
		}
	}

	public void run () {
		// Calls the handleInput() method with the client's socket as an argument.
		handleInput(getClientSocket());
	}

	private class HandleServerOutput {

		// Private field which holds a PrintWriter object
		private PrintWriter broadcaster;

		private void startBroadcastProcess (String clientInput) {
			/*
			 * Parameters:
			 * String clientInput	: The client's input
			 */

			// Get the list of all client's sockets that have connected to the server.
			// Go through each of the connections and call the broadcast() method with the client's socket (different for each connection)..-
			// -.. and the client's input passed on to this method as arguments.
			// If the broadcast() method returns false, remove that specific socket from the connections list.
			List<Socket> connections = HandleClientInput.this.getServer().getListOfConnections();
			connections.removeIf(connection -> !broadcast(connection, clientInput));
		}

		private boolean broadcast (Socket clientSocket, String toBroadcast) {
			/*
			 * Parameters:
			 * Socket clientSocket	: The client's socket
			 * String toBroadcast	: The string to be broadcasted
			 */

			// Try to create a new PrintWriter instance and set autoFlush to true.
			// Send the toBroadcast string to the client after attaching a "Client: " prefix to it.
			// Return true if successful. If there is an IO exception, it suggests that this specific client has closed their connection.
			// If this is the case, return false.
			try {
				broadcaster = new PrintWriter(clientSocket.getOutputStream(), true);
				toBroadcast = ("Client: " + toBroadcast);
				broadcaster.println(toBroadcast);
				return true;
			} catch (IOException exception) {
				return false;
			}
		}

	}

}
