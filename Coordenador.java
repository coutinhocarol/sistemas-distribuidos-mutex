import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Coordenador {
    private final Queue<Integer> filaPedidos = new LinkedList<>(); //Armazena os IDs das threads que pediram acesso à região crítica
    private final Map<Integer, Socket> sockets = new HashMap<>(); //Armazena os sockets de comunicação com cada thread, usando o ID da thread como chave
    private final Map<Integer, Integer> contagemAtendimentos = new HashMap<>(); //Registra quantas vezes cada thread foi atendida
    private final int porta;

    public Coordenador(int porta) {
        this.porta = porta;
    }

    public void iniciar() {
        try (ServerSocket serverSocket = new ServerSocket(porta); 
            Scanner scanner = new Scanner(System.in)) { //O coordenador cria um ServerSocket que ficará ouvindo conexões de entrada na porta especificada
            System.out.println("Coordenador iniciado na porta " + porta);
            
            // Thread única que ficará executando em loop para aceitar conexões de novas threads
            new Thread(() -> {
                while (true) {
                    try {
                        Socket socket = serverSocket.accept(); //Espera até que uma thread tente se conectar ao coordenador. Quando a conexão é aceita, o Socket é obtido, permitindo a comunicação com a thread.
                        DataInputStream input = new DataInputStream(socket.getInputStream()); //DataInputStream é usado para ler dados do socket. Aqui, ele lê um int que representa o ID da thread que se conectou.
                        int threadId = input.readInt();
    
                        synchronized (sockets) { 
                            sockets.put(threadId, socket);
                            contagemAtendimentos.put(threadId, 0); //Coloca 0 como valor nessa linha para indicar que a thread ainda não foi atendido, apenas conectou
                        }
                        System.out.println("Thread conectada: " + threadId);
    
                        // Inicia thread para tratar mensagens da thread
                        tratarMensagens(threadId, socket);
                    } catch (IOException e) {
                        System.err.println("Erro ao aceitar conexão: " + e.getMessage());
                    }
                }
            }).start();
    
            // Interface para comandos no terminal: mantém o programa aguardando comandos do usuário até que o comando "sair" seja dado
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

    private void tratarMensagens(int threadId, Socket socket) { // Cada conexão coordenador <> thread, criará uma nova thread própria para tratar as mensagens dessa conexão, garantindo que a comunicação entre o coordenador e as threads não bloqueie a execução do servidor
        new Thread(() -> { 
            try (DataInputStream input = new DataInputStream(socket.getInputStream()); 
                DataOutputStream output = new DataOutputStream(socket.getOutputStream())) { 

                while (true) { // A thread pode enviar múltiplas mensagens para o coordenador na mesma conexão, e é por isso que esse loop é necessário, pois vai ser uma execução para cada mensagem
                    String mensagem = input.readUTF(); // Coordenador fica aguardando até que a thread envie uma mensagem via socket
                    if (mensagem.startsWith("1")) { // Thread solicita acesso a região crítica
                        log(mensagem);
                        synchronized (filaPedidos) { 
                            filaPedidos.add(threadId); // Thread é adicionada na fila de acesso a região crítica
                        } 
                        
                        // A thread aguarda até ser a primeiro na fila
                        synchronized (filaPedidos) {
                            try {
                                while (filaPedidos.peek() != threadId) {
                                    filaPedidos.wait();  // Espera enquanto não for o primeiro da fila
                                }

                                // Depois de sair do loop, é a vez da thread

                                String mensagem_grant = new Mensagem(2, Integer.parseInt(Thread.currentThread().getName().split("-")[1])).mensagemCodificada;
                                output.writeUTF(mensagem_grant);
                                log(mensagem_grant); 
                                synchronized (contagemAtendimentos) {
                                    contagemAtendimentos.put(threadId, contagemAtendimentos.get(threadId) + 1);
                                }
                            } catch (InterruptedException e) {
                                System.err.println("Erro ao aguardar acesso à região crítica: " + e.getMessage());
                            }
                        }
                    } else if (mensagem.startsWith("3")) {
                        log(mensagem);
                        synchronized (filaPedidos) {
                            filaPedidos.poll(); // Remove a thread da fila
                            filaPedidos.notify();  // Notifica todos as threads aguardando a fila
                        }   
                    }
                }
            } catch (IOException e) {
                if (socket.isClosed()) {
                    System.out.println("Conexão com thread " + threadId + " fechada.");
                } else {
                    System.err.println("Erro na thread " + threadId + ": " + e.getMessage());
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
            contagemAtendimentos.forEach((threadId, count) -> {
                System.out.println("Thread " + threadId + " foi atendido " + count + " vezes.");
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
        String tipo_mensagem = mensagem.split("\\|")[0];
        String thread = mensagem.split("\\|")[1]; 
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        String timestamp = sdf.format(new Date());

        if (tipo_mensagem.equals("1")) {
            System.out.println("[" + timestamp + "] REQUEST recebido da thread " + thread);
        } else if (tipo_mensagem.equals("2")) {
            System.out.println("[" + timestamp + "] GRANT enviado para thread " + thread);
        } else if (tipo_mensagem.equals("3")) {
            System.out.println("[" + timestamp + "] RELEASE recebido da thread " + thread);
        }
    }
    
    public static void main(String[] args) {
        new Coordenador(12345).iniciar();
    }
}

