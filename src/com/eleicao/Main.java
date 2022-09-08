package com.eleicao;
import java.net.*;

public class Main {

    public static MulticastSocket socket_multicast;
    public static int meuID;
    public static int coordenadorEleito;

    public static void main(String[] args) { // primeiro arg é a posição do ID
	// write your code here
        inicia(args); 
        boolean a = true;
        while (a){
            Processos.começaEleicao();
            if (meuID == coordenadorEleito){
                Processos.enviaMensagemOlaCoordenador();
            } else{
                Processos.escutaPedidosEleicao();
                Processos.escutaMensagemOlaCoordenador();
            }
        }
    }

    public static void inicia(String[] args){
        meuID = Processos.IDs[Integer.parseInt(args[0])]; 
        // ******** DUVIDA ************//
        // a classe MulticastSocket já cria um novo socket, então eu chamo a classe ou crio um new MulticastSocket?
        Processos.enviaMensagemEntreiGrupo();
        Processos.recebeMensagemDeId();
        if(Integer.parseInt(args[0]) == 0){ // é o maior processo
            Processos.enviaMensagemOlaCoordenador();
        } else{
            coordenadorEleito = Processos.IDs[0];
            Processos.escutaPedidosEleicao();
            Processos.escutaMensagemOlaCoordenador();
        }

    }
}
