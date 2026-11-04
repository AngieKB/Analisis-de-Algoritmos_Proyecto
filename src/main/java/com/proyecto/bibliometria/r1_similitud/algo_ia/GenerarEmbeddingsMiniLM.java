package com.proyecto.bibliometria.r1_similitud.algo_ia;

import com.proyecto.bibliometria.corpus.CorpusAbstracts;

import java.io.IOException;
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
        List<String> textos = leerTextos(corpus);
        Path carpetaSalida = ARCHIVO_SALIDA.getParent();
        if (carpetaSalida != null) {
            Files.createDirectories(carpetaSalida);
        }

        try (MiniLMEmbeddings modelo = new MiniLMEmbeddings(CARPETA_MODELO)) {
            List<String> lineas = new ArrayList<>(textos.size() + 1);
            lineas.add("id\ttitulo\tembedding_384");

            for (int i = 0; i < textos.size(); i++) {
                double[] vector = modelo.vectorizarTexto(textos.get(i));
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

        System.out.println("Embeddings generados para " + textos.size() + " abstracts en "
                + ARCHIVO_SALIDA.toAbsolutePath());
    }

    private static List<String> leerTextos(CorpusAbstracts corpus) throws IOException {
        List<String> textos = new ArrayList<>(corpus.tamano());
        for (String nombre : corpus.nombres) {
            Path archivo = CARPETA_ABSTRACTS.resolve(nombre + ".txt");
            if (!Files.isRegularFile(archivo)) {
                throw new IOException("No se encontró el abstract: " + archivo.toAbsolutePath());
            }
            String texto = Files.readString(archivo, StandardCharsets.UTF_8);
            if (texto.isBlank()) {
                throw new IOException("El abstract está vacío: " + archivo.toAbsolutePath());
            }
            textos.add(texto);
        }
        return textos;
    }
}
