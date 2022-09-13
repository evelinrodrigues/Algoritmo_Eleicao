package com.eleicao;

import java.util.Arrays;
import java.util.List;
import java.net.*;
import java.io.*;

public class Processos {

    public static int T1 = 5000;
    public static int T2 = 10000;
    public static int T3 = 9000;
    public static int coordenadorEleito = -1;
    public static List<Integer> IDs = Arrays.asList(6793, 6792, 6791, 6790);
    public static List<Integer> IDsProcessosDisponiveis = Arrays.asList(6793, 6792, 6791, 6790);
    public static List<Integer> IDsProcessosAtivos = Arrays.asList();
    public static int meuID;
    public static MulticastSocket socket_multicast;

    /*
        As mensagens devem ser do tipo "ID:cod:Conteúdo"
        ID é o ID do processo
        cod é o tipo de mensagem: A (mensagem de chegada de novo processo), B (mensagem de resposta à chegada de um novo processo), 
                                  C (mensagem de eleição), D (mensagem de resposta à eleição), E (mensagem de coordenador) 
                                  e F (mensagem de olá)
    */

    public static void setMeuID(int indexID){
        meuID = IDs.get(indexID);
        //Pode tirar essa chamada e colocar em outro lugar, mas aqui resolve o problema dele precisar entrar como ativo e tirar do disponível 
        processoEntrou(indexID);
    }

    public static void enviaMensagemMulticast(String mensagem){
        try {
			InetAddress group = InetAddress.getByName("228.5.6.7");
            byte [] m = mensagem.getBytes();
            socket_multicast = new MulticastSocket(6789);
		    DatagramPacket messageOut = new DatagramPacket(m, m.length, group, 6789);
		    socket_multicast.send(messageOut);
		}catch (SocketException e){System.out.println("Socket: " + e.getMessage());
		}catch (IOException e){System.out.println("IO: " + e.getMessage());
		}finally {if(socket_multicast != null) socket_multicast.close();}
    }

    public static void enviaMensagemEntreiGrupo() {
        
    }
    
    // Função para retirada da lista de IDs Disponíveis e adição à lista de Ativos
    public static void processoEntrou(int idPossivelmenteNovo){
        for (int i=0; i<IDsProcessosDisponiveis.size();i++){
            if (IDsProcessosDisponiveis.get(i) == idPossivelmenteNovo){
                IDsProcessosDisponiveis.remove(i);
                IDsProcessosAtivos.add(idPossivelmenteNovo);
                break;
            }
        }
    }

    // Função para retirada da lista de IDs ativos e adição à lista de Disponíveis
    public static void processoSaiu(int idDeQuemMorreu){
        for (int i=0; i<IDsProcessosAtivos.size();i++){
            if (IDsProcessosAtivos.get(i) == idDeQuemMorreu){
                IDsProcessosAtivos.remove(i);
                IDsProcessosDisponiveis.add(idDeQuemMorreu);
                break;
            }
        }
    }

    public static void recebeMensagemDeId() {
        while (!IDsProcessosDisponiveis.isEmpty()){
            try{
                byte[] buffer = new byte[1000];
                DatagramPacket messageIn = new DatagramPacket(buffer, buffer.length);
                socket_multicast.receive(messageIn);
                Mensagem mensagem = new Mensagem(messageIn.getData());
                if(mensagem.cod.equals("A") || mensagem.cod.equals("B")){
                    //Pode ser que ele já anotou que esse entrou, mas essa função cuida desse caso
                    processoEntrou(mensagem.ID);
                }
            }catch(Exception e){
                System.out.println("Não teve sucesso em esperar pelos IDs\nSocket: " + e.getMessage());
            }
        }
    }


    public static void escutaMensagemOlaCoordenador() {
        while(true)
        {
            try{
                byte[] buffer = new byte[1000];
                DatagramPacket messageIn = new DatagramPacket(buffer, buffer.length);
                socket_multicast.setSoTimeout(T3);
                socket_multicast.receive(messageIn);
                Mensagem mensagem = new Mensagem(messageIn.getData());
                if(mensagem.cod.equals("F")) {
                    return;
                }
                System.out.println(mensagem.conteudo);
            }catch(Exception e){
                return;
            }
        }
    }

    public static void escutaPedidosEleicao() {
        try{
            byte[] buffer = new byte[1000];
            DatagramPacket messageIn = new DatagramPacket(buffer, buffer.length);
            socket_multicast.setSoTimeout(T3);
            socket_multicast.receive(messageIn);
            Mensagem mensagem = new Mensagem(messageIn.getData());
            int id = mensagem.ID;
            if(mensagem.cod.equals("C")) {
                UDPClient.inicia(meuID + ":D:Estou ativo", id);
            }
            else {
                return;
            }
            System.out.println(mensagem.conteudo);
        }catch(Exception e){
            return;
        }
    }

    private static int idMaiorIDDisponivel(){
        for(Integer id : IDsProcessosDisponiveis){
            if(id != -1) return id;
        }
        return -1;
    }

    private static boolean escutaMensagemCoordenador(){
        try{
            byte[] buffer = new byte[1000];
            DatagramPacket messageIn = new DatagramPacket(buffer, buffer.length);
            socket_multicast.setSoTimeout(T3);
            socket_multicast.receive(messageIn);
            Mensagem mensagem = new Mensagem(messageIn.getData());
            coordenadorEleito = mensagem.ID;
            System.out.println(mensagem.conteudo);
        }catch(Exception e){
            return false;
        }
        return true;
    }

    public static void comecaEleicao() {
        if(meuID == idMaiorIDDisponivel()){
            seDeclaraCoordenador();
        }else{
            if(!enviaPedidoEleicaoMaiorId()){
                seDeclaraCoordenador();
            }else{
                if(escutaMensagemCoordenador()){
                    comecaEleicao();
                }
            }
        }
    }


    public static void seDeclaraCoordenador() {
        enviaMensagemCoordenadorEleito();
    }

    public static void enviaMensagemCoordenadorEleito() {
        enviaMensagemMulticast("E:" + meuID + "Novo coordenador eleito");
        coordenadorEleito = meuID;
    }

    public static void enviaMensagemOlaCoordenador() {

    }

    public static boolean enviaPedidoEleicaoMaiorId() {
        for(Integer id : IDsProcessosDisponiveis){
            if(id != meuID){
                UDPClient.inicia("", id);
                if(!UDPServer.inicia()){
                    return false;
                }
            } else {
                break;
            }
        }
        return true;
    }

    public static class Mensagem{
        public int ID;
        public String cod;
        public String conteudo;

        public Mensagem(byte[] entrada){
            String txtEntrada = new String(entrada);
            String[] partes = txtEntrada.split(":");
            ID = Integer.parseInt(partes[0]);
            cod = partes[1];
            conteudo = partes[2];
        }
    }

    public static class UDPClient{

        private static void inicia(String mensagem, int serverPort){ 
            DatagramSocket aSocket = null;
            try {
                aSocket = new DatagramSocket();    
                byte [] m = mensagem.getBytes();
                InetAddress aHost = InetAddress.getByName("localhost");	                                                 
                DatagramPacket request = new DatagramPacket(m,  mensagem.length(), aHost, serverPort);
                aSocket.send(request);			                        
                byte[] buffer = new byte[1000];
                DatagramPacket reply = new DatagramPacket(buffer, buffer.length);	
                aSocket.receive(reply);
                System.out.println("Reply: " + new String(reply.getData()));	
            }catch (SocketException e){System.out.println("Socket: " + e.getMessage());
            }catch (IOException e){System.out.println("IO: " + e.getMessage());
            }finally {if(aSocket != null) aSocket.close();}
        }		      	
    }
    public static class UDPServer{

        public static boolean inicia(){ 
            DatagramSocket aSocket = null;
            try{
                aSocket = new DatagramSocket(meuID);
                aSocket.setSoTimeout(T2);
                byte[] buffer = new byte[1000];
                DatagramPacket request = new DatagramPacket(buffer, buffer.length); 
                aSocket.receive(request);     
                return true;
            }catch (SocketException e){System.out.println("Socket: " + e.getMessage());
            }catch (IOException e) {System.out.println("IO: " + e.getMessage());
            }finally {if(aSocket != null) aSocket.close();}
            return false;
        }
    }
}

class EscutaPedidosDeEleicaoThread extends Thread {
    public void EscutaPedidosDeEleicao() {
        this.start();
    }
    public void run () {
        try{
            Processos.escutaPedidosEleicao();
        }
        catch(Exception e) {
            System.out.println("readline:" + e.getMessage());
        }


    }
}
