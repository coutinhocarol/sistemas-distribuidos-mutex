public class Main {
    public static void main(String[] args) {
        int repeticoes = 3;
        int n = 2;

        for (int i = 0; i < n; i++) { 
            new Cliente("localhost", 12345, repeticoes).start();
        }
    }
}
