package udpgroupchat.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class Server {

	// constants
	public static final int DEFAULT_PORT = 20000;
	public static final int MAX_PACKET_SIZE = 512;

	// port number to listen on
	protected int port;

	// // set of clientEndPoints
	// // note that this is synchronized, i.e. safe to be read/written from
	// // concurrent threads without additional locking
	// protected static final Set<ClientEndPoint> clientEndPoints = Collections
	// .synchronizedSet(new HashSet<ClientEndPoint>());

	protected static final Map<String, Queue<String>> userToMessages = Collections
			.synchronizedMap(new HashMap<String, Queue<String>>());
	protected static final Map<String, Long> lastUpdated = Collections
			.synchronizedMap(new HashMap<String, Long>());
	protected static final Map<String, LinkedList<String>> groupToUsers = Collections
			.synchronizedMap(new HashMap<String, LinkedList<String>>());

	protected static final ArrayList<WorkerThread> threads = new ArrayList<WorkerThread>();

	// constructor
	Server(int port) {
		this.port = port;
	}

	// start up the server
	public void start() {
		DatagramSocket socket = null;
		try {
			// create a datagram socket, bind to port port. See
			// http://docs.oracle.com/javase/tutorial/networking/datagrams/ for
			// details.

			socket = new DatagramSocket(port);

			// TimerThread will check every 120 seconds for inactive users.
			// Users are deleted after 600 seconds of inactivity.
			TimerThread tt = new TimerThread(120, 600, socket);
			tt.start();

			// receive packets in an infinite loop
			while (!socket.isClosed()) {
				// create an empty UDP packet
				byte[] buf = new byte[Server.MAX_PACKET_SIZE];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				// call receive (this will poulate the packet with the received
				// data, and the other endpoint's info)
				socket.receive(packet);
				// start up a worker thread to process the packet (and pass it
				// the socket, too, in case the
				// worker thread wants to respond)
				WorkerThread t = new WorkerThread(packet, socket);
				threads.add(t);
				t.start();
			}
		} catch (IOException e) {
			// we jump out here if there's an error, or if the worker thread (or
			// someone else) closed the socket
			e.printStackTrace();
		} finally {
			if (socket != null && !socket.isClosed())
				socket.close();
		}
	}

	// main method
	public static void main(String[] args) {
		int port = Server.DEFAULT_PORT;

		// check if port was given as a command line argument
		if (args.length > 0) {
			try {
				port = Integer.parseInt(args[0]);
			} catch (Exception e) {
				System.out.println("Invalid port specified: " + args[0]);
				System.out.println("Using default port " + port);
			}
		}

		// instantiate the server
		Server server = new Server(port);

		System.out
				.println("Starting server. Connect with netcat (nc -u localhost "
						+ port
						+ ") or start multiple instances of the client app to test the server's functionality.");

		// start it
		server.start();

		// ...when the socket has been closed and the start() function
		// completed...
		System.out.println("Server shutdown complete");

	}

}
