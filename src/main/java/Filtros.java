import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class Filtros {

    public static BufferedImage binarizar(BufferedImage imagemOriginal, int limiar) {
        int altura = imagemOriginal.getHeight();
        int largura = imagemOriginal.getWidth();

        BufferedImage imagemBinarizada = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);

        int numThreads = Runtime.getRuntime().availableProcessors();
        int intervalo = altura / numThreads;

        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            int yInicial = i * intervalo;
            int yFinal = (i == numThreads - 1) ? altura : yInicial + intervalo;

            Thread thread = new Thread(new TarefaSimples(imagemOriginal, imagemBinarizada, yInicial, yFinal, limiar));
            threads.add(thread);
            thread.start();
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return imagemBinarizada;
    }

    public static BufferedImage binarizar(BufferedImage imagemOriginal) {
        return binarizar(imagemOriginal, 128);
    }
}
