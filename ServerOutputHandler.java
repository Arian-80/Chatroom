import java.util.Collection;

public class ServerOutputHandler {

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

    private void broadcast(Connection target, String toBroadcast) {
        target.getBroadcaster().println(toBroadcast);
    }

    protected void clientBroadcast(Connection source, Collection<Connection> targets, String toBroadcast) {
        /*
         * Parameters:
         * Socket clientSocket	: The client's socket
         * String toBroadcast	: The string to be broadcast
         */

        // Try to create a new PrintWriter instance and set autoFlush to true.
        // Send the toBroadcast string to the client after attaching a "Client: " prefix to it.
        // Return true if successful. If there is an IO exception, it suggests that this specific client has closed their connection.
        // If this is the case, return false.
        String alteredToBroadcast = ("[" + source.getUniqueID() + "] " + source.getName() + ": " + toBroadcast);
        targets.forEach(target -> broadcast(target, alteredToBroadcast));
        broadcastToAdmin(alteredToBroadcast);
    }

    protected void serverBroadcast(Connection target, String toBroadcast) {
        toBroadcast = ("\033[0;31m[SERVER]:\033[0m " + toBroadcast);
        broadcast(target, toBroadcast);
    }

    protected void globalServerBroadcast(Collection<Connection> connections, String toBroadcast) {
        connections.forEach(connection -> serverBroadcast(connection, toBroadcast));
        broadcastToAdmin(toBroadcast);
    }

    protected void adminBroadcast(Collection<Connection> connections, String toBroadcast) {
        String alteredToBroadcast = ("\033[0;31m[ADMIN]:\033[0m " + toBroadcast);
        connections.forEach(connection -> broadcast(connection, alteredToBroadcast));
        broadcastToAdmin(alteredToBroadcast);
    }

    protected void adminPrivateMessage(Connection target, String toBroadcast) {
        String prefix = "\033[0;33m[Admin PM]\033[0m ";
        String name = "\033[0;31m[ADMIN]\033[0m -> " + target.getName() + ": ";
        toBroadcast = prefix.concat(name + toBroadcast);
        broadcast(target, toBroadcast);
        broadcastToAdmin(toBroadcast);
    }

    protected void privateMessageBroadcast(Connection target, Connection source, String toBroadcast) {
        String prefix = "\033[0;33m[PM]\033[0m ";
        String name = source.getName() + " -> " + target.getName() + ": ";
        toBroadcast = prefix.concat(name + toBroadcast);
        broadcast(target, toBroadcast);
        broadcast(source, toBroadcast);
    }

    protected void broadcastToAdmin(String toBroadcast) {
        System.out.println(toBroadcast);
    }
}
