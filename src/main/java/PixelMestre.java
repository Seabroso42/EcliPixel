import org.bytedeco.opencv.opencv_core.Mat;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;

public class PixelMestre {

    private final ExecutorService executor;

    public PixelMestre() {
        int numThreads = Runtime.getRuntime().availableProcessors();
        this.executor = Executors.newFixedThreadPool(numThreads);
        System.out.println("PixelMestre iniciado com " + numThreads + " threads.");
    }

    /**
     * Executa um pipeline de processamento em um lote de imagens.
     * @param pastaEntrada O caminho para a pasta de imagens de entrada.
     * @param pastaSaida O caminho para a pasta onde os resultados serão salvos.
     * @param pipelineDeProcessamento Uma função que define a sequência de operações a serem aplicadas em cada imagem.
     */
    public void executarEmLote(String pastaEntrada, String pastaSaida, Function<Mat, Mat> pipelineDeProcessamento) {
        long startTime = System.currentTimeMillis();
        List<Path> caminhosDasImagens = PixelCorreio.listarImagens(Paths.get(pastaEntrada));
        System.out.println("\nIniciando lote: " + caminhosDasImagens.size() + " imagens encontradas.");

        List<Future<Void>> futuros = new ArrayList<>();

        for (Path caminhoDaImagem : caminhosDasImagens) {
            Callable<Void> tarefa = () -> {
                String threadName = Thread.currentThread().getName();
                try {
                    System.out.println("[" + threadName + "] Iniciando processamento de: " + caminhoDaImagem.getFileName());

                    Mat imagem = PixelCorreio.lerImagem(caminhoDaImagem);
                    Mat imagemProcessada = pipelineDeProcessamento.apply(imagem);

                    String nomeSaida = "processado_" + caminhoDaImagem.getFileName().toString();
                    Path caminhoFinal = Paths.get(pastaSaida).resolve(nomeSaida);
                    PixelCorreio.salvarImagem(caminhoFinal, imagemProcessada);

                    System.out.println("[" + threadName + "] Finalizou: " + caminhoDaImagem.getFileName());
                } catch (Exception e) {
                    System.err.println("[" + threadName + "] Erro ao processar " + caminhoDaImagem.getFileName() + ": " + e.getMessage());
                }
                return null;
            };
            futuros.add(executor.submit(tarefa));
        }

        for (Future<Void> futuro : futuros) {
            try {
                futuro.get(); // Espera a conclusão de cada tarefa
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Uma tarefa de processamento em lote falhou: " + e.getMessage());
            }
        }

        long endTime = System.currentTimeMillis();
        System.out.println("\nProcessamento em lote concluído em " + (endTime - startTime) / 1000.0 + " segundos.");
        desligar();
    }

    public void desligar() {
        System.out.println("Desligando o PixelMestre...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("PixelMestre desligado.");
    }
}
