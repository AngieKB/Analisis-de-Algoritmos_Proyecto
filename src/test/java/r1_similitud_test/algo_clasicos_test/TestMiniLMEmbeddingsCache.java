package r1_similitud_test.algo_clasicos_test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.proyecto.bibliometria.r1_similitud.algo_ia.MiniLMEmbeddingsCache;

// Se comprueba el cálculo de similitudes usando embeddings guardados, sin cargar MiniLM.
class TestMiniLMEmbeddingsCache {

    @TempDir
    Path carpetaTemporal;

    @Test
    void cargaElArchivoGeneradoParaElCorpus() throws Exception {
        Path archivo = Path.of("embeddings", "minilm_abstracts.txt");
        assumeTrue(Files.isRegularFile(archivo), "Primero se deben generar los embeddings MiniLM");

        MiniLMEmbeddingsCache cache = new MiniLMEmbeddingsCache(archivo);

        assertEquals(20, cache.ids().size());
        assertTrue(cache.similitud(11, 16) > cache.similitud(11, 7));
    }

    @Test
    void calculaSimilitudDeDosArticulosDesdeArchivo() throws Exception {
        MiniLMEmbeddingsCache cache = cargarCache();

        assertEquals(0.8, cache.similitud(11, 16), 1e-8);
        assertEquals("Artículo 11", cache.titulo(11));
    }

    @Test
    void calculaMatrizSimetricaParaVariosArticulos() throws Exception {
        MiniLMEmbeddingsCache cache = cargarCache();
        double[][] matriz = cache.matrizSimilitudes(List.of(11, 16, 7));

        assertEquals(3, matriz.length);
        for (int i = 0; i < matriz.length; i++) {
            assertEquals(1.0, matriz[i][i], 1e-9);
            for (int j = 0; j < matriz.length; j++) {
                assertEquals(matriz[i][j], matriz[j][i], 1e-9);
            }
        }
        assertEquals(0.8, matriz[0][1], 1e-8);
        assertEquals(0.0, matriz[0][2], 1e-8);
    }

    @Test
    void rechazaIdSinEmbeddingGuardado() throws Exception {
        MiniLMEmbeddingsCache cache = cargarCache();

        assertThrows(IllegalArgumentException.class, () -> cache.similitud(11, 99));
    }

    @Test
    void rechazaVectorConDimensionIncorrecta() throws Exception {
        Path archivo = carpetaTemporal.resolve("embeddings.txt");
        Files.writeString(archivo,
                "id\ttitulo\tembedding_384\n1\tArtículo 1\t1.0\n",
                StandardCharsets.UTF_8);

        assertThrows(IOException.class, () -> new MiniLMEmbeddingsCache(archivo));
    }

    private MiniLMEmbeddingsCache cargarCache() throws IOException {
        Path archivo = carpetaTemporal.resolve("embeddings.txt");
        Files.writeString(archivo, contenidoValido(), StandardCharsets.UTF_8);
        return new MiniLMEmbeddingsCache(archivo);
    }

    private static String contenidoValido() {
        double[] a = new double[384];
        a[0] = 1.0;

        double[] b = new double[384];
        b[0] = 0.8;
        b[1] = 0.6;

        double[] c = new double[384];
        c[1] = 1.0;

        return "id\ttitulo\tembedding_384\n"
                + fila(11, "Artículo 11", a) + "\n"
                + fila(16, "Artículo 16", b) + "\n"
                + fila(7, "Artículo 7", c) + "\n";
    }

    private static String fila(int id, String titulo, double[] vector) {
        String componentes = IntStream.range(0, vector.length)
                .mapToObj(i -> String.format(Locale.ROOT, "%.8f", vector[i]))
                .collect(Collectors.joining(","));
        return id + "\t" + titulo + "\t" + componentes;
    }
}
