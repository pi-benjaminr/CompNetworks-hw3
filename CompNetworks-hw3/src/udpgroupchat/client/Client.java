package udpgroupchat.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;

import udpgroupchat.server.Server;

public class Client {

	String serverAddress;
	int serverPort;

	// constructor
	Client(String serverAddress, int serverPort) {
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
	}

	// start up the server
	public void start() {
		DatagramSocket socket = null;
		try {

			InetSocketAddress serverSocketAddress = new InetSocketAddress(
					serverAddress, serverPort);

			// create a datagram socket, let the OS bind to an ephemeral UDP
			// port. See
			// http://docs.oracle.com/javase/tutorial/networking/datagrams/ for
			// details.
			socket = new DatagramSocket();

			// send "REGISTER" to the server
			//
			// create an UDP packet that we'll send to the server
			String command = "REGISTER";
			DatagramPacket txPacket = new DatagramPacket(command.getBytes(),
					command.length(), serverSocketAddress);
			// send the packet through the socket to the server
			socket.send(txPacket);

			// receive the server's response
			//
			// create an empty UDP packet
			byte[] buf = new byte[Server.MAX_PACKET_SIZE];
			DatagramPacket rxPacket = new DatagramPacket(buf, buf.length);
			// call receive (this will poulate the packet with the received
			// data, and the other endpoint's info)
			socket.receive(rxPacket);
			// print the payload
			String payload = new String(rxPacket.getData(), 0,
					rxPacket.getLength());
			System.out.println(payload);

			// send "SEND Hello everybody!" to the server
			//
			command = "SEND Hello everybody! The time is "
					+ new SimpleDateFormat("HH:mm:ss").format(new Date());
			txPacket = new DatagramPacket(command.getBytes(), command.length(),
					serverSocketAddress);
			socket.send(txPacket);

			// receive the server's response
			//
			socket.receive(rxPacket);
			// print the payload
			payload = new String(rxPacket.getData(), 0, rxPacket.getLength());
			System.out.println(payload);

			// and then keep on receiving packets in an infinite loop
			while (true) {
				socket.receive(rxPacket);
				payload = new String(rxPacket.getData(), 0,
						rxPacket.getLength());
				System.out.println(payload);
			}
		} catch (IOException e) {
			// we jump out here if there's an error
			e.printStackTrace();
		} finally {
			// close the socket
			if(socket!=null && !socket.isClosed())
				socket.close();
		}
	}

	// main method
	public static void main(String[] args) {
		int serverPort = Server.DEFAULT_PORT;
		String serverAddress = "localhost";

		// check if server address and port were given as command line arguments
		if (args.length > 0) {
			serverAddress = args[0];
		}

		if (args.length > 1) {
			try {
				serverPort = Integer.parseInt(args[1]);
			} catch (Exception e) {
				System.out.println("Invalid serverPort specified: " + args[0]);
				System.out.println("Using default serverPort " + serverPort);
			}
		}

		// instantiate the client
		Client client = new Client(serverAddress, serverPort);

		// start it
		//client.start();
		
		client.send("REGISTER alice");
		client.send("REGISTER bob");
		client.send("REGISTER charlie");
		client.send("JOIN alice group1");
		client.send("JOIN bob group1");
		client.send("JOIN charlie group2");
		client.send("SENDU bob alice Hello!");
		client.send("SENDG charlie group1 Hello, group1!");
		client.send("POLL alice");
		client.send("UNREGISTER bob");
		client.send("ABCD INVALID INPUT");
		client.send("SHUTDOWN");
	}
	
	public void send(String command) {
		try {
			InetSocketAddress serverSocketAddress = new InetSocketAddress(
					serverAddress, serverPort);
			DatagramSocket socket = new DatagramSocket();
			DatagramPacket txPacket = new DatagramPacket(command.getBytes(),
					command.length(), serverSocketAddress);
			// send the packet through the socket to the server
			socket.send(txPacket);
			System.out.println("SENT: "+command);

			// receive the server's response
			//
			// create an empty UDP packet
			byte[] buf = new byte[Server.MAX_PACKET_SIZE];
			DatagramPacket rxPacket = new DatagramPacket(buf, buf.length);
			// call receive (this will poulate the packet with the received
			// data, and the other endpoint's info)
			socket.receive(rxPacket);
			// print the payload
			String payload = new String(rxPacket.getData(), 0,
					rxPacket.getLength());
			System.out.println(payload);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
