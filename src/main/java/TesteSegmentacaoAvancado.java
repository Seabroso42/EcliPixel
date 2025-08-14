import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Classe de teste avançada para validar a funcionalidade EcliPixel.segmentarHSV()
 * com múltiplos limiares, múltiplas imagens e medição de tempo de execução.
 */
public class TesteSegmentacaoAvancado {

    public static void main(String[] args) {
        System.out.println("--- INICIANDO TESTE AVANÇADO DE SEGMENTAÇÃO HSV ---");

        long tempoTotalInicio = System.nanoTime();
        String pastaSaida = "output/teste_segmentacao_avancado";

        List<String> nomesRecursos = List.of(
                "input/tangela_berry.png",
                "input/coolpaint.jpg",
                "input/bluebirds.jpg"
        );

        System.out.println(nomesRecursos.size() + " imagens alvo selecionadas.");

        for (String nomeRecurso : nomesRecursos) {
            Path caminhoDaImagem = null;
            try {
                caminhoDaImagem = Paths.get(TesteSegmentacaoAvancado.class.getClassLoader().getResource(nomeRecurso).toURI());
            } catch (Exception e) {
                System.err.println("Não foi possível encontrar o recurso: " + nomeRecurso);
                continue;
            }

            System.out.println("\n--- Processando: " + caminhoDaImagem.getFileName() + " ---");
            long tempoImagemInicio = System.nanoTime();
            String nomeBaseArquivo = caminhoDaImagem.getFileName().toString().substring(0, caminhoDaImagem.getFileName().toString().lastIndexOf('.'));

            try (Mat imagemOriginal = PixelCorreio.lerImagem(caminhoDaImagem)) {

                // --- TESTES PARA A COR AZUL ---
                Scalar azulEscuroInf = new Scalar(110, 100, 50, 0);
                Scalar azulEscuroSup = new Scalar(130, 255, 255, 0);
                executarTesteDeCor(imagemOriginal, pastaSaida, nomeBaseArquivo, "azul_escuro", azulEscuroInf, azulEscuroSup);

                // --- TESTES PARA A COR VERDE ---
                Scalar verdeBrilhanteInf = new Scalar(40, 80, 80, 0);
                Scalar verdeBrilhanteSup = new Scalar(80, 255, 255, 0);
                executarTesteDeCor(imagemOriginal, pastaSaida, nomeBaseArquivo, "verde_brilhante", verdeBrilhanteInf, verdeBrilhanteSup);

                // --- TESTES PARA A COR VERMELHA (TESTANDO O WRAP-AROUND) ---
                Scalar vermelhoPuroInf = new Scalar(170, 120, 70, 0);
                Scalar vermelhoPuroSup = new Scalar(10, 255, 255, 0);
                executarTesteDeCor(imagemOriginal, pastaSaida, nomeBaseArquivo, "vermelho_puro", vermelhoPuroInf, vermelhoPuroSup);

            } catch (Exception e) {
                System.err.println("Ocorreu um erro CRÍTICO ao processar " + caminhoDaImagem.getFileName() + ": " + e.getMessage());
                e.printStackTrace();
            }

            long duracaoImagemMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - tempoImagemInicio);
            System.out.println("-> Tempo de processamento para '" + caminhoDaImagem.getFileName() + "': " + duracaoImagemMs + " ms");
        }

        long duracaoTotalMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - tempoTotalInicio);
        System.out.println("\n--- TESTE DE SEGMENTAÇÃO AVANÇADO CONCLUÍDO ---");
        System.out.println("Tempo total de execução de todos os testes: " + duracaoTotalMs + " ms (" + duracaoTotalMs / 1000.0 + " segundos)");
        System.out.println("Verifique os resultados na pasta: " + Paths.get(pastaSaida).toAbsolutePath());
    }

    /**
     * Método auxiliar para executar um teste de segmentação e salvar os dois resultados.
     */
    private static void executarTesteDeCor(Mat imgOrig, String pastaSaida, String nomeBase, String nomeTeste, Scalar inf, Scalar sup) {
        System.out.println("-> Testando segmentação para: " + nomeTeste);
        Mat mascara = null, recorte = null;
        try {
            mascara = EcliPixel.segmentarHSV(imgOrig, inf, sup, true);
            recorte = EcliPixel.segmentarHSV(imgOrig, inf, sup, false);

            String nomeMascara = nomeBase + "_" + nomeTeste + "_mascara.png";
            String nomeRecorte = nomeBase + "_" + nomeTeste + "_recorte.png";

            PixelCorreio.salvarImagem(Paths.get(pastaSaida, nomeMascara), mascara);
            PixelCorreio.salvarImagem(Paths.get(pastaSaida, nomeRecorte), recorte);
        } finally {
            if (mascara != null) mascara.close();
            if (recorte != null) recorte.close();
        }
    }
}
