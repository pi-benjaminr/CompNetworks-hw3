package udpgroupchat.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Scanner;

public class WorkerThread extends Thread {

	private DatagramPacket rxPacket;
	private DatagramSocket socket;

	public WorkerThread(DatagramPacket packet, DatagramSocket socket) {
		this.rxPacket = packet;
		this.socket = socket;
	}

	@Override
	public void run() {
		// convert the rxPacket's payload to a string
		String payload = new String(rxPacket.getData(), 0, rxPacket.getLength())
				.trim();
		System.out.print("RECEIVED: " + payload + "\n");

		// dispatch request handler functions based on the payload's prefix

		if (payload.startsWith("REGISTER")) {
			onRegisterRequested(payload);
			return;
		}

		if (payload.startsWith("UNREGISTER")) {
			onUnregisterRequested(payload);
			return;
		}

		if (payload.startsWith("SENDU")) {
			onSendURequested(payload);
			return;
		}

		if (payload.startsWith("SENDG")) {
			onSendGRequested(payload);
			return;
		}

		if (payload.startsWith("JOIN")) {
			onJoinRequested(payload);
			return;
		}

		if (payload.startsWith("POLL")) {
			onPollRequested(payload);
			return;
		}

		if (payload.startsWith("SHUTDOWN")) {
			onShutdownRequested(payload);
			return;
		}

		//
		// implement other request handlers here...
		//

		// if we got here, it must have been a bad request, so we tell the
		// client about it
		onBadRequest(payload);
	}

	// send a string, wrapped in a UDP packet, to the specified remote endpoint
	public void send(String payload, InetAddress address, int port)
			throws IOException {
		DatagramPacket txPacket = new DatagramPacket(payload.getBytes(),
				payload.length(), address, port);
		this.socket.send(txPacket);
	}

	/**
	 * REGISTER format: REGISTER username Creates a new user, and sends
	 * confirmation to the sender. If username is already taken, sends error
	 * message to the sender.
	 * 
	 * @param payload
	 */
	private void onRegisterRequested(String payload) {

		String username;
		try {
			Scanner line = new Scanner(payload);
			line.next();
			username = line.next();
		} catch (NoSuchElementException e) {
			onBadRequest(payload);
			return;
		}

		// if the user already exists...
		if (!Server.userToMessages.containsKey(username)) {
			Server.userToMessages.put(username, new LinkedList<String>());
			Server.lastUpdated.put(username, System.currentTimeMillis());
			try {
				send("REGISTERED " + username + "\n",
						this.rxPacket.getAddress(), this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else { // if the user doesn't already exist, create them
			try {
				send("FAILED TO REGISTER: " + username + " already exists\n",
						this.rxPacket.getAddress(), this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * UNREGISTER format: UNREGISTER username If the username exists, delete it
	 * from all maps and lists, and send confirmation Else, send error message.
	 * 
	 * @param payload
	 */
	private void onUnregisterRequested(String payload) {
		String username;
		try {
			Scanner line = new Scanner(payload);
			line.next();
			username = line.next();
		} catch (NoSuchElementException e) {
			onBadRequest(payload);
			return;
		}
		if (Server.userToMessages.containsKey(username)) {
			// yes, remove it
			Server.userToMessages.remove(username);
			for (String key : Server.groupToUsers.keySet()) {
				Server.groupToUsers.get(key).remove(username);
			}
			Server.lastUpdated.remove(username);
			try {
				send("UNREGISTERED " + username + "\n",
						this.rxPacket.getAddress(), this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			// no, send back a message
			try {
				send("CLIENT NOT REGISTERED\n", this.rxPacket.getAddress(),
						this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * SENDU format: SENDU sendingUser receivingUser message Adds message to the
	 * queue of recipient. Sends error message if either username does not
	 * exist.
	 * 
	 * @param payload
	 */
	private void onSendURequested(String payload) {
		String to, from, message;
		try {
			Scanner line = new Scanner(payload);
			line.next();
			from = line.next();
			to = line.next();
			message = line.nextLine().trim();
		} catch (NoSuchElementException e) {
			onBadRequest(payload);
			return;
		}
		if (!Server.userToMessages.containsKey(from)) {
			try {
				send("SENDER " + from + " DOES NOT EXIST\n",
						this.rxPacket.getAddress(), this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
		if (!Server.userToMessages.containsKey(to)) {
			try {
				send("RECIPIENT " + to + " DOES NOT EXIST\n",
						this.rxPacket.getAddress(), this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
		Server.userToMessages.get(to).add(
				"FROM " + from + " TO " + to + ":: " + message + "\n");
		Server.lastUpdated.put(from, System.currentTimeMillis());
		try {
			send("MESSAGE ADDED TO QUEUE\n", this.rxPacket.getAddress(),
					this.rxPacket.getPort());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * SENDG format: SENDG sendingUser receivingGroup message adds message to
	 * the queues of all users in receivingGroup sends error message if user or
	 * group does not exist
	 * 
	 * @param payload
	 */
	private void onSendGRequested(String payload) {
		String to, from, message;
		try {
			Scanner line = new Scanner(payload);
			line.next();
			from = line.next();
			to = line.next();
			message = line.nextLine().trim();
		} catch (NoSuchElementException e) {
			onBadRequest(payload);
			return;
		}
		if (!Server.userToMessages.containsKey(from)) {
			try {
				send("SENDER " + from + " DOES NOT EXIST\n",
						this.rxPacket.getAddress(), this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
		if (!Server.groupToUsers.containsKey(to)) {
			try {
				send("RECIPIENT GROUP " + to + " DOES NOT EXIST\n",
						this.rxPacket.getAddress(), this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
		for (String member : Server.groupToUsers.get(to)) {
			Server.userToMessages.get(member).add(
					"FROM " + from + " TO " + to + ":: " + message + "\n");
		}
		Server.lastUpdated.put(from, System.currentTimeMillis());
		try {
			send("MESSAGE ADDED TO QUEUE\n", this.rxPacket.getAddress(),
					this.rxPacket.getPort());
		} catch (IOException e) {
			e.printStackTrace();
		}
		Server.lastUpdated.put(from, System.currentTimeMillis());
	}

	/**
	 * JOIN format: JOIN username groupname Adds user to group. If group does
	 * not exist, group is created. Sends error message if username does not
	 * exist.
	 * 
	 * @param payload
	 */
	private void onJoinRequested(String payload) {
		String username;
		String groupname;
		try {
			Scanner line = new Scanner(payload);
			line.next();
			username = line.next();
			groupname = line.next();
		} catch (NoSuchElementException e) {
			onBadRequest(payload);
			return;
		}
		if (!Server.userToMessages.containsKey(username)) {
			try {
				send("USER " + username + " DOES NOT EXIST\n",
						this.rxPacket.getAddress(), this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
		if (!Server.groupToUsers.containsKey(groupname)) {
			Server.groupToUsers.put(groupname, new LinkedList<String>());
		}
		Server.groupToUsers.get(groupname).add(username);
		try {
			send("USER " + username + " ADDED TO GROUP " + groupname + "\n",
					this.rxPacket.getAddress(), this.rxPacket.getPort());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * POLL format: POLL username Sends all messages in the queue of user.
	 * Removes messages from queue. Sends an error is username does not exist.
	 * 
	 * @param payload
	 */
	private void onPollRequested(String payload) {
		String username;
		try {
			Scanner line = new Scanner(payload);
			line.next();
			username = line.next();
		} catch (NoSuchElementException e) {
			onBadRequest(payload);
			return;
		}
		if (!Server.userToMessages.containsKey(username)) {
			try {
				send("USER " + username + " DOES NOT EXIST\n",
						this.rxPacket.getAddress(), this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
		Server.lastUpdated.put(username, System.currentTimeMillis());
		while (!Server.userToMessages.get(username).isEmpty()) {
			try {
				send(Server.userToMessages.get(username).poll(),
						this.rxPacket.getAddress(), this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		Server.lastUpdated.put(username, System.currentTimeMillis());
	}

	/**
	 * SHUTDOWN format: SHUTDOWN closes the socket if the request comes from
	 * localhost. First waits for all other threads to finish.
	 * 
	 * @param payload
	 */
	private void onShutdownRequested(String payload) {
		// the string is the address that I found packets sent via netcat to be
		// coming from.
		if (this.rxPacket.getAddress().toString().equals("/0:0:0:0:0:0:0:1"))
			;
		{
			for (WorkerThread t : Server.threads) {
				try {
					if (t != Thread.currentThread())
						t.join();
				} catch (InterruptedException e) {
				}
			}
			socket.close();
		}
	}

	private void onBadRequest(String payload) {
		try {
			send("BAD REQUEST\n", this.rxPacket.getAddress(),
					this.rxPacket.getPort());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
