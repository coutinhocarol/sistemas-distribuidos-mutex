import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Processo extends Thread {
    private final int id; // Identificador único do processo
    private final String ipCoordenador; // IP do coordenador
    private final int portaCoordenador; // Porta do coordenador
    private final int repeticoes; // Quantas vezes o processo acessará a região crítica
    private final int tempoEspera; // Tempo (em segundos) que o processo permanecerá na região crítica antes de liberar o acesso.

    public Processo(int id, String ipCoordenador, int portaCoordenador, int repeticoes, int tempoEspera) {
        this.id = id;
        this.ipCoordenador = ipCoordenador;
        this.portaCoordenador = portaCoordenador;
        this.repeticoes = repeticoes;
        this.tempoEspera = tempoEspera;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(ipCoordenador, portaCoordenador);
             DataOutputStream output = new DataOutputStream(socket.getOutputStream());
             DataInputStream input = new DataInputStream(socket.getInputStream())) {

            // Envia o ID do processo ao coordenador
            output.writeInt(id);

            for (int i = 0; i < repeticoes; i++) {
                // Envia REQUEST
                output.writeUTF("REQUEST");
                System.out.println("[" + id + "] REQUEST enviado.");

                // Aguarda GRANT
                String resposta = input.readUTF();
                if ("GRANT".equals(resposta)) {
                    System.out.println("[" + id + "] GRANT recebido.");

                    // Região crítica
                    escreverArquivo();
                    Thread.sleep(tempoEspera * 1000);

                    // Envia RELEASE
                    output.writeUTF("RELEASE");
                    System.out.println("[" + id + "] RELEASE enviado.");
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void escreverArquivo() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("resultado.txt", true))) {
            String timestamp = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
            writer.write("Processo " + id + " - " + timestamp + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String ipCoordenador = "localhost";
        int portaCoordenador = 12345;
        int repeticoes = 3;
        int tempoEspera = 2;

        // Criação de múltiplos processos (threads)
        for (int i = 1; i <= 2; i++) { // Exemplo: criando 2 threads
            Processo processo = new Processo(i, ipCoordenador, portaCoordenador, repeticoes, tempoEspera);
            processo.start(); // Inicia a execução da thread (chama o método run)
        }
    }
}
