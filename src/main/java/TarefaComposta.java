import java.awt.image.BufferedImage;
import java.util.List;

public abstract class TarefaComposta implements Runnable {
    protected List<BufferedImage> imagens;

    public TarefaComposta(List<BufferedImage> imagens) {
        this.imagens = imagens;
    }

    public abstract TarefaSimples criarTarefa(BufferedImage imagem);

    @Override
    public void run() {
        for (BufferedImage imagem : imagens) {
            TarefaSimples tarefa = criarTarefa(imagem);
            new Thread(tarefa).start();
        }
    }
}