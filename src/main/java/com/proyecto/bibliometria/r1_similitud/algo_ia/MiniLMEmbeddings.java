package com.proyecto.bibliometria.r1_similitud.algo_ia;

import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Se carga un Transformer de embeddings local y se calcula la similitud coseno
// entre los vectores que representa cada abstract.
public final class MiniLMEmbeddings implements AutoCloseable {

    private final ZooModel<String, float[]> modelo;
    private final Predictor<String, float[]> predictor;

    // El modelo y el tokenizador deben estar juntos en la carpeta indicada.
    // Se configura mean pooling para modelos que entregan un vector por token.
    public MiniLMEmbeddings(Path carpetaModelo) throws IOException, ModelException {
        Objects.requireNonNull(carpetaModelo, "La ruta del modelo no puede ser nula");

        if (!Files.isRegularFile(carpetaModelo.resolve("model.onnx"))) {
            throw new IOException("No se encontró model.onnx en: " + carpetaModelo.toAbsolutePath());
        }
        if (!Files.isRegularFile(carpetaModelo.resolve("tokenizer.json"))) {
            throw new IOException("No se encontró tokenizer.json en: " + carpetaModelo.toAbsolutePath());
        }

        Criteria<String, float[]> criterios = Criteria.builder()
                .setTypes(String.class, float[].class)
                .optModelPath(carpetaModelo)
                .optEngine("OnnxRuntime")
                .optTranslatorFactory(new TextEmbeddingTranslatorFactory())
                .optArgument("pooling", "mean")
                .optArgument("normalize", true)
                .optArgument("includeTokenTypes", true)
                .build();

        this.modelo = criterios.loadModel();
        this.predictor = modelo.newPredictor();
    }

    // El texto se tokeniza y se convierte en un embedding mediante el modelo ONNX.
    public double[] vectorizar(List<String> tokens) throws TranslateException {
        Objects.requireNonNull(tokens, "La lista de tokens no puede ser nula");
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("El abstract debe contener al menos un token");
        }

        return predecir(String.join(" ", tokens));
    }

    // El abstract original se entrega al tokenizador para conservar su contexto completo.
    public double[] vectorizarTexto(String texto) throws TranslateException {
        Objects.requireNonNull(texto, "El texto no puede ser nulo");
        if (texto.isBlank()) {
            throw new IllegalArgumentException("El abstract no puede estar vacío");
        }
        return predecir(texto);
    }

    private double[] predecir(String texto) throws TranslateException {
        float[] embedding = predictor.predict(texto);
        if (embedding.length == 0) {
            throw new IllegalStateException("El modelo ONNX devolvió un embedding vacío");
        }

        double[] vector = new double[embedding.length];
        for (int i = 0; i < embedding.length; i++) {
            if (!Float.isFinite(embedding[i])) {
                throw new IllegalStateException("El embedding contiene un valor no finito en la posición " + i);
            }
            vector[i] = embedding[i];
        }
        return vector;
    }

    // Se calcula el coseno entre los embeddings de los dos abstracts.
    public double similitud(List<String> a, List<String> b) throws TranslateException {
        return coseno(vectorizar(a), vectorizar(b));
    }

    // Se calcula una matriz simétrica para comparar dos o más abstracts seleccionados.
    // Cada embedding se calcula una sola vez y se reutiliza para todos los pares.
    public double[][] matrizSimilitudes(List<List<String>> abstracts) throws TranslateException {
        Objects.requireNonNull(abstracts, "La lista de abstracts no puede ser nula");
        if (abstracts.size() < 2) {
            throw new IllegalArgumentException("Se requieren al menos dos abstracts para compararlos");
        }

        List<double[]> vectores = new ArrayList<>(abstracts.size());
        for (List<String> tokens : abstracts) {
            vectores.add(vectorizar(tokens));
        }

        double[][] matriz = new double[abstracts.size()][abstracts.size()];
        for (int i = 0; i < vectores.size(); i++) {
            matriz[i][i] = 1.0;
            for (int j = i + 1; j < vectores.size(); j++) {
                double similitud = coseno(vectores.get(i), vectores.get(j));
                matriz[i][j] = similitud;
                matriz[j][i] = similitud;
            }
        }
        return matriz;
    }

    private static double coseno(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Los vectores deben tener la misma dimensión");
        }

        double productoPunto = 0.0;
        double normaA = 0.0;
        double normaB = 0.0;

        for (int i = 0; i < a.length; i++) {
            productoPunto += a[i] * b[i];
            normaA += a[i] * a[i];
            normaB += b[i] * b[i];
        }

        if (normaA == 0.0 || normaB == 0.0) {
            return 0.0;
        }

        double resultado = productoPunto / (Math.sqrt(normaA) * Math.sqrt(normaB));
        return Math.max(-1.0, Math.min(1.0, resultado));
    }

    @Override
    public void close() {
        predictor.close();
        modelo.close();
    }
}
