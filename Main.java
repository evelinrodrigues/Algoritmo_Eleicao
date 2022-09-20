import java.net.*;
import java.util.Arrays;
import java.io.*;

public class Main {

    public static void main(String[] args) { // primeiro arg é a posição do ID
        init(args[0]); 
        boolean a = true;
        while (a){
            Election.startElection();
            if (Election.myId == Election.electedCoordinator){
                Election.sendHello();
            } else{
                Election.listenElectionRequest();
                Election.listenHelloFromCoordinator();
            }
        }
    }

    public static void init(String arg){
        try {
            Election.group = InetAddress.getByName("228.5.6.7");
            Election.socket_multicast = new MulticastSocket(6789);
            Election.socket_multicast.joinGroup(Election.group);	
		}catch (SocketException e){System.out.println("Socket: " + e.getMessage());
		}catch (IOException e){System.out.println("IO: " + e.getMessage());
        }
        Election.setMyId(Integer.parseInt(arg));
        Election.sendMessageJoinedGroup();
        Election.getMessageId();
        Election.electedCoordinator = Election.ids.get(0);
        if(Integer.parseInt(arg) == 0){
            Election.sendHello();
        } else{
            Election.listenHelloFromCoordinator();
        }

    }
}