import enums.Thresh;
import org.bytedeco.opencv.opencv_core.Mat;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class TesteParalelo {

    public static void main(String[] args) {
        System.out.println("--- INICIANDO TESTES PARALELOS COM PIXELMESTRE ---");

        long tempoTotalInicio = System.nanoTime();

        String pastaDeEntrada;
        try {
            pastaDeEntrada = Paths.get(TesteParalelo.class.getClassLoader().getResource("input").toURI()).toString();
        } catch (Exception e) {
            System.err.println("Não foi possível encontrar a pasta 'input' nos resources.");
            return;
        }

        for (Thresh metodoDeTeste : Thresh.values()) {

            System.out.println("\n=======================================================");
            System.out.println("=== INICIANDO LOTE PARA: " + metodoDeTeste.name() + " ===");

            long tempoLoteInicio = System.nanoTime();
            String pastaSaida = "output/teste_paralelo_binarizacao";
            PixelMestre mestre = new PixelMestre();

            // AÇÃO COMPLETA: O teste define exatamente o que fazer, incluindo como nomear o arquivo.
            Consumer<Path> acaoDeTeste = (caminhoDaImagem) -> {
                Mat imagem = PixelCorreio.lerImagem(caminhoDaImagem);

                Mat imagemProcessada = switch (metodoDeTeste) {
                    case OTSU -> EcliPixel.binarizar(imagem, Thresh.OTSU);
                    case GLOBAL -> EcliPixel.binarizar(imagem, Thresh.GLOBAL, 127.0);
                    case GLOBAL_INV -> EcliPixel.binarizar(imagem, Thresh.GLOBAL_INV, 127.0);
                    case LOCAL_MEDIA -> EcliPixel.binarizar(imagem, Thresh.LOCAL_MEDIA, 11, 2.0);
                    case LOCAL_GAUSSIANA -> EcliPixel.binarizar(imagem, Thresh.LOCAL_GAUSSIANA, 11, 2.0);
                };

                String nomeSaida = metodoDeTeste.name() + "_" + caminhoDaImagem.getFileName().toString();
                Path caminhoFinal = Paths.get(pastaSaida).resolve(nomeSaida);
                PixelCorreio.salvarImagem(caminhoFinal, imagemProcessada);

                imagem.close();
                imagemProcessada.close();
            };

            mestre.executarEmLote(pastaDeEntrada, acaoDeTeste);

            long duracaoLoteMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - tempoLoteInicio);
            System.out.println("Tempo de execução do Lote [" + metodoDeTeste.name() + "]: " + duracaoLoteMs + " ms");
        }

        long duracaoTotalMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - tempoTotalInicio);
        System.out.println("\n=======================================================");
        System.out.println("TODOS OS LOTES DE BINARIZAÇÃO FORAM CONCLUÍDOS!");
        System.out.println("Tempo total de execução de todos os testes: " + duracaoTotalMs + " ms (" + duracaoTotalMs / 1000.0 + " segundos)");
    }
}
