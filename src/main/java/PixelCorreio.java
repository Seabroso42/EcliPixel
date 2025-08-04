import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.FileWriter;
import java.io.IOException;

public class PixelCorreio {

    public static int[] calcularHistograma(BufferedImage imagem) {
        int[] histograma = new int[256];
        for (int y = 0; y < imagem.getHeight(); y++) {
            for (int x = 0; x < imagem.getWidth(); x++) {
                int rgb = imagem.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int gray = (r + g + b) / 3;
                histograma[gray]++;
            }
        }
        return histograma;
    }

    public static void salvarHistograma(int[] histograma, String caminho) {
        try (FileWriter writer = new FileWriter(caminho)) {
            for (int i = 0; i < histograma.length; i++) {
                writer.write(i + "," + histograma[i] + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void salvarImagem(BufferedImage imagem, String caminho) {
    try {
        File outputfile = new File(caminho);
        ImageIO.write(imagem, "png", outputfile);
        System.out.println("Imagem salva em: " + caminho);
    } catch (IOException e) {
        e.printStackTrace();
    }
}

}