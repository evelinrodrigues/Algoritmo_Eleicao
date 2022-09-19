import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.net.*;
import java.io.*;
import java.util.Scanner;

public class Processos {

  public static int T1 = 5000;
  public static int T2 = 10000;
  public static int T3 = 9000;
  public static int coordenadorEleito = -1;
  public static List<Integer> IDs = new ArrayList<Integer>(Arrays.asList(6793, 6792, 6791, 6790));
  public static List<Integer> IDsProcessosAtivos = new ArrayList<Integer>();
  public static int meuID;
  public static MulticastSocket socket_multicast;
  public static boolean isMinhaEleicaoOcorrendo;
  public static UDPServer serverPedidoEleicao;
  public static UDPServer serverRespostaEleicao;
  public static int escolha = -1;
  public static Scanner sc = new Scanner(System.in);

  /*
   * As mensagens devem ser do tipo "ID:cod:Conteúdo"
   * ID é o ID do processo
   * cod é o tipo de mensagem: A (mensagem de chegada de novo processo), B
   * (mensagem de resposta à chegada de um novo processo),
   * C (mensagem de eleição), D (mensagem de resposta à eleição), E (mensagem de
   * coordenador)
   * e F (mensagem de olá)
   */

  public static void setMeuID(int indexID) {
    meuID = IDs.get(indexID);
    serverPedidoEleicao = new UDPServer(meuID);
    serverRespostaEleicao = new UDPServer(meuID + 100, T2);
    processoEntrou(indexID); // se coloca na lista de processos ativos
  }

  public static void enviaMensagemMulticast(String mensagem) {
    try {
      InetAddress group = InetAddress.getByName("228.5.6.7");
      byte[] m = mensagem.getBytes();
      DatagramPacket messageOut = new DatagramPacket(m, m.length, group, 7000);
      System.out.println("Enviado multicast: " + mensagem);
      socket_multicast.send(messageOut);
    } catch (SocketException e) {
      System.out.println("Socket multicast: " + e.getMessage());
    } catch (IOException e) {
      System.out.println("IO: " + e.getMessage());
    }
  }

  public static void enviaMensagemEntreiGrupo() {
    enviaMensagemMulticast(Integer.toString(meuID) + ":A:" + "Entrei no grupo");
  }

  public static void processoEntrou(int idPossivelmenteNovo) {
    for (int i = 0; i < IDs.size(); i++) {
      if (IDs.get(i) == idPossivelmenteNovo) {
        IDsProcessosAtivos.add(idPossivelmenteNovo);
        break;
      }
    }
  }

  public static void processoSaiu(int idDeQuemMorreu) {
    for (int i = 0; i < IDsProcessosAtivos.size(); i++) {
      if (IDsProcessosAtivos.get(i) == idDeQuemMorreu) {
        IDsProcessosAtivos.remove(i);
        break;
      }
    }
  }

  public static void recebeMensagemDeId() {
    while (IDsProcessosAtivos.size() < 3) { // como que aqui é <3 se ele se add na lista de processos ativos - verificar
      try {
        byte[] buffer = new byte[1000];
        DatagramPacket messageIn = new DatagramPacket(buffer, buffer.length);
        socket_multicast.receive(messageIn);
        Mensagem mensagem = new Mensagem(messageIn.getData());
        if (mensagem.ID != meuID) {
          if (mensagem.cod.equals("A")) { //mensagem de que entrou um novo processo
            processoEntrou(mensagem.ID); //add o processo na lista dos disponíveis
            System.out.println("Recebido de " + mensagem.ID + ": " + mensagem);
            enviaMensagemMulticast(meuID + ":B:" + "Bem-vinda");
          } else if (mensagem.cod.equals("B")) {
            boolean existe = false;
            for (Integer i : IDsProcessosAtivos) { //verifica se o processo já está na  lista
              if (i == mensagem.ID) {
                existe = true;
                break;
              }
            }
            if (!existe) {
              IDsProcessosAtivos.add(mensagem.ID);
              System.out.println("Recebido de " + mensagem.ID + ": " + mensagem);
            }
          }
        }
      } catch (Exception e) {
        System.out.println(e);
        System.exit(0);
      }
    }
    IDsProcessosAtivos = new ArrayList<Integer>(Arrays.asList(6793, 6792, 6791, 6790)); //para ordenar os processos
  }

  public static void retiraCoordenador() {
    if (coordenadorEleito == -1) {
      return;
    }

    for (int i = 0; i < IDsProcessosAtivos.size(); i++) {
      if (IDsProcessosAtivos.get(i) == coordenadorEleito) {
        IDsProcessosAtivos.remove(i);
        break;
      }
    }
    coordenadorEleito = -1;
  }

  public static void desejaComecarEleicao(){
    System.out.println("Deseja comecar uma eleicao? 0 nao  1 sim: ");
    escolha = sc.nextInt();
  }

  public static void escutaMensagemOlaCoordenador() {
    while (true) {
      try {
        byte[] buffer = new byte[1000];
        DatagramPacket messageIn = new DatagramPacket(buffer, buffer.length);
        socket_multicast.setSoTimeout(T3);
        socket_multicast.receive(messageIn);
        Mensagem mensagem = new Mensagem(messageIn.getData());
        System.out.println("Recebido de " + mensagem.ID + ": " + mensagem);
      } catch (Exception e) { //deu time out
        retiraCoordenador();
        desejaComecarEleicao();
        if (escolha == 1) {
          comecaEleicao();
        }
        return;
      }
    }
  }

  public static void escutaPedidosEleicao() {
    UDPServer server = serverPedidoEleicao;
    try {
      while (true) {
        if (server.inicia()) { //se recebeu um pedido
          Mensagem mensagem = new Mensagem(server.request.getData());
          //System.out.println(mensagem);
          int id = mensagem.ID;
          EscutaThread escuta = new EscutaThread(id, mensagem); //faz uma thread pq ele pode receber outros pedidos enquanto responde
        }
      }
    } catch (Exception e) {
      System.out.println(e);
      return;
    }finally{
      server.fecha();
    }
  }

  static class EscutaThread extends Thread {
    Mensagem mensagem;
    int id;

    public EscutaThread(int id, Mensagem mensagem) {
      this.mensagem = mensagem;
      this.id = id;
      this.start();
    }

    public void run() {
      if (!isMinhaEleicaoOcorrendo && coordenadorEleito != -1) {
        retiraCoordenador();
      }
      if (escolha != 0) {
        UDPClient client = new UDPClient();
        client.inicia(meuID + ":D:Estou ativo", id + 100);
        client.fecha();
        if (!isMinhaEleicaoOcorrendo && coordenadorEleito != -1 && escolha == 1) {
          ComecaoEleicaoThread comecaoEleicao = new ComecaoEleicaoThread();
        }
      }
    }
  }

  private static int idMaiorIDDisponivel() {
    int maior = -1;
    for (Integer id : IDsProcessosAtivos) {
      if (id > maior){
        maior = id;
      }
    }
    return maior;
  }

  public static boolean escutaMensagemCoordenador(int timeout) {
    try {
      byte[] buffer = new byte[1000];
      DatagramPacket messageIn = new DatagramPacket(buffer, buffer.length);
      socket_multicast.setSoTimeout(timeout);
      socket_multicast.receive(messageIn);
      Mensagem mensagem = new Mensagem(messageIn.getData());
      if(mensagem.cod.equals("F")){ //não seria de colocar o e aqui também? verificar
        isMinhaEleicaoOcorrendo = false;
        coordenadorEleito = mensagem.ID;
        System.out.println("Recebido de " + mensagem.ID + ": " + mensagem);
        escolha = -1;
      }else{
        return escutaMensagemCoordenador(timeout);
      }
      
    } catch (Exception e) {
      System.out.println(e);
      return false;
    }
    return true;
  }

  public static void comecaEleicao() {
    if (isMinhaEleicaoOcorrendo)
      return;
    System.out.println("Estou começando uma eleição.");
    isMinhaEleicaoOcorrendo = true;
    if (meuID > idMaiorIDDisponivel()) {
      seDeclaraCoordenador();
    } else {
      if (!enviaPedidoEleicaoMaiorId()) {
        seDeclaraCoordenador();
      } else {
        if (!escutaMensagemCoordenador(T3)) {
          isMinhaEleicaoOcorrendo = false;
          comecaEleicao();
        }
      }
    }
  }

  public static void seDeclaraCoordenador() {
    enviaMensagemCoordenadorEleito();
    escolha = -1;
  }

  public static void enviaMensagemCoordenadorEleito() {
    enviaMensagemMulticast(meuID + ":E:" + "Novo coordenador eleito");
    coordenadorEleito = meuID;
    isMinhaEleicaoOcorrendo = false;
  }

  public static void enviaMensagemOlaCoordenador() {
    while (true) {
      enviaMensagemMulticast(meuID + ":F:" + "Olá, sou coordenador");
      try {
        Thread.sleep(T1);
      } catch (Exception e) {
      }
    }
  }

  public static boolean enviaPedidoEleicaoMaiorId() {
    UDPServer server = serverRespostaEleicao;
    UDPClient client = new UDPClient();
    for (Integer id : IDsProcessosAtivos) {
      if (id > meuID) {
        client.inicia(meuID + ":C:Vote em mim, quero ser coordenador", id);
        if (server.inicia()) { //se alguem responder que esta ativo
          return true;
        }
      }
    }
    server.fecha();
    client.fecha();
    return false;
  }

  public static class Mensagem {
    public int ID;
    public String cod;
    public String conteudo;
    private String txtEntrada;

    public Mensagem(byte[] entrada) {
      txtEntrada = new String(entrada);
      String[] partes = txtEntrada.split(":");
      ID = Integer.parseInt(partes[0]);
      cod = partes[1];
      conteudo = partes[2];
    }

    public String toString() {
      return txtEntrada;
    }
  }

  public static class UDPClient {

    public DatagramSocket aSocket;

    public UDPClient(){
      try {
        aSocket = new DatagramSocket();
      } catch (SocketException e) {
        System.out.println(e);
      }
    }

    public void inicia(String mensagem, int serverPort) {
      try {
        byte[] m = mensagem.getBytes();
        InetAddress aHost = InetAddress.getByName("localhost");
        DatagramPacket request = new DatagramPacket(m, mensagem.length(), aHost, serverPort);
        System.out.println("Enviado para "+ serverPort+ " : " + mensagem);
        aSocket.send(request);
      } catch (SocketException e) {
        System.out.println("Socket UDP Client: " + e.getMessage());
      } catch (IOException e) {
        System.out.println("IO: " + e.getMessage());
      } catch (Exception e) {
        System.out.println(e);
      }
    }

    public void fecha(){
      if(aSocket != null) aSocket.close();
    }
  }

  public static class UDPServer {

    public DatagramPacket request;
    public DatagramSocket aSocket;
    public int porta;
    public int timeout;

    public UDPServer(int porta, int timeout){
      this.porta = porta;
      this.timeout = timeout;
      inicializa();
    }

    public UDPServer(int porta){
      this.porta = porta;
      this.timeout = 0;
      inicializa();
    }

    public void inicializa(){
      try {
        aSocket = new DatagramSocket(porta);
        if(timeout > 0) aSocket.setSoTimeout(timeout);
      } catch (SocketException e) {
        System.out.println(e);
      }
    }

    public boolean inicia() {
      try {
        byte[] buffer = new byte[1000];
        request = new DatagramPacket(buffer, buffer.length);
        aSocket.receive(request);
        Mensagem msg = new Mensagem(request.getData());
        System.out.println("Recebido de " +msg.ID+ ": " + msg);
        return true; //recebeu a mensagem dentro do tempo
      } catch (SocketException e) {
        System.out.println("Socket UDPServer: " + e.getMessage());
      } catch (IOException e) {
        System.out.println("IO: " + e.getMessage());
      }
      return false; //não recebeu a mensagem dentro do tempo
    }

    public void fecha(){
      if(aSocket != null) aSocket.close();
    }
  }
}

class EscutaPedidosDeEleicaoThread extends Thread {
  public EscutaPedidosDeEleicaoThread() {
    this.start();
  }

  public void run() {
    try {
      Processos.escutaPedidosEleicao();
    } catch (Exception e) {
      System.out.println("readline:" + e.getMessage());
    }
  }
}

class ComecaoEleicaoThread extends Thread {
  public ComecaoEleicaoThread() {
    this.start();
  }

  public void run() {
    try {
      Processos.comecaEleicao();
    } catch (Exception e) {
      System.out.println("readline:" + e.getMessage());
    }
  }
}
