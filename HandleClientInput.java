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

	public HandleClientInput (Connection clientConnection) {
		this.clientConnection = clientConnection;
		// Set the value of the serverOutputHandler field
		this.serverOutputHandler = new HandleServerOutput();
	}

	private Connection getClientConnection () {
		return this.clientConnection;
	}

	private HandleServerOutput getServerOutputHandler () {
		// Returns the HandleServerOutput object "serverOutputHandler".
		return this.serverOutputHandler;
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
		try {
			String clientInput;
			List<Connection> toRemove = new ArrayList<>();
			BufferedReader clientInputStream = clientConnection.getClientInputStream();
			while (!(clientInput = clientInputStream.readLine()).equalsIgnoreCase("exit")) {
				synchronized (this) {
					for (Connection connection : clientConnection.getChatServer().getListOfConnections()) {
						if (!(getServerOutputHandler().broadcast(connection.getSocket(), clientInput, clientConnection.getName()))) {
							toRemove.add(connection);
						}
					}
					for (Connection connection : toRemove) {
						disconnectClient(connection);
					}
				}
			}
		} catch (IOException | NullPointerException exception) {
			String toBroadcast = "[ERROR] Failed to continue process. Closing connection.";
			getServerOutputHandler().serverBroadcast(clientSocket, toBroadcast);
			ResourceCloser.closeCloseables(List.of(clientConnection.getSocket(), clientConnection.getBroadcaster(), clientConnection.getClientInputStream()));
		}
	}

	private void disconnectClient (Connection clientConnection) {
		clientConnection.getChatServer().getListOfConnections().remove(clientConnection);
		ResourceCloser.closeCloseables(List.of(clientConnection.getClientInputStream(), clientConnection.getBroadcaster(), clientConnection.getSocket()));
	}

	public void run () {
		// Calls the handleInput() method with the client's socket as an argument.
		handleInput();
	}

	private static class HandleServerOutput {

		private PrintWriter broadcaster;

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
				this.broadcaster = new PrintWriter(clientSocket.getOutputStream(), true);
				toBroadcast = (name + ": " + toBroadcast);
				this.broadcaster.println(toBroadcast);
				return true;
			} catch (IOException exception) {
				return false;
			}
		}

		private void serverBroadcast (Socket clientSocket, String toBroadcast) {
			try {
				this.broadcaster = new PrintWriter(clientSocket.getOutputStream(), true);
				toBroadcast = ("\033[0;31mSERVER: " + toBroadcast + "\033[0m");
				this.broadcaster.println(toBroadcast);
			} catch (IOException ignored) {
			}
		}

	}

}
