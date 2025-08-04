import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class Histograma implements Runnable {
    private final BufferedImage imagem;

    public Histograma(BufferedImage imagem) {
        this.imagem = imagem;
    }

    @Override
    public void run() {
        System.out.println("Calculando histograma");
        int[] histograma = PixelCorreio.calcularHistograma(imagem);
        PixelCorreio.salvarHistograma(histograma, "resources/output/histograma.csv");
    }
}
