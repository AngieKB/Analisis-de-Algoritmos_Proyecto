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

import com.proyecto.bibliometria.r1_similitud.algo_ia.MiniLMEmbeddings;

// Estas pruebas usan vectores MiniLM que ya fueron generados y guardados en un archivo.
// La caché carga cada ID, título y vector de 384 componentes en memoria.
// Después calcula el coseno directamente entre los vectores, sin abrir el modelo ONNX
// ni volver a procesar los abstracts. Así se prueba el uso de embeddings precalculados.
class TestMiniLMEmbeddings {

    @TempDir
    Path carpetaTemporal;

    @Test
    void cargaElArchivoGeneradoParaElCorpus() throws Exception {
        Path archivo = Path.of("embeddings", "minilm_abstracts.txt");
        assumeTrue(Files.isRegularFile(archivo), "Primero se deben generar los embeddings MiniLM");

        MiniLMEmbeddings embeddings = new MiniLMEmbeddings(archivo);
        double mismoTema = embeddings.similitud(11, 16);
        double distintoTema = embeddings.similitud(11, 7);

        assertEquals(20, embeddings.ids().size());
        assertTrue(mismoTema > distintoTema);

        System.out.println("MiniLM en caché - artículos cargados: " + embeddings.ids().size());
        System.out.printf(Locale.ROOT,
                "MiniLM en caché - Artículo 11 vs 16 (mismo tema): %.12f%n", mismoTema);
        System.out.printf(Locale.ROOT,
                "MiniLM en caché - Artículo 11 vs 7 (distinto tema): %.12f%n", distintoTema);
    }

    @Test
    void calculaSimilitudDeDosArticulosDesdeArchivo() throws Exception {
        MiniLMEmbeddings embeddings = cargarEmbeddings();
        double similitud = embeddings.similitud(11, 16);

        assertEquals(0.8, similitud, 1e-8);
        assertEquals("Artículo 11", embeddings.titulo(11));

        System.out.println("Prueba con vectores controlados de 384 componentes:");
        System.out.println("Artículo 11: vector unitario en la dimensión 0.");
        System.out.println("Artículo 16: valores 0.8 en la dimensión 0 y 0.6 en la dimensión 1.");
        System.out.println("Producto punto = 0.8; ambas normas = 1; coseno = 0.8.");
        System.out.printf(Locale.ROOT, "Similitud calculada desde la caché: %.8f%n", similitud);
    }

    @Test
    void calculaMatrizSimetricaParaVariosArticulos() throws Exception {
        MiniLMEmbeddings embeddings = cargarEmbeddings();
        double[][] matriz = embeddings.matrizSimilitudes(List.of(11, 16, 7));

        assertEquals(3, matriz.length);
        for (int i = 0; i < matriz.length; i++) {
            assertEquals(1.0, matriz[i][i], 1e-9);
            for (int j = 0; j < matriz.length; j++) {
                assertEquals(matriz[i][j], matriz[j][i], 1e-9);
            }
        }
        assertEquals(0.8, matriz[0][1], 1e-8);
        assertEquals(0.0, matriz[0][2], 1e-8);

        System.out.println("Matriz de similitudes MiniLM en caché, orden de IDs: [11, 16, 7]");
        imprimirMatriz(matriz);
        System.out.println("La diagonal vale 1 y los valores a ambos lados de la diagonal son iguales.");
    }

    @Test
    void rechazaIdSinEmbeddingGuardado() throws Exception {
        MiniLMEmbeddings embeddings = cargarEmbeddings();

        assertThrows(IllegalArgumentException.class, () -> embeddings.similitud(11, 99));
        System.out.println("Validación correcta: la caché rechaza el artículo 99 porque no tiene embedding.");
    }

    @Test
    void rechazaVectorConDimensionIncorrecta() throws Exception {
        Path archivo = carpetaTemporal.resolve("embeddings.txt");
        Files.writeString(archivo,
                "id\ttitulo\tembedding_384\n1\tArtículo 1\t1.0\n",
                StandardCharsets.UTF_8);

        assertThrows(IOException.class, () -> new MiniLMEmbeddings(archivo));
        System.out.println("Validación correcta: se rechaza el archivo porque el vector no tiene 384 componentes.");
    }

    // Esta caché temporal se crea con vectores sencillos para conocer de antemano
    // el coseno esperado y comprobar el cálculo sin depender del modelo ni del corpus real.
    private MiniLMEmbeddings cargarEmbeddings() throws IOException {
        Path archivo = carpetaTemporal.resolve("embeddings.txt");
        Files.writeString(archivo, contenidoValido(), StandardCharsets.UTF_8);
        return new MiniLMEmbeddings(archivo);
    }

    // Los tres vectores tienen 384 posiciones, igual que MiniLM.
    // El artículo 11 apunta en la dimensión 0 y el 7 en la dimensión 1, por eso
    // su coseno es 0. El artículo 16 forma un ángulo con coseno 0.8 respecto al 11.
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

    private static void imprimirMatriz(double[][] matriz) {
        for (double[] fila : matriz) {
            System.out.println(java.util.Arrays.toString(fila));
        }
    }
}
