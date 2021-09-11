import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

public abstract class Client {

	// List of all available ports
	private final List<Integer> listOfPorts = new ArrayList<>(Arrays.asList(14001, 14002, 14003, 14004, 14005, 14006, 14007, 14008, 14009, 14010));
	// Holds the server socket
	private Socket serverSocket;
	// Holds the host's address
	private InetAddress hostAddress;
	// Holds the server's port
	private int serverPort;
	// Holds a PrintWriter object, "broadcaster"
	private PrintWriter broadcaster;

	private BufferedReader userInputReader;

	private BufferedReader serverInputReader;

	protected Client () {
		// Try setting the value of the hostAddress field to localhost.
		// If an UnknownHost exception occurs, the user is notified and the exit() method is called, which shuts the program down.
		try {
			this.hostAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException exception) {
			System.out.println("Connection failed: Unable to connect to localhost.");
			exit();
		}
		// The value of serverPort is set to default, which is 14001
		this.serverPort = 14001;
	}

	private List<Integer> getListOfPorts() {
		// Returns the list of default server ports
		return this.listOfPorts;
	}

	protected Socket getServerSocket () {
		// Returns the server's socket
		return this.serverSocket;
	}

	protected InetAddress getHostAddress () {
		// Returns the host's address
		return this.hostAddress;
	}

	private void setHostAddress (String address) {
		// Tries assigning the hostAddress field to the address passed on to it as an argument.
		// If an UnknownHost exception occurs, it implies that the host was not identified and the user is notified.
		// The value of this field hence remains unchanged from the default value.
		try {
			this.hostAddress = InetAddress.getByName(address);
		} catch (UnknownHostException exception) {
			System.out.println("Unable to identify host. Default value has been set.");
		}
	}

	protected int getServerPort () {
		// Returns the server's port.
		return this.serverPort;
	}

	private void setServerPort (int port) {
		// Sets the server's port to the port passed on to it as an argument.
		this.serverPort = port;
	}

	protected PrintWriter getBroadcaster () {
		// Returns the PrintWriter object "broadcaster".
		return this.broadcaster;
	}

	private void setBroadcaster() {
		// Sets up the broadcaster by assigning it to a new PrintWriter instance.
		// This allows the client to have the ability to send messages to the server.
		// Auto flush is set to true.
		// If there is an IO exception in the process of setting up the broadcaster, the user is notified..-
		// -.. and the exit() method is called which shuts the program down.
		try {
			this.broadcaster = new PrintWriter(getServerSocket().getOutputStream(), true);
		} catch (IOException exception) {
			System.out.println("Error occurred with processing input. Please try again later.");
			exit();
		}
	}

	private void setServerInputReader() {
		try {
			this.serverInputReader = new BufferedReader(new InputStreamReader(this.getServerSocket().getInputStream()));
		} catch (IOException exception) {
			System.out.println("Unable to proceed. Please try again.");
			ResourceCloser.closeCloseables(List.of(getServerSocket(), getUserInputReader()));
			System.exit(-1);
		}
	}

	protected BufferedReader getServerInputReader() {
		return this.serverInputReader;
	}

	private void setUserInputReader() {
		this.userInputReader = new BufferedReader(new InputStreamReader(System.in));
	}

	protected BufferedReader getUserInputReader() {
		return this.userInputReader;
	}

	protected void startProcess (String[] args) {
		// Calls the establishConnection() method which takes the command line args passed on to this method as an argument
		// The setBroadcaster() method is then called, which gives the client the ability to send messages to the server
		establishConnection(args);
		setUserInputReader();
		setServerInputReader();
		setBroadcaster();
		getDetails();
	}

	private void getDetails() {
		BufferedReader userInputReader = getUserInputReader();
		System.out.println("Please enter a name to proceed with:");
		try {
			String name = userInputReader.readLine();
			while (!isLegalName(name, serverInputReader)) {
				System.out.println("Name is already taken. Please choose another name:");
				name = userInputReader.readLine();
			}
			System.out.println("Name successfully chosen!");
		} catch (IOException exception) {
			System.out.println("Unable to proceed.");
			exit();
		}
	}

	private boolean isLegalName(String name, BufferedReader serverInputReader) {
		try {
			getBroadcaster().println(name);
			return (serverInputReader.readLine().equals("1"));
		} catch (IOException e) {
			System.out.println("Unable to proceed. Please try again.");
			ResourceCloser.closeCloseables(List.of(serverInputReader));
			exit();
		}
		return false;
	}

	private void validateAndSetValues (String[] args, String toCheck, int index) {
		// Works very closely with the validateArgs() method.

		/*
		 * Parameters:
		 * String[] args	: command line args in the form of a string array
		 * String toCheck	: a string which indicates what is to be checked, "address" or "port"
		 * int index		: an index which indicates where in the string array the program should look for the toCheck string
		 */

		// If the toCheck string is "address", set the host address to the address found in the specified location in the string array.
		// If there is no element at the specified index, the user is notified..-
		// -.. and the value of the host address hence remains unchanged from the default value.
		switch (toCheck) {
			case ("address"):
				try {
					setHostAddress(args[index]);
				} catch (ArrayIndexOutOfBoundsException exception) {
					System.out.println("Illegal arguments. Default value has been set.");
				}
				break;
			// If the toCheck string is "port", set the server port to the port number found in the specified location in the string array.
			// If there is no element at the specified index or the element can not be parsed as an integer, ..-
			// -.. the user is notified and the value of the port number hence remains unchanged from the default value.
			case ("port"):
				try {
					setServerPort(Integer.parseInt(args[index]));
				} catch (NumberFormatException | ArrayIndexOutOfBoundsException exception) {
					System.out.println("Illegal arguments. Default value has been set.");
				}
				break;
		}
		// The validateArgs() method is called at the end regardless of the outcome of the previous procedure.
		// The args string array, and the location right after the location passed on to this method are passed on as arguments.
		// This is due to the commands and values most likely being in the format of (-command1 value1 -command2 value2).
		validateArgs(args, index + 1);
	}

	private void validateArgs (String[] args, int index) {
		// Works very closely with the validateAndSetValues() method.

		/*
		 * Parameters:
		 * String[] args	: command line args in the form of a string array
		 * int index		: an index which indicates where in the string array the program should run the following procedure on.
		 */

		// Looks for the value of the string at the specified index passed on as an argument in the args string array, which is also passed on.
		// Calls the validateAndSetValues() method with the appropriate arguments based on the value of the string in the string array.
		// The index passed on as an argument to validateAndSetValues is exactly one higher than the index passed on to this method.
		// This is due to the commands and values most likely being in the format of (-command1 value1 -command2 value2).
		// If the user enters a command other than "-cca" and "-ccp", they are notified that the program has faced an unknown argument.
		// If an ArrayIndexOutOfBounds exception occurs, then that means the user has not entered any further arguments, hence this is ignored.
		try {
			switch (args[index]) {
				case ("-cca"):
					validateAndSetValues(args, "address", index + 1);
					break;
				case ("-ccp"):
					validateAndSetValues(args, "port", index + 1);
					break;
				default:
					System.out.println("Unknown argument: " + args[index]);
			}
		} catch (ArrayIndexOutOfBoundsException ignored) {
		}
	}

	protected void establishConnection (String[] args) {
		// Calls the validateArgs() method and passes on the command line args and index 0 (the start) as arguments.
		// Sets up the server socket after the arguments have been validated.
		validateArgs(args, 0);
		setServerSocket(0);
	}

	private void setServerSocket (int head) {
		// Tries to set up the server socket with the values of the host address and server port fields.
		// If successful, the user is notified that the connection has been successfully established, ..-
		// -.. and they are also told what address and port they have connected to.
		// If there is an IO exception, the program notifies the user and suggests that the user ensures the correct address and port..-
		// -.. have been entered, since that is likely the cause of the IO exception. The program then exits.
		// If there is an IllegalArgument exception, it means that the port entered is outside the accepted range.
		// The user is notified regarding this matter, and they are also told that the port is being set to default.
		// The port is set to default and this method is called again.
		if (head > (getListOfPorts().size() - 1)) {
			System.out.println("\n\033[0;31mConnection failed - please ensure correct address and port have been entered.");
			System.out.println("Exiting the program...\033[0m");
			System.exit(1);
		}
		try {
			System.out.println("Connecting to server " + (head + 1) + "...");
			this.serverSocket = new Socket(getHostAddress(), getServerPort());
			System.out.println("\033[0;32mConnection successfully established! Address : port: " + getHostAddress() +
					" : " + getServerPort() + "\033[0m");
		} catch (IOException exception) {
			System.out.println("\033[0;31mConnection failed. Trying again...\033[0m");
			setServerPort(getListOfPorts().get(head));
			setServerSocket(++head);
		} catch (IllegalArgumentException exception) {
			System.out.println("Port outside of range (0 to 65535). Setting port to default.");
			setServerPort(getListOfPorts().get(0));
			setServerSocket(0);
		}
	}

	protected void exit () {
		// Tries to close the PrintWriter object and the server socket.
		// Finally, the program shuts down.

		// Closing getUserInputReader() forces the program to wait for the readLine() in ChatClient under the method of processClientInput().
		// Therefore, that stream is not being closed.
		ResourceCloser.closeCloseables(List.of(getBroadcaster(), getServerSocket(), getServerInputReader()));
		System.out.println("Exiting the program...");
		System.exit(1);
	}
}
