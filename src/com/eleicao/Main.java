import java.net.*;
import java.io.*;

public class Main {

  public static void main(String[] args) {
    inicia(args[0]);
    EscutaPedidosDeEleicaoThread esc = new EscutaPedidosDeEleicaoThread();
    Processos.desejaComecarEleicao();
    if(Processos.escolha == 1){
      Processos.comecaEleicao();
    }
    while (true) {
      if (Processos.meuID == Processos.coordenadorEleito) {
        Processos.enviaMensagemOlaCoordenador();
      } else if (Processos.coordenadorEleito != -1 || Processos.escolha == 0) {
        Processos.escutaMensagemCoordenador(0);
        Processos.escutaMensagemOlaCoordenador();
      }
      try {
        Thread.sleep(1000);
      } catch (Exception e) {}
    }
  }

  public static void inicia(String arg) {
    try {
      Processos.setMeuID(Integer.parseInt(arg));
      InetAddress group = InetAddress.getByName("228.5.6.7");
      Processos.socket_multicast = new MulticastSocket(7000);
      Processos.socket_multicast.joinGroup(group);
    } catch (SocketException e) {
      System.out.println("Socket: " + e.getMessage());
    } catch (IOException e) {
      System.out.println("IO: " + e.getMessage());
    }
    Processos.enviaMensagemEntreiGrupo();
    Processos.recebeMensagemDeId();
  }
}
