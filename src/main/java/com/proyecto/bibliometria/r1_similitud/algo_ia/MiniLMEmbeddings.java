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

// Esta clase usa MiniLM para representar cada abstract como un vector numérico.
// El vector resume el texto en un espacio de 384 dimensiones, donde cada posición
// contiene una característica aprendida por el modelo y no una palabra específica.
// Luego se compara la orientación de dos vectores mediante la similitud del coseno.
public final class MiniLMEmbeddings implements AutoCloseable {

    private final ZooModel<String, float[]> modelo;
    private final Predictor<String, float[]> predictor;

    // El modelo y el tokenizador deben estar juntos en la carpeta indicada.
    // MiniLM primero obtiene representaciones para los tokens del texto.
    // Con mean pooling, esas representaciones se promedian para formar un solo vector
    // por abstract. La normalización deja los embeddings con longitud unitaria.
    // Aunque los vectores ya vengan normalizados, el cálculo del coseno vuelve a
    // considerar las normas para que la fórmula quede explícita y sea robusta.
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

    // Los tokens preparados por el proyecto se unen en un texto y el tokenizador
    // del modelo los transforma a su propia representación de entrada.
    // El modelo procesa esa secuencia y devuelve un único embedding del abstract.
    public double[] vectorizar(List<String> tokens) throws TranslateException {
        Objects.requireNonNull(tokens, "La lista de tokens no puede ser nula");
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("El abstract debe contener al menos un token");
        }

        return predecir(String.join(" ", tokens));
    }

    // Se entrega el abstract completo para que el tokenizador de MiniLM genere
    // sus tokens y el modelo aproveche el contexto de la frase.
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

        // El resultado del modelo llega como un arreglo de valores float.
        // Se copia a double para realizar las operaciones de similitud con mayor
        // precisión en los acumuladores del producto punto y las normas.
        double[] vector = new double[embedding.length];
        for (int i = 0; i < embedding.length; i++) {
            if (!Float.isFinite(embedding[i])) {
                throw new IllegalStateException("El embedding contiene un valor no finito en la posición " + i);
            }
            vector[i] = embedding[i];
        }
        return vector;
    }

    // Se vectoriza cada abstract y se calcula el coseno entre sus embeddings.
    // La similitud mide qué tan alineadas están sus direcciones en el espacio
    // vectorial; no representa directamente un porcentaje de contenido compartido.
    public double similitud(List<String> a, List<String> b) throws TranslateException {
        return coseno(vectorizar(a), vectorizar(b));
    }

    // Para n abstracts se construye una matriz cuadrada de n filas por n columnas.
    // La posición matriz[i][j] guarda la similitud entre el abstract i y el j.
    // La diagonal vale 1 porque cada abstract tiene similitud coseno 1 consigo mismo.
    // La matriz es simétrica: comparar i con j da lo mismo que comparar j con i.
    // Cada embedding se calcula una sola vez y se reutiliza para todos los pares,
    // evitando volver a ejecutar MiniLM durante el llenado de la matriz.
    public double[][] matrizSimilitudes(List<List<String>> abstracts) throws TranslateException {
        Objects.requireNonNull(abstracts, "La lista de abstracts no puede ser nula");
        if (abstracts.size() < 2) {
            throw new IllegalArgumentException("Se requieren al menos dos abstracts para compararlos");
        }

        List<double[]> vectores = new ArrayList<>(abstracts.size());
        for (List<String> tokens : abstracts) {
            vectores.add(vectorizar(tokens));
        }

        // La matriz contiene n² posiciones. Solo se calcula una vez cada par
        // distinto de abstracts y se copia el resultado a las dos posiciones
        // correspondientes, por la simetría del coseno.
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

        // Si cada embedding tiene d dimensiones, ambos arreglos se pueden ver
        // como vectores fila de tamaño 1 por d. En una matriz de embeddings,
        // cada fila sería un abstract y cada columna una dimensión del modelo.
        // Se acumulan tres cantidades recorriendo las d dimensiones:
        // producto punto = suma de a[i] * b[i].
        // normaA² = suma de a[i]² y normaB² = suma de b[i]².
        double productoPunto = 0.0;
        double normaA = 0.0;
        double normaB = 0.0;

        for (int i = 0; i < a.length; i++) {
            productoPunto += a[i] * b[i];
            normaA += a[i] * a[i];
            normaB += b[i] * b[i];
        }

        // La fórmula del coseno divide por el producto de las longitudes.
        // Si alguno de los vectores es cero, esa división no está definida;
        // en ese caso se devuelve 0 para indicar que no hay dirección comparable.
        if (normaA == 0.0 || normaB == 0.0) {
            return 0.0;
        }

        // cos(a,b) = (a · b) / (||a|| * ||b||).
        // La raíz convierte las normas al cuadrado acumuladas en longitudes.
        // El resultado matemático está entre -1 y 1: cerca de 1 indica direcciones
        // parecidas, cerca de 0 direcciones poco alineadas y cerca de -1 direcciones
        // opuestas. El límite final evita pequeñas salidas del rango por redondeo.
        double resultado = productoPunto / (Math.sqrt(normaA) * Math.sqrt(normaB));
        return Math.max(-1.0, Math.min(1.0, resultado));
    }

    @Override
    public void close() {
        predictor.close();
        modelo.close();
    }
}
