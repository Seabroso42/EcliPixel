import fi.iki.elonen.NanoHTTPD;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Servidor extends NanoHTTPD {

    public static final int SOCKET_READ_TIMEOUT = 5000;
    public static final String MIME_TEXTO_SIMPLES = "text/plain";

    private final ExecutorService executor = Executors.newCachedThreadPool();

    public Servidor() throws IOException {
        super(8080);
        start(SOCKET_READ_TIMEOUT, false);
        System.out.println(">>> Servidor rodando em http://localhost:8080/");
    }

    @Override
    public Response serve(IHTTPSession sessao) {
        String uri = sessao.getUri();

        try {
            if (sessao.getMethod() == Method.POST && uri.equals("/binarizar")) {
                return processarBinarizacao(sessao);
            } else if (sessao.getMethod() == Method.POST && uri.equals("/histograma")) {
                return processarHistograma(sessao);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_TEXTO_SIMPLES, "Erro interno no servidor: " + e.getMessage());
        }
        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_TEXTO_SIMPLES, "Operação não encontrada.");
    }

    private Response processarHistograma(IHTTPSession sessao) throws IOException, InterruptedException, ResponseException {
        Map<String, String> arquivos = new HashMap<>();
        sessao.parseBody(arquivos);

        String caminhoArquivoTemp = arquivos.get("postData");
        if (caminhoArquivoTemp == null) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_TEXTO_SIMPLES, "Nenhum arquivo enviado no corpo da requisição.");
        }

        InputStream fluxoEntrada = new FileInputStream(caminhoArquivoTemp);
        BufferedImage imagem = ImageIO.read(fluxoEntrada);
        
        Thread threadHistograma = new Thread(new Histograma(imagem));
        threadHistograma.start();
        threadHistograma.join();

        return newFixedLengthResponse(Response.Status.OK, MIME_TEXTO_SIMPLES, "Histograma salvo com sucesso!");
    }

    private Response processarBinarizacao(IHTTPSession sessao) {
        try {
            Map<String, String> arquivos = new HashMap<>();
            sessao.parseBody(arquivos);

            String caminhoArquivoTemp = arquivos.get("postData");
            if (caminhoArquivoTemp == null) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_TEXTO_SIMPLES, "Arquivo não encontrado no corpo da requisição.");
            }

            Mat imagemOriginal = opencv_imgcodecs.imread(caminhoArquivoTemp, opencv_imgcodecs.IMREAD_GRAYSCALE);
            if (imagemOriginal.empty()) {
                 return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_TEXTO_SIMPLES, "Não foi possível ler o arquivo de imagem.");
            }

            Mat imagemBinarizada = new Mat();
            opencv_imgproc.threshold(imagemOriginal, imagemBinarizada, 128, 255, opencv_imgproc.THRESH_BINARY);

            ByteArrayOutputStream fluxoSaida = new ByteArrayOutputStream();
            byte[] buffer = new byte[(int) (imagemBinarizada.total() * imagemBinarizada.channels())];
            opencv_imgcodecs.imencode(".png", imagemBinarizada, buffer);
            fluxoSaida.write(buffer);

            return newFixedLengthResponse(
                    Response.Status.OK,
                    "image/png",
                    new ByteArrayInputStream(fluxoSaida.toByteArray()),
                    fluxoSaida.size()
            );

        } catch (Exception e) {
            e.printStackTrace();
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_TEXTO_SIMPLES, "Erro ao processar a imagem: " + e.getMessage());
        }
    }
}