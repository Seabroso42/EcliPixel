import java.awt.image.BufferedImage;

public class ConversaoCinza extends TarefaSimples {

    public ConversaoCinza(BufferedImage imagem) {
        super(imagem);
    }

    @Override
    public BufferedImage processar() {
        return EcliPixel.converterParaCinza(imagem);
    }
}