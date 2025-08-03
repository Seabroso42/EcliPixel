import java.awt.image.BufferedImage;

public abstract class TarefaSimples implements Runnable {
    protected BufferedImage imagem;

    public TarefaSimples(BufferedImage imagem) {
        this.imagem = imagem;
    }

    public abstract BufferedImage processar();

    @Override
    public void run() {
        BufferedImage resultado = processar();
        PixelCorreio.salvarImagem(resultado, "output_" + Thread.currentThread().getId() + ".png");
        System.out.println("Imagem processada na thread " + Thread.currentThread().getName());
    }
}