package com.eleicao;

import java.net.*;
import java.io.*;
import java.util.Scanner;

public class Multicast{
    public static void main(String args[]){ 
		// args give message contents and destination multicast group (e.g. "228.5.6.7")
		MulticastSocket s =null;
		Scanner sc = new Scanner(System.in);
		try {
			InetAddress group = InetAddress.getByName(args[1]);
			s = new MulticastSocket(6789);
			s.joinGroup(group);
			Connection c = new Connection(s); //multicast
			// unicast
			Unicast u = new Unicast();
			System.out.println(u);

			boolean a = true;
			
			while (a){
				String[] argsUni = new String[2];
				System.out.println("Put your message here: ");
				argsUni[0] = sc.next();
				System.out.println("Uni or Multi?");
				if (sc.nextInt() == 1){
					argsUni[1] = "10.10.6.189";
					//ToDo:
					//UDPClient.main(argsUni);
				} else {
					byte [] m = argsUni[0].getBytes();
					DatagramPacket messageOut = new DatagramPacket(m, m.length, group, 6789);
					s.send(messageOut);	
				}
			}
			
			//s.leaveGroup(group);		
		}catch (SocketException e){System.out.println("Socket: " + e.getMessage());
		}catch (IOException e){System.out.println("IO: " + e.getMessage());
		}finally {if(s != null) s.close();}
	}		      	
	
}


class Connection extends Thread {
	DataInputStream in;
	DataOutputStream out;
	MulticastSocket clientSocket;
	public Connection (MulticastSocket aClientSocket) {
		try {
			clientSocket = aClientSocket;
			this.start();
		} catch(Exception e) {System.out.println("Connection:"+e.getMessage());}
	}
	public void run(){
		try {			                 // an echo server
			byte[] buffer = new byte[1000];
			while(true){
				DatagramPacket messageIn = new DatagramPacket(buffer, buffer.length);
				clientSocket.receive(messageIn);
				System.out.println("Received:" + new String(messageIn.getData()));
			}

		} catch(Exception e) {System.out.println("readline:"+e.getMessage());
		} 

	}
}

class Unicast extends Thread {
	public Unicast () {
		this.start();
	}
	public void run(){
		try {		
			System.out.println("test");	                 // an echo server
			//ToDo:
			//UDPServer.main(null);

		} catch(Exception e) {System.out.println("readline:"+e.getMessage());
		} 

	}
}
