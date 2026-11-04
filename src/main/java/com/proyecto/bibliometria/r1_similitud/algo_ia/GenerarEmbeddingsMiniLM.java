package com.proyecto.bibliometria.r1_similitud.algo_ia;

import com.proyecto.bibliometria.corpus.CorpusAbstracts;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

// Se generan y guardan embeddings de los abstracts para reutilizarlos en el análisis.
public final class GenerarEmbeddingsMiniLM {

    private static final Path CARPETA_MODELO = Path.of("modelos", "transformer");
    private static final Path CARPETA_ABSTRACTS = Path.of("abstracts");
    private static final Path ARCHIVO_SALIDA = Path.of("embeddings", "minilm_abstracts.txt");

    private GenerarEmbeddingsMiniLM() {
    }

    public static void main(String[] args) throws Exception {
        CorpusAbstracts corpus = new CorpusAbstracts(CARPETA_ABSTRACTS.toString());
        Path carpetaSalida = ARCHIVO_SALIDA.getParent();
        if (carpetaSalida != null) {
            Files.createDirectories(carpetaSalida);
        }

        try (MiniLMModelo modelo = new MiniLMModelo(CARPETA_MODELO)) {
            List<String> lineas = new ArrayList<>(corpus.tamano() + 1);
            lineas.add("id\ttitulo\tembedding_384");

            for (int i = 0; i < corpus.tamano(); i++) {
                double[] vector = modelo.vectorizar(corpus.tokens.get(i));
                if (vector.length != 384) {
                    throw new IllegalStateException(
                            "Se esperaba un embedding de 384 componentes y se recibieron " + vector.length);
                }

                String componentes = java.util.Arrays.stream(vector)
                        .mapToObj(valor -> String.format(Locale.ROOT, "%.8f", valor))
                        .collect(Collectors.joining(","));
                lineas.add((i + 1) + "\t" + corpus.nombres.get(i) + "\t" + componentes);
            }

            Files.write(ARCHIVO_SALIDA, lineas, StandardCharsets.UTF_8);
        }

        System.out.println("Embeddings generados para " + corpus.tamano() + " abstracts preprocesados en "
                + ARCHIVO_SALIDA.toAbsolutePath());
    }
}
