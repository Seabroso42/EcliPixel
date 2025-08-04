import enums.CanalCor;
import enums.Thresh;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Scalar;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit; // Import para conversão de tempo
import static org.bytedeco.opencv.global.opencv_core.normalize;
import static org.bytedeco.opencv.global.opencv_imgproc.line;

/**
 * Classe de teste definitiva para validar as funcionalidades da EcliPixel de forma sequencial,
 * com medição de tempo de execução para cada imagem e para o total.
 */
public class TesteSequencialDefinitivo {

    public static void main(String[] args) {
        System.out.println("--- INICIANDO TESTE SEQUENCIAL DEFINITIVO DAS FUNÇÕES EcliPixel ---");

        String pastaSaida = "output/teste_sequencial_definitivo";
        List<Path> caminhosDasImagens = PixelCorreio.listarImagens(Paths.get("src/main/resources/input"));

        if (caminhosDasImagens.isEmpty()) {
            System.err.println("Nenhuma imagem encontrada na pasta de recursos 'input'. Teste abortado.");
            return;
        }

        System.out.println(caminhosDasImagens.size() + " imagens encontradas. Iniciando processamento...");

        // Inicia o cronômetro para o tempo total de execução
        long tempoTotalInicio = System.nanoTime();

        for (Path caminhoDaImagem : caminhosDasImagens) {
            System.out.println("\n--- Processando: " + caminhoDaImagem.getFileName() + " ---");

            // Inicia o cronômetro para esta imagem específica
            long tempoImagemInicio = System.nanoTime();

            try (Mat imagemOriginal = PixelCorreio.lerImagem(caminhoDaImagem)) {
                Mat imagemGaussian = null, imagemBinarizada = null, imagemHSV = null;
                Mat canalH = null, dadosHistograma = null, imagemHistograma = null;

                try {
                    imagemGaussian = EcliPixel.aplicarGaussian(imagemOriginal, 15);
                    PixelCorreio.salvarImagem(Paths.get(pastaSaida, "1_gaussian_" + caminhoDaImagem.getFileName()), imagemGaussian);

                    imagemBinarizada = EcliPixel.binarizar(imagemOriginal, Thresh.OTSU);
                    PixelCorreio.salvarImagem(Paths.get(pastaSaida, "2_binarizada_" + caminhoDaImagem.getFileName()), imagemBinarizada);

                    imagemHSV = EcliPixel.converterCanalCores(imagemOriginal, CanalCor.HSV);
                    PixelCorreio.salvarImagem(Paths.get(pastaSaida, "3_hsv_" + caminhoDaImagem.getFileName()), imagemHSV);

                    canalH = EcliPixel.isolarCanal(imagemHSV, 1);
                    PixelCorreio.salvarImagem(Paths.get(pastaSaida, "4_canal_h_" + caminhoDaImagem.getFileName()), canalH);

                    dadosHistograma = EcliPixel.calcularHistograma(imagemOriginal);
                    imagemHistograma = desenharHistograma(dadosHistograma);
                    PixelCorreio.salvarImagem(Paths.get(pastaSaida, "5_histograma_" + caminhoDaImagem.getFileName()), imagemHistograma);

                } finally {
                    // Libera a memória nativa de todos os objetos Mat intermediários
                    if (imagemGaussian != null) imagemGaussian.close();
                    if (imagemBinarizada != null) imagemBinarizada.close();
                    if (imagemHSV != null) imagemHSV.close();
                    if (canalH != null) canalH.close();
                    if (dadosHistograma != null) dadosHistograma.close();
                    if (imagemHistograma != null) imagemHistograma.close();
                }

            } catch (Exception e) {
                System.err.println("Ocorreu um erro CRÍTICO ao processar " + caminhoDaImagem.getFileName() + ": " + e.getMessage());
                e.printStackTrace();
            }

            // Calcula e imprime o tempo de execução para a imagem atual
            long tempoImagemFim = System.nanoTime();
            long duracaoImagemMs = TimeUnit.NANOSECONDS.toMillis(tempoImagemFim - tempoImagemInicio);
            System.out.println("Tempo de processamento para '" + caminhoDaImagem.getFileName() + "': " + duracaoImagemMs + " ms");
        }

        // Calcula e imprime o tempo total de execução
        long tempoTotalFim = System.nanoTime();
        long duracaoTotalMs = TimeUnit.NANOSECONDS.toMillis(tempoTotalFim - tempoTotalInicio);
        System.out.println("\n--- TESTES CONCLUÍDOS ---");
        System.out.println("Tempo total de execução: " + duracaoTotalMs + " ms (" + duracaoTotalMs / 1000.0 + " segundos)");
        System.out.println("Verifique os resultados na pasta: " + Paths.get(pastaSaida).toAbsolutePath());
    }

    private static Mat desenharHistograma(Mat histograma) {
        if (histograma == null || histograma.empty()) {
            return new Mat(400, 512, org.bytedeco.opencv.global.opencv_core.CV_8UC3, Scalar.WHITE);
        }

        int histSize = 256;
        int histWidth = 512;
        int histHeight = 400;
        int binWidth = histWidth / histSize;

        Mat histImage = new Mat(histHeight, histWidth, org.bytedeco.opencv.global.opencv_core.CV_8UC3, Scalar.WHITE);
        normalize(histograma, histograma, 0, histImage.rows(), org.bytedeco.opencv.global.opencv_core.NORM_MINMAX, -1, new Mat());

        try (FloatIndexer indexer = histograma.createIndexer()) {
            for (int i = 1; i < histSize; i++) {
                Point p1 = new Point(binWidth * (i - 1), histHeight - (int) Math.round(indexer.get(i - 1)));
                Point p2 = new Point(binWidth * i, histHeight - (int) Math.round(indexer.get(i)));
                line(histImage, p1, p2, Scalar.BLACK, 2, 8, 0);
            }
        }
        return histImage;
    }
}
