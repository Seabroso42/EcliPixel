import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PixelMestre {
    public static void main(String[] args) {
        List<BufferedImage> imagens = IntStream.range(1, 2)
        .mapToObj(i -> PixelCorreio.receberImagem("resources/img/img" + i + ".png"))
        .collect(Collectors.toList());


        ConversaoCinzaComposta tarefa = new ConversaoCinzaComposta(imagens);
        new Thread(tarefa).start();
    }
}
