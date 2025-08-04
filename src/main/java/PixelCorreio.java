import exceptions.FalhaAoCarregarImagem;
import org.bytedeco.opencv.opencv_core.Mat;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;

public final class PixelCorreio {

    private PixelCorreio() {}

    public static Mat lerImagem(Path caminhoDaImagem) {
        Mat imagem = imread(caminhoDaImagem.toString());
        if (imagem.empty()) {
            throw new FalhaAoCarregarImagem("Falha ao carregar a imagem em: " + caminhoDaImagem);
        }
        return imagem;
    }

    public static Mat lerImagemDeRecurso(String caminhoNoRecurso) {
        try {
            URL url = PixelCorreio.class.getClassLoader().getResource(caminhoNoRecurso);
            if (url == null) {
                throw new FalhaAoCarregarImagem("Recurso não encontrado no classpath: " + caminhoNoRecurso);
            }
            return lerImagem(Paths.get(url.toURI()));
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar o caminho do recurso: " + caminhoNoRecurso, e);
        }
    }

    public static void salvarImagem(Path caminhoFinal, Mat imagemParaSalvar) {
        try {
            Files.createDirectories(caminhoFinal.getParent());
            imwrite(caminhoFinal.toString(), imagemParaSalvar);
            System.out.println("Imagem salva em: " + caminhoFinal.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Falha ao criar diretórios para o caminho de saída.", e);
        }
    }

    public static List<Path> listarImagens(Path pastaDeEntrada) {
        if (!Files.isDirectory(pastaDeEntrada)) {
            System.err.println("AVISO: O caminho de entrada não é um diretório válido: " + pastaDeEntrada);
            return Collections.emptyList();
        }
        try (Stream<Path> stream = Files.list(pastaDeEntrada)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().matches(".*\\.(jpg|jpeg|png|bmp)$"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Falha ao listar os arquivos do diretório: " + pastaDeEntrada, e);
        }
    }
}
