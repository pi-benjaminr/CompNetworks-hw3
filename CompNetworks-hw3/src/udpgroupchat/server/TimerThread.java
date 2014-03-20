package udpgroupchat.server;

import java.net.DatagramSocket;

public class TimerThread extends Thread {
	
	//Time between deletion cycles, in seconds
	int sleepTime;
	//Time before a user is deleted due to inactivity, in seconds
	int timeToLive;
	
	DatagramSocket socket;
	
	public TimerThread(int time, int ttl,  DatagramSocket socket) {
		this.sleepTime = time;
		this.timeToLive = ttl;
		this.socket = socket;
	}
	
	@Override
	public void run() {
		while(!socket.isClosed()){
			try {
				sleep(sleepTime * 1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//every cycle, delete any user that has been inactive for timeToLive seconds
			for (String key : Server.lastUpdated.keySet()) {
				if (System.currentTimeMillis() - Server.lastUpdated.get(key) > timeToLive*1000) {
					deleteUser(key);
					System.out.println("User "+key+" has timed out.");
				}
			}
			
		}
		
		
		
	}
	
	private static void deleteUser(String username) {
		Server.userToMessages.remove(username);
		for (String key : Server.groupToUsers.keySet()) {
			Server.groupToUsers.get(key).remove(username);
		}
		Server.lastUpdated.remove(username);
		
	}

}
