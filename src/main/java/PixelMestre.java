import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PixelMestre {

    public static BufferedImage binarizarComThreads(BufferedImage imagem, int limiar) {
        int largura = imagem.getWidth();
        int altura = imagem.getHeight();
        BufferedImage resultado = new BufferedImage(largura, altura, BufferedImage.TYPE_BYTE_BINARY);

        int totalThreads = Runtime.getRuntime().availableProcessors();
        Thread[] threads = new Thread[totalThreads];

        for (int i = 0; i < totalThreads; i++) {
            int yInicial = i * altura / totalThreads;
            int yFinal = (i + 1) * altura / totalThreads;
            threads[i] = new Thread(new TarefaSimples(imagem, resultado, yInicial, yFinal, limiar));
            threads[i].start();
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return resultado;
    }
}