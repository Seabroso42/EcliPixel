import enums.Thresh;
import org.bytedeco.opencv.opencv_core.Mat;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class TesteParalelo {

    public static void main(String[] args) {
        System.out.println("--- INICIANDO TESTES PARALELOS COM PIXELMESTRE ---");

        // Inicia o cronômetro para a execução total
        long tempoTotalInicio = System.nanoTime();

        String pastaDeEntrada;
        try {
            pastaDeEntrada = Paths.get(TesteParalelo.class.getClassLoader().getResource("input").toURI()).toString();
        } catch (Exception e) {
            System.err.println("Não foi possível encontrar a pasta 'input' nos resources. Verifique a estrutura do projeto.");
            e.printStackTrace();
            return;
        }

        // --- TESTE 1: Pipeline de Binarização Otimizada em Lote ---

        System.out.println("\n=======================================================");
        System.out.println("=== INICIANDO LOTE 1: BINARIZAÇÃO OTIMIZADA ===");
        System.out.println("=======================================================");

        long tempoLote1Inicio = System.nanoTime();

        String pastaSaidaBinarizacao = "output/teste_paralelo_binarizacao";
        PixelMestre mestreBinarizacao = new PixelMestre();

        Function<Mat, Mat> pipelineBinarizacao = (imagem) -> {
            if (imagem.cols() > 1500) {
                return EcliPixel.binarizarParalelo(imagem, Thresh.OTSU);
            } else {
                return EcliPixel.binarizar(imagem, Thresh.OTSU);
            }
        };

        mestreBinarizacao.executarEmLote(pastaDeEntrada, pastaSaidaBinarizacao, pipelineBinarizacao);

        long duracaoLote1Ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - tempoLote1Inicio);
        System.out.println("Tempo de execução do Lote 1: " + duracaoLote1Ms + " ms");

        // --- TESTE 2: Pipeline de Efeitos (Gaussian + Canal Vermelho) em Lote ---

        System.out.println("\n=======================================================");
        System.out.println("=== INICIANDO LOTE 2: EFEITOS (GAUSSIAN + CANAL VERMELHO) ===");
        System.out.println("=======================================================");

        long tempoLote2Inicio = System.nanoTime();

        String pastaSaidaEfeitos = "output/teste_paralelo_efeitos";
        PixelMestre mestreEfeitos = new PixelMestre();

        Function<Mat, Mat> pipelineEfeitos = (imagem) -> {
            Mat imagemComBlur = EcliPixel.aplicarGaussian(imagem, 21);
            Mat canalVermelho = EcliPixel.isolarCanal(imagemComBlur, 3);
            imagemComBlur.close();
            return canalVermelho;
        };

        mestreEfeitos.executarEmLote(pastaDeEntrada, pastaSaidaEfeitos, pipelineEfeitos);

        long duracaoLote2Ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - tempoLote2Inicio);
        System.out.println("Tempo de execução do Lote 2: " + duracaoLote2Ms + " ms");

        long duracaoTotalMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - tempoTotalInicio);
        System.out.println("\n=======================================================");
        System.out.println("TODOS OS LOTES PARALELOS FORAM CONCLUÍDOS!");
        System.out.println("Tempo total de execução de todos os testes: " + duracaoTotalMs + " ms (" + duracaoTotalMs / 1000.0 + " segundos)");
    }
}
