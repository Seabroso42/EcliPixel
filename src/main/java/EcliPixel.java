import java.awt.image.BufferedImage;
import java.awt.Graphics;

public class EcliPixel {
    public static BufferedImage converterParaCinza(BufferedImage imagemOriginal) {
        BufferedImage cinza = new BufferedImage(imagemOriginal.getWidth(), imagemOriginal.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = cinza.getGraphics();
        g.drawImage(imagemOriginal, 0, 0, null);
        g.dispose();
        return cinza;
    }
}