import org.bytedeco.opencv.opencv_core.Mat;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Classe de teste focada em validar a funcionalidade EcliPixel.isolarCanal().
 */
public class TesteIsolarCanal {

    public static void main(String[] args) {
        System.out.println("--- INICIANDO TESTE FOCADO: Isolar Canais de Cor ---");

        String pastaSaida = "output/teste_isolar_canal";
        List<Path> caminhosDasImagens = PixelCorreio.listarImagens(Paths.get("src/main/resources/input"));

        if (caminhosDasImagens.isEmpty()) {
            System.err.println("Nenhuma imagem encontrada na pasta de recursos 'input'. Teste abortado.");
            return;
        }

        System.out.println(caminhosDasImagens.size() + " imagens encontradas. Iniciando extração de canais...");

        for (Path caminhoDaImagem : caminhosDasImagens) {
            System.out.println("\n--- Processando: " + caminhoDaImagem.getFileName() + " ---");

            try (Mat imagemOriginal = PixelCorreio.lerImagem(caminhoDaImagem)) {

                // Ignora imagens que já são tons de cinza
                if (imagemOriginal.channels() < 3) {
                    System.out.println("-> Imagem em tons de cinza, pulando teste de isolamento de canal.");
                    continue;
                }

                // Loop para extrair e salvar cada um dos 3 canais de cor
                for (int i = 1; i <= 3; i++) {
                    Mat canalIsolado = null;
                    try {
                        // 1. Isola o canal 'i'
                        canalIsolado = EcliPixel.isolarCanal(imagemOriginal, i);

                        // 2. Monta o nome do arquivo de saída
                        String nomeOriginal = caminhoDaImagem.getFileName().toString();
                        // Remove a extensão do arquivo original para um nome mais limpo
                        String nomeBase = nomeOriginal.substring(0, nomeOriginal.lastIndexOf('.'));
                        String nomeSaida = nomeBase + "_testeBGR_canal" + i + ".png";

                        // 3. Salva o canal isolado
                        PixelCorreio.salvarImagem(Paths.get(pastaSaida, nomeSaida), canalIsolado);

                    } finally {
                        // 4. Garante a liberação da memória do Mat do canal isolado
                        if (canalIsolado != null) {
                            canalIsolado.close();
                        }
                    }
                }
                System.out.println("-> Canais B, G, R extraídos com sucesso.");

            } catch (Exception e) {
                System.err.println("Ocorreu um erro CRÍTICO ao processar " + caminhoDaImagem.getFileName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("\n--- TESTE DE ISOLAMENTO DE CANAIS CONCLUÍDO ---");
        System.out.println("Verifique os resultados na pasta: " + Paths.get(pastaSaida).toAbsolutePath());
    }
}
