public class Mensagem {
    public final String mensagemCodificada;

    public Mensagem(int codigo, int idThread) {
        String base = codigo + "|" + idThread + "|";
        int zerosRestantes = 10 - base.length();
    
        StringBuilder mensagemFinal = new StringBuilder(base);
        for (int i = 0; i < zerosRestantes; i++) {
            mensagemFinal.append("0");
        }

        this.mensagemCodificada = mensagemFinal.toString();
    }
}
