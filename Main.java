import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        int repeticoes = 4;
        int n = 3;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("resultado.txt", false))) {
            writer.write(""); 
        } catch (IOException e) {
            System.err.println("Erro ao limpar o arquivo de resultado: " + e.getMessage());
        }

        for (int i = 0; i < n; i++) { 
            new Cliente("localhost", 12345, repeticoes).start();
        }
    }
}
