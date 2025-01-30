import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Cliente extends Thread {
    private int id; // Identificador único da thread
    private final String ipCoordenador; // IP do coordenador
    private final int portaCoordenador; // Porta do coordenador
    private final int repeticoes; // Quantas vezes a thread acessará a região crítica

    public Cliente(String ipCoordenador, int portaCoordenador, int repeticoes) {
        this.ipCoordenador = ipCoordenador;
        this.portaCoordenador = portaCoordenador;
        this.repeticoes = repeticoes;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(ipCoordenador, portaCoordenador);
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            DataInputStream input = new DataInputStream(socket.getInputStream())) {
            
            this.id = Integer.parseInt(Thread.currentThread().getName().split("-")[1]);

            output.writeInt(id);  // Envia o ID da thread ao coordenador 

            for (int i = 0; i < repeticoes; i++) {
                // Envia REQUEST
                output.writeUTF(new Mensagem(1, id).mensagemCodificada); //Usada para enviar uma string no formato UTF-8 através do fluxo de dados
                System.out.println("[" + id + "] REQUEST enviado");

                // Aguarda GRANT
                String resposta = input.readUTF();

                if (resposta.startsWith("2")) {   
                    System.out.println("[" + id + "] GRANT recebido");

                    // Região crítica
                    escreverArquivo();
                    Thread.sleep(5 * 1000); //Ficará 5 segundos na região crítica

                    // Envia RELEASE
                    output.writeUTF(new Mensagem(3, id).mensagemCodificada);
                    System.out.println("[" + id + "] RELEASE enviado");
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void escreverArquivo() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("resultado.txt", true))) {
            String timestamp = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
            writer.write("Thread " + id + " - " + timestamp + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
