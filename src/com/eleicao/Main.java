package com.eleicao;
import java.net.*;
import java.io.*;

public class Main {

    public static void main(String[] args) { // primeiro arg é a posição do ID
	// write your code here
        inicia(args[0]); 
        boolean a = true;
        while (a){
            Processos.comecaEleicao();
            if (Processos.meuID == Processos.coordenadorEleito){
                Processos.enviaMensagemOlaCoordenador();
            } else{
                EscutaPedidosDeEleicaoThread escutaEleicao = new EscutaPedidosDeEleicaoThread();
                escutaEleicao.run();
                Processos.escutaMensagemOlaCoordenador();
            }
        }
    }

    public static void inicia(String arg){
        try {
            InetAddress group = InetAddress.getByName("228.5.6.7");
            Processos.socket_multicast = new MulticastSocket(6789);
            Processos.socket_multicast.joinGroup(group);	
		}catch (SocketException e){System.out.println("Socket: " + e.getMessage());
		}catch (IOException e){System.out.println("IO: " + e.getMessage());
		}finally {if(Processos.socket_multicast != null) Processos.socket_multicast.close();}
        Processos.setMeuID(Integer.parseInt(arg));
        Processos.enviaMensagemEntreiGrupo();
        Processos.recebeMensagemDeId();
        if(Integer.parseInt(arg) == 0){
            Processos.enviaMensagemOlaCoordenador();
        } else{
            Processos.coordenadorEleito = Processos.IDs.get(0);
            EscutaPedidosDeEleicaoThread escutaEleicao = new EscutaPedidosDeEleicaoThread();
            escutaEleicao.run();
            Processos.escutaMensagemOlaCoordenador();
        }

    }
}
