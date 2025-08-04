import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Orquestra a execução concorrente de uma AÇÃO definida pelo chamador
 * em um lote de imagens. É um serviço genérico e reutilizável.
 */
public class PixelMestre {

    private final ExecutorService executor;

    public PixelMestre() {
        int numThreads = Runtime.getRuntime().availableProcessors();
        this.executor = Executors.newFixedThreadPool(numThreads);
        System.out.println("PixelMestre iniciado com " + numThreads + " threads.");
    }

    /**
     * Executa uma ação de processamento em um lote de imagens.
     * @param pastaEntrada O caminho para a pasta de imagens de entrada.
     * @param acaoDeProcessamento A ação completa a ser executada para cada imagem.
     */
    public void executarEmLote(String pastaEntrada, Consumer<Path> acaoDeProcessamento) {
        long startTime = System.currentTimeMillis();
        List<Path> caminhosDasImagens = PixelCorreio.listarImagens(Paths.get(pastaEntrada));
        System.out.println("\nIniciando lote: " + caminhosDasImagens.size() + " imagens encontradas.");

        List<Future<Void>> futuros = new ArrayList<>();

        for (Path caminhoDaImagem : caminhosDasImagens) {
            Callable<Void> tarefa = () -> {
                String threadName = Thread.currentThread().getName();
                try {
                    System.out.println("[" + threadName + "] Processando: " + caminhoDaImagem.getFileName());
                    // Executa a ação completa definida pelo chamador
                    acaoDeProcessamento.accept(caminhoDaImagem);
                } catch (Exception e) {
                    System.err.println("[" + threadName + "] Erro ao processar " + caminhoDaImagem.getFileName() + ": " + e.getMessage());
                }
                return null;
            };
            futuros.add(executor.submit(tarefa));
        }

        for (Future<Void> futuro : futuros) {
            try {
                futuro.get();
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
        // ... (resto do método desligar permanece o mesmo) ...
    }
}
