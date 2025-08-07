import enums.CanalCor;
import enums.Thresh;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * Um teste de benchmark definitivo que executa um pipeline de processamento complexo
 * de forma sequencial e paralela, salvando os resultados apenas na execução final
 * para uma medição de performance mais limpa.
 */
public class TesteSupremo {

    public static void main(String[] args) {
        System.out.println("========= INICIANDO O TESTE SUPREMO DE BENCHMARK =========");

        // --- 1. SETUP ---
        List<String> imagensAlvo = List.of(
                "input/bluebirds.jpg",
                "input/coolpaint.jpg",
                "input/tangela_berry.png"
        );

        Function<Mat, Mat> pipelineSupremo = (imagemOriginal) -> {
            Mat desfocada = null, hsv = null, segmentada = null, cinza = null;
            try {
                desfocada = EcliPixel.aplicarGaussian(imagemOriginal, 5);
                hsv = EcliPixel.converterCanalCores(desfocada, CanalCor.HSV);
                Scalar azulInferior = new Scalar(90, 50, 50, 0);
                Scalar azulSuperior = new Scalar(130, 255, 255, 0);
                segmentada = EcliPixel.segmentarHSV(hsv, azulInferior, azulSuperior, false);
                cinza = EcliPixel.converterCanalCores(segmentada, CanalCor.GRAYSCALE);
                return EcliPixel.binarizar(cinza, Thresh.OTSU);
            } finally {
                if (desfocada != null) desfocada.close();
                if (hsv != null) hsv.close();
                if (segmentada != null) segmentada.close();
                if (cinza != null) cinza.close();
            }
        };

        // --- 2. EXECUÇÃO SEQUENCIAL (BASELINE) ---
        System.out.println("\n--- INICIANDO ETAPA SEQUENCIAL (BASELINE) ---");
        long tempoSequencialInicio = System.nanoTime();
        String pastaSaidaSequencial = "output/teste_supremo_sequencial";

        for (String recurso : imagensAlvo) {
            System.out.println("[Sequencial] Processando: " + Paths.get(recurso).getFileName());
            try (Mat imagemOriginal = PixelCorreio.lerImagemDeRecurso(recurso);
                 Mat imagemProcessada = pipelineSupremo.apply(imagemOriginal)) {
                String nomeSaida = "supremo_" + Paths.get(recurso).getFileName().toString();
                PixelCorreio.salvarImagem(Paths.get(pastaSaidaSequencial, nomeSaida), imagemProcessada);
            }
        }
        long duracaoSequencialMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - tempoSequencialInicio);
        System.out.println("--- ETAPA SEQUENCIAL CONCLUÍDA ---\n");

        // --- 3. LOOP DE EXECUÇÃO PARALELA COM DIFERENTES THREADS ---

        Map<Integer, Long> resultadosParalelos = new LinkedHashMap<>();
        int maxThreads = Runtime.getRuntime().availableProcessors();
        List<Integer> contagemDeThreadsParaTestar = List.of(1, 2, 4, 8, maxThreads);

        for (int numThreads : contagemDeThreadsParaTestar) {
            if (numThreads > maxThreads) continue;

            // Define se os arquivos devem ser salvos nesta iteração
            boolean salvarResultados = (numThreads == maxThreads);
            String pastaSaidaParalelo = "output/teste_supremo_paralelo";

            System.out.println("--- INICIANDO ETAPA PARALELA COM " + numThreads + " THREAD(S) ---");
            if(salvarResultados) {
                System.out.println("(Esta etapa salvará os arquivos de resultado)");
            }

            long tempoParaleloInicio = System.nanoTime();

            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            List<Future<Void>> futuros = new ArrayList<>();

            for (String recurso : imagensAlvo) {
                Callable<Void> tarefa = () -> {
                    try (Mat imagemOriginal = PixelCorreio.lerImagemDeRecurso(recurso);
                         Mat imagemProcessada = pipelineSupremo.apply(imagemOriginal)) {

                        // A LÓGICA DE SALVAR SÓ É EXECUTADA NA ÚLTIMA ITERAÇÃO
                        if (salvarResultados) {
                            String nomeSaida = "supremo_" + Paths.get(recurso).getFileName().toString();
                            PixelCorreio.salvarImagem(Paths.get(pastaSaidaParalelo, nomeSaida), imagemProcessada);
                        }
                    }
                    return null;
                };
                futuros.add(executor.submit(tarefa));
            }

            for (Future<Void> futuro : futuros) {
                try { futuro.get(); } catch (Exception e) { e.printStackTrace(); }
            }
            executor.shutdown();

            long duracaoParaleloMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - tempoParaleloInicio);
            resultadosParalelos.put(numThreads, duracaoParaleloMs);
            System.out.println("--- ETAPA PARALELA COM " + numThreads + " THREAD(S) CONCLUÍDA ---\n");
        }

        // --- 4. RELATÓRIO FINAL DE COMPARAÇÃO ---
        System.out.println("========= RELATÓRIO DE PERFORMANCE - TESTE SUPREMO =========");
        System.out.println("Pipeline executado em " + imagensAlvo.size() + " imagens.");
        System.out.println("\nTempo de Execução Sequencial (Baseline): " + duracaoSequencialMs + " ms");
        System.out.println("----------------------------------------------------------");

        resultadosParalelos.forEach((numThreads, duracaoMs) -> {
            double speedup = (duracaoMs > 0) ? (double) duracaoSequencialMs / duracaoMs : 0;
            System.out.printf("Tempo Paralelo com %d Thread(s): %d ms (%.2f vezes mais rápido)\n", numThreads, duracaoMs, speedup);
        });
        System.out.println("===============================================================");
    }
}
