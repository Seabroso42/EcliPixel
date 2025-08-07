import enums.CanalCor;
import enums.Thresh;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static enums.Thresh.GLOBAL;
import static enums.Thresh.LOCAL_MEDIA;

// --- SEÇÃO DE IMPORTS CORRIGIDA E OTIMIZADA ---
// Importa todas as constantes e funções principais (split, bitwise_and, CV_8UC1, etc.)
import static org.bytedeco.opencv.global.opencv_core.*;
// Importa todas as funções de processamento (cvtColor, GaussianBlur, threshold, inRange, etc.)
import static org.bytedeco.opencv.global.opencv_imgproc.*;

/**
 * O Motor. Contém a lógica pura e stateless de processamento de imagem.
 */
public final class EcliPixel {

    private EcliPixel() {}

    public static Mat aplicarGaussian(Mat imagemEntrada, int kernelSize) {
        if (imagemEntrada.empty() || kernelSize <= 0 || kernelSize % 2 == 0) {
            throw new IllegalArgumentException("Imagem de entrada não pode ser vazia e o kernel deve ser um inteiro ímpar positivo.");
        }
        Mat imagemDesfocada = new Mat();
        GaussianBlur(imagemEntrada, imagemDesfocada, new Size(kernelSize, kernelSize), 0);
        return imagemDesfocada;
    }

    public static Mat converterCanalCores(Mat imagemEntrada, CanalCor canal) {
        Mat imagemConvertida = new Mat();
        int escolha = switch (canal) {
            case HSV -> COLOR_BGR2HSV;
            case RGB -> COLOR_BGR2RGB;
            case GRAYSCALE -> COLOR_BGR2GRAY;
            case REVERSE -> COLOR_GRAY2BGR;
        };
        cvtColor(imagemEntrada, imagemConvertida, escolha);
        return imagemConvertida;
    }

    public static Mat binarizar(Mat imagemEntrada, Thresh metodo, Object... binparams) {
        Mat imagemBinaria = new Mat();
        Mat imagemCinza = (imagemEntrada.channels() == 3) ? converterCanalCores(imagemEntrada, CanalCor.GRAYSCALE) : imagemEntrada;

        switch (metodo) {
            case OTSU -> threshold(imagemCinza, imagemBinaria, 0, 255, THRESH_BINARY | THRESH_OTSU);
            case GLOBAL, GLOBAL_INV -> {
                if (binparams.length < 1) throw new IllegalArgumentException("Binarização GLOBAL requer 1 parâmetro: limiar (double).");
                double limiar = (double) binparams[0];
                int tipo = (metodo == GLOBAL) ? THRESH_BINARY : THRESH_BINARY_INV;
                threshold(imagemCinza, imagemBinaria, limiar, 255, tipo);
            }
            case LOCAL_MEDIA, LOCAL_GAUSSIANA -> {
                if (binparams.length < 2) throw new IllegalArgumentException("Binarização ADAPTATIVA requer 2 parâmetros: tamanhoBloco (int) e constanteC (double).");
                int tamanhoBloco = (int) binparams[0];
                double constanteC = (double) binparams[1];
                int tipo = (metodo == LOCAL_MEDIA) ? ADAPTIVE_THRESH_MEAN_C : ADAPTIVE_THRESH_GAUSSIAN_C;
                adaptiveThreshold(imagemCinza, imagemBinaria, 255, tipo, THRESH_BINARY, tamanhoBloco, constanteC);
            }
            default -> throw new UnsupportedOperationException("Tipo de binarização não implementado: " + metodo);
        }

        if (imagemCinza != imagemEntrada) {
            imagemCinza.close();
        }
        return imagemBinaria;
    }

    public static Mat binarizarParalelo(Mat imagemEntrada, Thresh metodo, Object... binparams) {
        Mat imagemCinza = (imagemEntrada.channels() == 3) ? converterCanalCores(imagemEntrada, CanalCor.GRAYSCALE) : imagemEntrada;
        Mat imagemBinarizada = new Mat(imagemCinza.size(), imagemCinza.type());

        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        int alturaTotal = imagemCinza.rows();
        int alturaFatia = alturaTotal / numThreads;

        for (int i = 0; i < numThreads; i++) {
            int yInicial = i * alturaFatia;
            int alturaAtual = (i == numThreads - 1) ? (alturaTotal - yInicial) : alturaFatia;

            Rect regiao = new Rect(0, yInicial, imagemCinza.cols(), alturaAtual);
            Mat fatiaEntrada = new Mat(imagemCinza, regiao);
            Mat fatiaSaida = new Mat(imagemBinarizada, regiao);

            Runnable tarefa = () -> {
                Mat resultadoFatia = binarizar(fatiaEntrada, metodo, binparams);
                resultadoFatia.copyTo(fatiaSaida);
                resultadoFatia.close();
            };
            executor.submit(tarefa);
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        if (imagemCinza != imagemEntrada) {
            imagemCinza.close();
        }
        return imagemBinarizada;
    }

    public static Mat isolarCanal(Mat imagemEntrada, int canalEscolhido) {
        if (imagemEntrada.empty() || imagemEntrada.channels() < 3) {
            throw new IllegalArgumentException("A imagem de entrada deve ser colorida (3 canais) para isolar um canal.");
        }
        if (canalEscolhido < 1 || canalEscolhido > 3) {
            throw new IllegalArgumentException("O canal escolhido deve ser 1, 2 ou 3.");
        }
        try (MatVector canais = new MatVector()) {
            split(imagemEntrada, canais);
            return new Mat(canais.get(canalEscolhido - 1));
        }
    }

    public static Mat calcularHistograma(Mat imagemEntrada) {
        if (imagemEntrada.empty()) {
            throw new IllegalArgumentException("A imagem de entrada para o histograma não pode ser vazia.");
        }

        Mat imagemCinza = new Mat();
        imagemEntrada.convertTo(imagemCinza, CV_8UC1);

        int[] histValues = new int[256];

        try (UByteIndexer indexer = imagemCinza.createIndexer()) {
            for (int y = 0; y < indexer.height(); y++) {
                for (int x = 0; x < indexer.width(); x++) {
                    int pixelValue = indexer.get(y, x);
                    histValues[pixelValue]++;
                }
            }
        }

        Mat histograma = new Mat(256, 1, CV_32F);
        try (FloatIndexer histIndexer = histograma.createIndexer()) {
            for (int i = 0; i < 256; i++) {
                histIndexer.put(i, histValues[i]);
            }
        }

        imagemCinza.close();
        return histograma;
    }

    public static Mat segmentarHSV(Mat imagemEntrada, Scalar limiarInferior, Scalar limiarSuperior, boolean binary) {
        if (imagemEntrada.empty() || imagemEntrada.channels() != 3) {
            throw new IllegalArgumentException("A imagem de entrada deve ser colorida (3 canais).");
        }

        try (
                Mat imagemHSV = new Mat();
                Mat mascara = new Mat()
        ) {
            cvtColor(imagemEntrada, imagemHSV, COLOR_BGR2HSV);

            double hInferior = limiarInferior.get(0);
            double hSuperior = limiarSuperior.get(0);

            // Lida com o caso do Matiz (Hue) "circular", como o vermelho
            if (hInferior > hSuperior) {
                // Para o caso circular, precisamos de 2 máscaras e 4 matrizes de limite
                try (
                        Mat mascara1 = new Mat();
                        Mat mascara2 = new Mat();
                        // Limites para a primeira faixa (ex: 160 a 179)
                        Mat limiteInf1 = new Mat(new Scalar(hInferior, limiarInferior.get(1), limiarInferior.get(2), 0));
                        Mat limiteSup1 = new Mat(new Scalar(179.0, limiarSuperior.get(1), limiarSuperior.get(2), 0));
                        // Limites para a segunda faixa (ex: 0 a 10)
                        Mat limiteInf2 = new Mat(new Scalar(0.0, limiarInferior.get(1), limiarInferior.get(2), 0));
                        Mat limiteSup2 = new Mat(new Scalar(hSuperior, limiarSuperior.get(1), limiarSuperior.get(2), 0))
                ) {
                    inRange(imagemHSV, limiteInf1, limiteSup1, mascara1);
                    inRange(imagemHSV, limiteInf2, limiteSup2, mascara2);
                    bitwise_or(mascara1, mascara2, mascara); // Combina as duas máscaras
                }
            } else {
                // Caso normal, com um único intervalo
                try (
                        Mat matInferior = new Mat(limiarInferior);
                        Mat matSuperior = new Mat(limiarSuperior)
                ) {
                    inRange(imagemHSV, matInferior, matSuperior, mascara);
                }
            }

            if (binary) {
                return mascara.clone(); // Retorna uma cópia da máscara P/B
            } else {
                try (Mat resultadoRecortado = new Mat()) {
                    bitwise_and(imagemEntrada, imagemEntrada, resultadoRecortado, mascara);
                    return resultadoRecortado.clone(); // Retorna uma cópia do recorte colorido
                }
            }
        }
    }
}
