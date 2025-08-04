import enums.Thresh;
import fi.iki.elonen.NanoHTTPD;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.opencv.opencv_core.Mat;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imencode;

public class Servidor extends NanoHTTPD {

    private final PixelMestre mestre = new PixelMestre();

    public Servidor() throws IOException {
        super(8080);
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        System.out.println("\n>>> Servidor rodando! Acesse http://localhost:8080/\n");
    }

    @Override
    public void stop() {
        super.stop();
        mestre.desligar();
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();
        try {
            if (Method.POST.equals(method) && "/processar-agora".equals(uri)) {
                return processarImagemUnicaSincrono(session);
            }
            if (Method.POST.equals(method) && "/iniciar-lote".equals(uri)) {
                return iniciarProcessamentoEmLoteAssincrono(session);
            }
            if (Method.POST.equals(method) && "/histograma-dados".equals(uri)) {
                return processarHistogramaDados(session);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Erro interno no servidor: " + e.getMessage());
        }
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Endpoint não encontrado.");
    }

    private Response processarImagemUnicaSincrono(IHTTPSession session) throws IOException, ResponseException {
        Map<String, String> files = new HashMap<>();
        session.parseBody(files);
        String tempFilePathStr = files.get("imagem");
        if (tempFilePathStr == null) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Campo 'imagem' não encontrado.");
        }

        Path tempPath = Paths.get(tempFilePathStr);
        try {
            Mat imagemOriginal = PixelCorreio.lerImagem(tempPath);
            Mat imagemProcessada = EcliPixel.binarizar(imagemOriginal, Thresh.OTSU);
            byte[] buffer = new byte[(int) (imagemProcessada.total() * imagemProcessada.channels() * 2)];
            imencode(".png", imagemProcessada, buffer);
            return newChunkedResponse(Response.Status.OK, "image/png", new ByteArrayInputStream(buffer));
        } finally {
            Files.deleteIfExists(tempPath);
        }
    }

    private Response iniciarProcessamentoEmLoteAssincrono(IHTTPSession session) {
        Map<String, String> params = session.getParms();
        String pastaEntrada = params.get("entrada");
        String pastaSaida = params.get("saida");
        if (pastaEntrada == null || pastaSaida == null) {
            // ... (tratamento de erro) ...
        }

        // AÇÃO COMPLETA: Define o que fazer para cada imagem no lote do servidor.
        Consumer<Path> acaoDoServidor = (caminhoDaImagem) -> {
            Mat imagem = PixelCorreio.lerImagem(caminhoDaImagem);

            Mat imagemProcessada;
            if (imagem.cols() > 2000) {
                imagemProcessada = EcliPixel.binarizarParalelo(imagem, Thresh.OTSU);
            } else {
                imagemProcessada = EcliPixel.binarizar(imagem, Thresh.OTSU);
            }

            String nomeSaida = "resultado_lote_" + caminhoDaImagem.getFileName().toString();
            Path caminhoFinal = Paths.get(pastaSaida).resolve(nomeSaida);
            PixelCorreio.salvarImagem(caminhoFinal, imagemProcessada);

            imagem.close();
            imagemProcessada.close();
        };

        // Usa uma nova thread para chamar o mestre, para não bloquear a requisição.
        new Thread(() -> {
            mestre.executarEmLote(pastaEntrada, acaoDoServidor);
        }).start();

        String mensagem = "Processamento em lote iniciado. Entrada: " + pastaEntrada + ", Saída: " + pastaSaida;
        return newFixedLengthResponse(Response.Status.ACCEPTED, "text/plain", mensagem);
    }

    private Response processarHistogramaDados(IHTTPSession session) throws IOException, ResponseException {
        Map<String, String> files = new HashMap<>();
        session.parseBody(files);
        String tempFilePathStr = files.get("imagem");
        Path tempPath = Paths.get(tempFilePathStr);

        try {
            Mat imagemOriginal = PixelCorreio.lerImagem(tempPath);
            Mat histogramaMat = EcliPixel.calcularHistograma(imagemOriginal);

            StringBuilder csv = new StringBuilder("nivel,pixels\n");
            FloatIndexer indexer = histogramaMat.createIndexer();
            for (int i = 0; i < histogramaMat.rows(); i++) {
                csv.append(i).append(",").append((int) indexer.get(i)).append("\n");
            }
            return newFixedLengthResponse(Response.Status.OK, "text/csv", csv.toString());
        } finally {
            Files.deleteIfExists(tempPath);
        }
    }

    public static void main(String[] args) {
        try {
            new Servidor();
        } catch (IOException ioe) {
            System.err.println("Não foi possível iniciar o servidor:\n" + ioe);
        }
    }
}
