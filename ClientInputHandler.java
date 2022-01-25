import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientInputHandler implements Runnable {
	// This runs on a separate thread.

	// Private final field which holds the client's connection.
	private final Connection clientConnection;
	// Private final field which holds an object of the HandleServerOutput inner class, "serverOutputHandler".
	private final ServerOutputHandler serverOutputHandler;

	public ClientInputHandler(Connection clientConnection) {
		this.clientConnection = clientConnection;
		// Set the value of the serverOutputHandler and chatServer fields respectfully.
		this.serverOutputHandler = new ServerOutputHandler();
		// Ensure that the list of bad words has been set only once
	}

	private Connection getClientConnection () {
		return this.clientConnection;
	}

	private ServerOutputHandler getServerOutputHandler () {
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
		Connection source = getClientConnection();
		ChatServer server = source.getChatServer();
		ServerOutputHandler serverOutputHandler = server.getServerOutputHandler();
		try {
			String clientInput;
			BufferedReader clientInputStream = source.getClientInputStream();
			while (!(clientInput = clientInputStream.readLine().trim()).equalsIgnoreCase("exit")) {
				if (!(processInput(source, clientInput, serverOutputHandler))) {
					continue;
				}
				getServerOutputHandler().clientBroadcast(source, server.getConnectionsMap().values(), clientInput);
			}
			source.disconnectConnection();
		} catch (IOException | NullPointerException exception) {
			if (!source.getSocket().isClosed()) {
				String toBroadcast = "Failed to continue process. Closing connection.";
				serverOutputHandler.serverBroadcast(source, toBroadcast);
				source.disconnectConnection();
			}
		}
	}

	private boolean processInput (Connection source, String clientInput, ServerOutputHandler serverOutputHandler) {
		String[] clientInputWords = clientInput.split("\\s+");
		if (clientInput.length() < 1) {
			return false;
		} else if (clientInputWords[0].equalsIgnoreCase("/pm")) {
			processPrivateMessage(source, clientInputWords, serverOutputHandler);
			return false;
		} else if (clientInputWords[0].equalsIgnoreCase("/serverpop")) {
			var mapOfConnections = source.getChatServer().getConnectionsMap();
			serverOutputHandler.serverBroadcast(source,"Server population: " + mapOfConnections.size());
			mapOfConnections.values().stream().map(Connection::getPublicIdentity).forEach(connection -> serverOutputHandler.serverBroadcast(source, connection));
			return false;
		} else {
			AtomicBoolean inappropriateWordFound = new AtomicBoolean(false);
			synchronized (source) {
				ChatServer.getBadWordsList().stream()
						.filter(clientInput::contains)
						.forEach(badWord ->
						{
							source.warn("Inappropriate word detected: " + badWord +
									". Please raise a ticket by typing \"/ticket <msg>\" without the speech marks and the <> if you believe this is an error.");
							inappropriateWordFound.set(true);
						});
			}
			return (!inappropriateWordFound.get());
		}
	}

	private void processPrivateMessage(Connection source, String[] clientInputWords, ServerOutputHandler serverOutputHandler) {
	    if (clientInputWords.length < 3) {
	    	return;
		}
	    try {
	    	int uniqueID = Integer.parseInt(clientInputWords[1]);
			if (source.getChatServer().getConnectionsMap().containsKey(uniqueID)) {
				getServerOutputHandler().privateMessageBroadcast(source.getChatServer().getConnectionsMap().get(uniqueID), source,
						String.join(" ", Arrays.copyOfRange(clientInputWords, 2, clientInputWords.length)));
				return;
			}
			serverOutputHandler.serverBroadcast(source, "User not found.");
		} catch (NumberFormatException exception) {
	    	serverOutputHandler.serverBroadcast(source, "Incorrect usage of /pm. Correct usage is \"/pm <ID> <msg>\".");
		}
    }

	public void run () {
		// Calls the handleInput() method with the client's socket as an argument.
		handleInput();
	}

}
