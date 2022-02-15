import java.util.Collection;

public class ServerOutputHandler {

    private void broadcast(Connection target, String toBroadcast) {
        target.getBroadcaster().println(toBroadcast);
    }

    protected void clientBroadcast(Connection source, Collection<Connection> targets, String toBroadcast) {
        /*
         * Parameters:
         * Socket clientSocket	: The client's socket
         * String toBroadcast	: The string to be broadcast
         */

        /* Sends the toBroadcast string to the client after attaching a "[number] name: " prefix to it.
         * Return true if successful. If there is an IO exception, it suggests that this specific client has closed their connection.
         * If this is the case, return false.
         */
        String alteredToBroadcast = ("[" + source.getUniqueID() + "] " + source.getName() + ": " + toBroadcast);
        targets.forEach(target -> broadcast(target, alteredToBroadcast));
        broadcastToAdmin(alteredToBroadcast);
    }

    protected void serverBroadcast(Connection target, String toBroadcast) {
        // Message broadcast by the server to a specific target.
        toBroadcast = ("\033[0;31m[SERVER]:\033[0m " + toBroadcast);
        broadcast(target, toBroadcast);
    }

    protected void globalServerBroadcast(Collection<Connection> connections, String toBroadcast) {
        // Message broadcast by the server to all clients.
        connections.forEach(connection -> serverBroadcast(connection, toBroadcast));
        broadcastToAdmin(toBroadcast);
    }

    protected void adminBroadcast(Collection<Connection> connections, String toBroadcast) {
        // Message broadcast globally by an admin.
        String alteredToBroadcast = ("\033[0;31m[ADMIN]:\033[0m " + toBroadcast);
        connections.forEach(connection -> broadcast(connection, alteredToBroadcast));
        broadcastToAdmin(alteredToBroadcast);
    }

    protected void adminPrivateMessage(Connection target, String toBroadcast) {
        // Message broadcast by an admin to a specific target (PM).
        String prefix = "\033[0;33m[Admin PM]\033[0m ";
        String name = "\033[0;31m[ADMIN]\033[0m -> " + target.getName() + ": ";
        toBroadcast = prefix.concat(name + toBroadcast);
        broadcast(target, toBroadcast);
        broadcastToAdmin(toBroadcast);
    }

    protected void privateMessageBroadcast(Connection target, Connection source, String toBroadcast) {
        // Private message from one client to another.
        String prefix = "\033[0;33m[PM]\033[0m ";
        String name = source.getName() + " -> " + target.getName() + ": ";
        toBroadcast = prefix.concat(name + toBroadcast);
        broadcast(target, toBroadcast);
        broadcast(source, toBroadcast);
    }

    protected void broadcastToAdmin(String toBroadcast) {
        // Simply prints out the passed on message to the admin.
        System.out.println(toBroadcast);
    }
}
