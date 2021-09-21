import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class HandleClientInput implements Runnable {
	// This runs on a separate thread.

	// Private final field which holds the client's connection.
	private final Connection clientConnection;
	// Private final field which holds an object of the HandleServerOutput inner class, "serverOutputHandler".
	private final HandleServerOutput serverOutputHandler;
	// Private final field which holds the client's server.
	private final ChatServer chatServer;

	public HandleClientInput (Connection clientConnection) {
		this.clientConnection = clientConnection;
		// Set the value of the serverOutputHandler and chatServer fields respectfully.
		this.serverOutputHandler = new HandleServerOutput();
		this.chatServer = clientConnection.getChatServer();
		// Ensure that the list of bad words has been set only once
	}

	private Connection getClientConnection () {
		return this.clientConnection;
	}

	private HandleServerOutput getServerOutputHandler () {
		// Returns the HandleServerOutput object "serverOutputHandler".
		return this.serverOutputHandler;
	}

	private ChatServer getChatServer () {
		return this.chatServer;
	}

	private void handleInput () {
		// Creates a new instance of the BufferedReader which allows the server to receive messages from the client.
		// Constantly check the user's input and call the startBroadcastProcess() method in the HandleServerOutput class..-
		// -.. via the serverOutputHandler object, as long as the input isn't "exit".
		// This is because the user can exit the program if they wish by entering "exit".
		// If there is an IO or a NullPointer exception, inform the user about this error and let them know their connection is being closed.
		// If this is the case, close the client's connection by closing their socket and ignoring the IO exception that may occur.
		// Finally, try to close the BufferedReader object and if an IO exception occurs there, ..-
		// -.. ignore it as it is redundant to try and handle it.
		Connection clientConnection = getClientConnection();
		Socket clientSocket = clientConnection.getSocket();
		HandleServerOutput serverOutputHandler = getServerOutputHandler();
		try {
			String clientInput;
			ChatServer server = getChatServer();
			List<Connection> toRemove = new ArrayList<>();
			BufferedReader clientInputStream = clientConnection.getClientInputStream();
			while (!(clientInput = clientInputStream.readLine()).equalsIgnoreCase("exit")) {
				if (!(processInput(clientConnection, clientInput))) {
					continue;
				}
				synchronized (this) {
					for (Connection connection : server.getListOfConnections()) {
						if (!(serverOutputHandler.broadcast(connection.getSocket(), clientInput, clientConnection.getName()))) {
							toRemove.add(connection);
						}
					}
					for (Connection connection : toRemove) {
						connection.disconnectClient();
					}
				}
			}
		} catch (IOException | NullPointerException exception) {
			String toBroadcast = "Failed to continue process. Closing connection.";
			HandleServerOutput.serverBroadcast(clientSocket, toBroadcast);
			clientConnection.disconnectClient();
		}
	}

	private boolean processInput (Connection connection, String clientInput) {
		switch (clientInput) {
			case "":
				return false;
			case "server_pop":
				HandleServerOutput.serverBroadcast(connection.getSocket(),
						"Server population: " + getChatServer().getListOfConnections().size());
				return false;
			default:
				for (String badWord : ChatServer.getListOfBadWords()) {
					if (clientInput.toLowerCase().contains(badWord)) {
						connection.warn();
						return false;
					}
				}
				return true;
		}
	}

	public void run () {
		// Calls the handleInput() method with the client's socket as an argument.
		handleInput();
	}

	public static class HandleServerOutput {

		private static PrintWriter broadcaster;

//		private void startBroadcastProcess (String clientInput) {
//			/*
//			 * Parameters:
//			 * String clientInput	: The client's input
//			 */
//
//			// Get the list of all client's sockets that have connected to the server.
//			// Go through each of the connections and call the broadcast() method with the client's socket (different for each connection)..-
//			// -.. and the client's input passed on to this method as arguments.
//			// If the broadcast() method returns false, remove that specific socket from the connections list.
//
//			HandleClientInput.this.getClientConnection().getChatServer().getListOfConnections().removeIf
//					(connection -> !broadcast(connection.getSocket(), clientInput));
//		}

		private boolean broadcast (Socket clientSocket, String toBroadcast, String name) {
			/*
			 * Parameters:
			 * Socket clientSocket	: The client's socket
			 * String toBroadcast	: The string to be broadcast
			 */

			// Try to create a new PrintWriter instance and set autoFlush to true.
			// Send the toBroadcast string to the client after attaching a "Client: " prefix to it.
			// Return true if successful. If there is an IO exception, it suggests that this specific client has closed their connection.
			// If this is the case, return false.
			try {
				HandleServerOutput.broadcaster = new PrintWriter(clientSocket.getOutputStream(), true);
				toBroadcast = (name + ": " + toBroadcast);
				HandleServerOutput.broadcaster.println(toBroadcast);
				return true;
			} catch (IOException exception) {
				return false;
			}
		}

		protected static void serverBroadcast (Socket clientSocket, String toBroadcast) {
			try {
				HandleServerOutput.broadcaster = new PrintWriter(clientSocket.getOutputStream(), true);
				toBroadcast = ("\033[0;31m[SERVER]:\033[0m " + toBroadcast);
				HandleServerOutput.broadcaster.println(toBroadcast);
			} catch (IOException ignored) {
			}
		}

	}

}
