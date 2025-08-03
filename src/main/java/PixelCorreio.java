import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class PixelCorreio {
   public static BufferedImage receberImagem(String caminho) {
        try {
            File file = new File(System.getProperty("user.dir") + File.separator + caminho);
            System.out.println("Lendo imagem de: " + file.getAbsolutePath());
            return ImageIO.read(file);
        } catch (IOException e) {
            System.err.println("Erro ao ler imagem: " + caminho);
            e.printStackTrace();
            return null;
        }
    }

    public static void salvarImagem(BufferedImage imagem, String caminho) {
        try {
            ImageIO.write(imagem, "png", new File(caminho));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}