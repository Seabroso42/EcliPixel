import java.awt.image.BufferedImage;
import java.util.List;

public class ConversaoCinzaComposta implements Runnable {
    private List<BufferedImage> imagens;

    public ConversaoCinzaComposta(List<BufferedImage> imagens) {
        this.imagens = imagens;
    }

    @Override
    public void run() {
        for (BufferedImage imagem : imagens) {
            if (imagem != null) {
                Thread thread = new Thread(new ConversaoCinza(imagem));
                thread.start();
            }
        }
    }
}