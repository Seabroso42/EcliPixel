import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Binarizacao implements Runnable {
    private final BufferedImage imagem;

    public Binarizacao(BufferedImage imagem) {
        this.imagem = imagem;
    }

    @Override
    public void run() {
        System.out.println("Iniciando binarização");
        BufferedImage binarizada = Filtros.binarizar(imagem);
        PixelCorreio.salvarImagem(binarizada, "resources/output/binarizada.png");
    }
}