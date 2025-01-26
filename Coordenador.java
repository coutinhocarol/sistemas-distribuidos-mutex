import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Coordenador {
    private final Queue<Integer> filaPedidos = new LinkedList<>(); //Armazena os IDs dos processos que pediram acesso à região crítica
    private final Map<Integer, Socket> sockets = new HashMap<>(); //Armazena os sockets de comunicação com cada processo, usando o ID do processo como chave.
    private final Map<Integer, Integer> contagemAtendimentos = new HashMap<>(); //Registra quantas vezes cada processo foi atendido
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS"); //Um objeto SimpleDateFormat para formatar timestamps em logs.
    private final int porta;

    public Coordenador(int porta) {
        this.porta = porta;
    }

    public void iniciar() {
        try (ServerSocket serverSocket = new ServerSocket(porta); Scanner scanner = new Scanner(System.in)) { //o coordenador cria um ServerSocket que ficará ouvindo conexões de entrada na porta especificada (porta). O ServerSocket aceita conexões de clientes (os processos).
            System.out.println("Coordenador iniciado na porta " + porta);
    
            // Thread única que ficará executando em loop para aceitar conexões de novos processos. O uso de uma thread separada garante que o coordenador não fique bloqueado esperando por uma conexão, podendo continuar executando outras tarefas (como aguardar comandos no terminal).
            new Thread(() -> {
                while (true) {
                    try {
                        Socket socket = serverSocket.accept(); //serverSocket.accept() espera até que um processo tente se conectar ao coordenador. Quando a conexão é aceita, o Socket é obtido, permitindo a comunicação com o processo.
                        DataInputStream input = new DataInputStream(socket.getInputStream()); //DataInputStream é usado para ler dados do socket. Aqui, ele lê um int que representa o ID do processo que se conectou.
                        int processoId = input.readInt();
    
                        synchronized (sockets) { 
                            sockets.put(processoId, socket);
                            contagemAtendimentos.put(processoId, 0); //Coloca 0 como valor nessa linha para indicar que o processo ainda não foi atendido, apenas conectou
                        }
                        System.out.println("Processo conectado: " + processoId);
    
                        // Inicia thread para tratar mensagens do processo
                        tratarMensagens(processoId, socket);

                        
                    } catch (IOException e) {
                        System.err.println("Erro ao aceitar conexão: " + e.getMessage());
                    }
                }
            }).start();
    
            // Interface para comandos no terminal: mantém o programa aguardando comandos do usuário até que o comando "sair" seja dado.
            while (true) {
                System.out.println("Comandos disponíveis: [fila] [contagem] [sair]");
                String comando = scanner.nextLine();
    
                switch (comando) {
                    case "fila":
                        imprimirFila();
                        break;
                    case "contagem":
                        imprimirContagem();
                        break;
                    case "sair":
                        encerrar();
                        return;
                    default:
                        System.out.println("Comando inválido.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void tratarMensagens(int processoId, Socket socket) { //Cada conexão de processo tem sua própria thread para tratar as mensagens, garantindo que a comunicação entre o coordenador e os processos não bloqueie a execução do servidor.
        new Thread(() -> { 
            try (DataInputStream input = new DataInputStream(socket.getInputStream()); //utilizado para ler as mensagens que o processo envia ao coordenador.
                 DataOutputStream output = new DataOutputStream(socket.getOutputStream())) { //Usado para enviar respostas ao processo

                while (true) { //O processo pode enviar múltiplas mensagens para o coordenador na mesma conexão, e é por isso que esse loop é necessário, pois vai ser uma execução para cada mensagem
                    String mensagem = input.readUTF(); //Coordenador fica aguardando até que o processo envie uma mensagem via socket. 
                    if (mensagem.startsWith("REQUEST")) { //Processo solicita acesso a região crítica
                        log("REQUEST recebido de " + processoId); //Imprime o log da ação de REQUEST na tela
                        synchronized (filaPedidos) { 
                            filaPedidos.add(processoId); //Processo é adicionado na fila de acesso a região crítica
                        }
                        
                        // O processo aguarda até ser o primeiro na fila
                        synchronized (filaPedidos) {
                            try {
                                while (filaPedidos.peek() != processoId) {
                                    filaPedidos.wait();  // Espera enquanto não for o primeiro da fila
                                }
                                // Depois de sair do loop, é a vez do processo
                                output.writeUTF("GRANT"); //Enviando a mensagem de GRANT pro processo
                                log("GRANT enviado para " + processoId); //Imprime o log da ação de GRANT na tela
                                synchronized (contagemAtendimentos) {
                                    contagemAtendimentos.put(processoId, contagemAtendimentos.get(processoId) + 1);
                                }
                            } catch (InterruptedException e) {
                                System.err.println("Erro ao aguardar acesso à região crítica: " + e.getMessage());
                            }
                        }
                    } else if (mensagem.startsWith("RELEASE")) {
                        log("RELEASE recebido de " + processoId);
                        synchronized (filaPedidos) {
                            filaPedidos.poll(); // Remove o processo da fila
                            filaPedidos.notify();  // Notifica todos os processos aguardando a fila
                        }   
                    }
                    

                }
            } catch (IOException e) {
                if (socket.isClosed()) {
                    System.out.println("Conexão com processo " + processoId + " fechada.");
                } else {
                    System.err.println("Erro no processo " + processoId + ": " + e.getMessage());
                }
                
            }

        }).start();
    }

    private void imprimirFila() {
        synchronized (filaPedidos) {
            System.out.println("Fila de pedidos: " + filaPedidos);
        }
    }

    private void imprimirContagem() {
        synchronized (contagemAtendimentos) {
            contagemAtendimentos.forEach((processoId, count) -> {
                System.out.println("Processo " + processoId + " foi atendido " + count + " vezes.");
            });
        }
    }

    private void encerrar() {
        System.out.println("Encerrando coordenador...");
        synchronized (sockets) {
            sockets.values().forEach(socket -> {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.err.println("Erro ao encerrar conexão: " + e.getMessage());
                }
            });
        }
        System.exit(0);
    }

    private void log(String mensagem) {
        String timestamp = sdf.format(new Date());
        System.out.println("[" + timestamp + "] " + mensagem);
    }

    public static void main(String[] args) {
        int porta = 12345;
        Coordenador coordenador = new Coordenador(porta);
        coordenador.iniciar();
    }
}
