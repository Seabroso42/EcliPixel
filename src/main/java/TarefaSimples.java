import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.awt.Color;

public class TarefaSimples implements Runnable {
    private final BufferedImage entrada;
    private final BufferedImage saida;
    private final int yInicial, yFinal, limiar;

    public TarefaSimples(BufferedImage entrada, BufferedImage saida, int yInicial, int yFinal, int limiar) {
        this.entrada = entrada;
        this.saida = saida;
        this.yInicial = yInicial;
        this.yFinal = yFinal;
        this.limiar = limiar;
    }

    @Override
    public void run() {
        for (int y = yInicial; y < yFinal; y++) {
            for (int x = 0; x < entrada.getWidth(); x++) {
                int rgb = entrada.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int gray = (r + g + b) / 3;
                int binarizado = (gray < limiar) ? 0x000000 : 0xFFFFFF;
                saida.setRGB(x, y, binarizado);
            }
        }
    }
}
