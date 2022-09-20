import java.util.Arrays;
import java.util.ArrayList; 
import java.util.List;
import java.util.Scanner;
import java.net.*;
import java.io.*;

public class Election {

    public static int T1 = 5000;
    public static int T2 = 10000;
    public static int T3 = 9000;
    public static int electedCoordinator = -1;
    public static List<Integer> ids = Arrays.asList(6794, 6793, 6792, 6791);
    public static List<Integer> activesIds = Arrays.asList(0, 0, 0, 0);
    public static int myId;
    public static MulticastSocket socket_multicast;
    public static InetAddress group;


    /*Mensagens:
     * id: A: Entrei (Chegada de um novo processo)
     * id: B: Salve meu id (Mensagem de resposta a novo processo)
     * id: C: Mensagem de Eleiçao (Solicitação para ids maiores)
     * id: D: Estou ativo (Mensagem de resposta a uma solicitação de eleição)
     * id: E: Novo coordenador eleito (Se declara coordenador)
     * id: F: Ola (Mensagem de coordenador avisando que está ativo)
     */

    public static void setMyId(int indexID){
        myId = ids.get(indexID);
        processJoined(myId);
    }

    public static void sendMulticast(String message){
        try {
            byte [] m = message.getBytes();
		    DatagramPacket messageOut = new DatagramPacket(m, m.length, group, 6789);
            socket_multicast.send(messageOut);
		}catch (SocketException e){System.out.println("Socket: " + e.getMessage());
		}catch (IOException e){System.out.println("IO: " + e.getMessage());
		}
    }

    public static void sendMessageJoinedGroup() {
        sendMulticast( myId + ":A: Entrei");
    }

    public static void replyId(){
        sendMulticast( myId +":B: Salve meu id");

    }
    
    // Função para adição à lista de Ativos
    public static void processJoined(int newId){
        for (int i=0; i<activesIds.size();i++){
            if (activesIds.get(i) == newId){
                return;
            }
        }
        for (int i=0; i<activesIds.size();i++){
            if (activesIds.get(i) == 0){
                System.out.println("Adicionei o seguinte id:" +newId);
                activesIds.set(i, newId);
                break;
            }
        }
    }

    //Quando morre, o id volta pra lista de "disponíveis"
    public static void processLeft(int id){
        for (int i=0; i<activesIds.size();i++){
            if (activesIds.get(i) == id){
                activesIds.set(i, 0);
                break;
            }

        }
    }

    public static boolean thereAreIdsAvailable(){
        for(int i=0; i< activesIds.size();i++){
            if (activesIds.get(i) == 0) {
                return true;
            }
        }
        return false;
    }
    public static void getMessageId() {
        byte[] buffer = new byte[1000];
        DatagramPacket messageIn = new DatagramPacket(buffer, buffer.length);
        while (thereAreIdsAvailable()){
            try{
                socket_multicast.receive(messageIn);
                Message message = new Message(messageIn.getData());
                if(message.cod.equals("A")){
                    replyId();
                }
                if(message.cod.equals("A") || message.cod.equals("B")){
                    //Pode ser que ele já anotou que esse entrou, mas essa função cuida desse caso
                    processJoined(message.id); 
                }
                if(!message.cod.equals("A") && !message.cod.equals("B")){
                    //Significa que ele entrou depois de tudo já ter iniciado.
                    return;
                }
            }catch(Exception e){
                System.out.println("Não teve sucesso em esperar pelos IDs\nSocket: " + e.getMessage());
            }
        }
    }


    public static void listenHelloFromCoordinator() {
        boolean coordinatorIsAlive = true;
        
        while(coordinatorIsAlive)
        {
            try{
                socket_multicast.setSoTimeout(T2);
                byte[] buffer = new byte[1000];
                DatagramPacket messageIn = new DatagramPacket(buffer, buffer.length);
                while(true){
                    socket_multicast.receive(messageIn);
                    Message message = new Message(messageIn.getData());
                    if(message.cod.equals("F")) {
                        System.out.println("F: Recebeu Ola");
                        //Se for uma mensagem de "Ola", nós iremos resetar o timer
                        break;
                    }
                }
            }catch(Exception e){
                if (e.getMessage().equals("Receive timed out")){
                    processLeft(electedCoordinator);
                    return;
                }
            }
        }
    }

    public static void listenElectionRequest() {
        DatagramSocket aSocket = null;
        try{
            List<Integer> activesIdsWithout0 = new ArrayList<>();
            for(int i=0; i<activesIds.size(); i++) {
                if(activesIds.get(i) != 0)
                {
                    activesIdsWithout0.add(activesIds.get(i));
                }
            }
            aSocket = new DatagramSocket(myId);
            for(int i=0; i < activesIdsWithout0.size()-1; i++) {
                aSocket.setSoTimeout(T2);
                byte[] buffer = new byte[1000];
                DatagramPacket request = new DatagramPacket(buffer, buffer.length); 
                while(true) {
                    aSocket.receive(request);  
                    Message message = new Message(request.getData());
                    System.out.println("Received: " + message.id + " " + message.content);
                    int id = message.id;
                    if(message.cod.equals("C")) {
                        System.out.println("Enviei: D: Estou ativo");
                        UDPClient.init(myId + ":D:Estou ativo", id);
                        break;
                    }
                }
            }
            aSocket.close();
        }
        catch (SocketException e){
            System.out.println("Socket: " + e.getMessage());
        }
        catch (IOException e) {
            System.out.println("IO: listenElectionRequest(): " + e.getMessage());
        }
        catch(Exception e){
            System.out.println(e.getMessage());
        }
        finally{
            if(aSocket != null) {aSocket.close();}
        }
    }

    public static boolean listenReplyElectionRequest() {
        DatagramSocket aSocket = null;
        try{
            aSocket = new DatagramSocket(myId);
            aSocket.setSoTimeout(T2);
            byte[] buffer = new byte[1000];
            DatagramPacket request = new DatagramPacket(buffer, buffer.length); 
            while(true) {
                aSocket.receive(request);  
                Message message = new Message(request.getData());
                System.out.println("Recebeu: " + message.id + " "+ message.content);
                if(message.cod.equals("D")) {
                    break;
                }
                if(message.cod.equals("C")) {
                    System.out.println("Enviei:" + message.id +"D: Estou ativo");
                    UDPClient.init(myId + ":D:Estou ativo", message.id);
                }

            }
            return true;
        }
        catch (SocketException e){
            System.out.println("Socket: " + e.getMessage());
            return false;
        }
        catch (IOException e) {
            System.out.println("IO: listenReplyElectionRequest(): " + e.getMessage());
            return false;

        }
        catch(Exception e){
            System.out.println(e.getMessage());
            return false;

        }
        finally{
            if(aSocket != null) {aSocket.close();
            }
        }
    }

    private static int biggestIdAvalaible(){
        int biggest = -1;
        for(Integer id : activesIds){
            if(id > biggest) biggest = id;
        }
        return biggest;
    }

    private static boolean listenCoordinatorMessage(){
        try{
            byte[] buffer = new byte[1000];
            DatagramPacket messageIn = new DatagramPacket(buffer, buffer.length);
            socket_multicast.setSoTimeout(T3);
            socket_multicast.receive(messageIn);
            Message message = new Message(messageIn.getData());
            electedCoordinator = message.id;
            System.out.println (message.content);
        }catch(Exception e){
            System.out.println("listenCoordinatorMessage(): " + e.getMessage());
            return false;
        }
        return true;
    }

    public static void startElection() {
        if(myId == biggestIdAvalaible()){
            Election.listenElectionRequest();
            becomeCoordinator();
        }else{
            if(sendRequestElection()){
                becomeCoordinator();
            }else{
                if(!listenCoordinatorMessage()){
                    System.out.println("Começa eleiçao");
                    startElection();
                }
            }
        }
    }


    public static void becomeCoordinator() {
        sendCoordinatorMessage();
    }

    public static void sendCoordinatorMessage() {
        sendMulticast(myId + ":E:" + "Novo coordenador eleito");
        System.out.println("Enviei: Mensagem de eleito");
        electedCoordinator = myId;
    }

    public static void sendHello() {
        try{
            while(true){
                try{
                    Thread.sleep(T1);
                    System.out.println("Enviei um OLA");
                    sendMulticast(myId+":F:Ola");
                }
                catch(Exception e){
                    System.out.println("sendHello()" + e.getMessage());
                }
                
            }
        }
        catch(Exception e){
            System.out.println(e.getMessage());
        }
    }

    public static boolean sendRequestElection() {

        for(Integer id : activesIds ){
            if(id > myId){
                System.out.println("Enviei: Mensagem de eleicao para "+ id);
                UDPClient.init(myId+":C:Mensagem de Eleicao", id);
            } 
        }
        if(listenReplyElectionRequest()){
            return false;
        }
        return true;
    }

    public static class Message{
        public int id;
        public String cod;
        public String content;

        public Message(byte[] initialValue){
            String initialText = new String(initialValue);
            String[] partes = initialText.split(":");
            id = Integer.parseInt(partes[0]);
            cod = partes[1];
            content = partes[2];
        }
    }

    public static class UDPClient{

        private static void init(String mensagem, int serverPort){ 
            DatagramSocket aSocket = null;
            try {
                aSocket = new DatagramSocket();    
                byte [] m = mensagem.getBytes();
                InetAddress aHost = InetAddress.getByName("localhost");	                                                 
                DatagramPacket request = new DatagramPacket(m,  mensagem.length(), aHost, serverPort);
                aSocket.send(request);	
            }catch (SocketException e){System.out.println("Socket: " + e.getMessage());
            }catch (IOException e){System.out.println("IO: " + e.getMessage());
            }finally {if(aSocket != null) aSocket.close();}
        }		      	
    }

}
