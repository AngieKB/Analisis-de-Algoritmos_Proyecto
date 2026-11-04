package r1_similitud_test.algo_clasicos_test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static utilidades_test.LectorAbstract.leerTokens;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.proyecto.bibliometria.r1_similitud.algo_ia.MiniLMEmbeddings;
import com.proyecto.bibliometria.r1_similitud.algo_ia.MiniLMModelo;

// Se verifica la carga e inferencia del modelo Transformer local con abstracts reales.
class TestMiniLMModelo {

    private static final Path CARPETA_MODELO = Path.of("modelos", "transformer");
    private static MiniLMModelo modelo;

    // Si los archivos grandes no están instalados, las pruebas se omiten.
    @BeforeAll
    static void cargarModelo() throws Exception {
        assumeTrue(
                Files.isRegularFile(CARPETA_MODELO.resolve("model.onnx"))
                        && Files.isRegularFile(CARPETA_MODELO.resolve("tokenizer.json")),
                "Se requieren modelos/transformer/model.onnx y tokenizer.json");
        modelo = new MiniLMModelo(CARPETA_MODELO);
    }

    @AfterAll
    static void cerrarModelo() {
        if (modelo != null) {
            modelo.close();
        }
    }

    // El modelo all-MiniLM-L6-v2 debe producir embeddings de 384 componentes finitos.
    @Test
    void generaEmbeddingDeDimensionEsperada() throws Exception {
        double[] vector = modelo.vectorizar(leerTokens("11. "));

        assertEquals(384, vector.length, "all-MiniLM-L6-v2 debe producir vectores de 384 dimensiones");
        for (double componente : vector) {
            assertTrue(Double.isFinite(componente), "Cada componente debe ser finito");
        }
    }

    // La similitud coseno de un abstract consigo mismo debe ser 1.
    @Test
    void abstractContraSiMismoTieneSimilitudUno() throws Exception {
        var tokens = leerTokens("11. ");

        assertEquals(1.0, modelo.similitud(tokens, tokens), 1e-6);
    }

    // Se compara el mismo caso de estudio usado por los otros algoritmos de similitud.
    @Test
    void comparaAbstractsDeTemaSimilarYDistinto() throws Exception {
        var articulo11 = leerTokens("11. ");
        var articulo16 = leerTokens("16. ");
        var articulo7 = leerTokens("7. ");

        double mismoTema = modelo.similitud(articulo11, articulo16);
        double distintoTema = modelo.similitud(articulo11, articulo7);

        System.out.println("MiniLM - Artículo 11 vs 16 (mismo tema): " + mismoTema);
        System.out.println("MiniLM - Artículo 11 vs 7 (distinto tema): " + distintoTema);

        assertTrue(mismoTema >= -1.0 && mismoTema <= 1.0);
        assertTrue(distintoTema >= -1.0 && distintoTema <= 1.0);
        assertTrue(mismoTema > distintoTema,
                "El par del mismo tema debe obtener mayor similitud en este caso de estudio");
    }

    @Test
    void embeddingsGuardadosCoincidenConLosTokensPreprocesados() throws Exception {
        var articulo11 = leerTokens("11. ");
        var articulo16 = leerTokens("16. ");
        var embeddings = new MiniLMEmbeddings(Path.of("embeddings", "minilm_abstracts.txt"));

        assertEquals(
                modelo.similitud(articulo11, articulo16),
                embeddings.similitud(11, 16),
                1e-6);
    }

    // Se comparan varios abstracts a la vez; la matriz debe ser cuadrada y simétrica.
    @Test
    void generaMatrizSimetricaParaTresAbstracts() throws Exception {
        var abstracts = java.util.List.of(
                leerTokens("11. "),
                leerTokens("16. "),
                leerTokens("7. "));

        double[][] matriz = modelo.matrizSimilitudes(abstracts);

        assertEquals(3, matriz.length);
        for (int i = 0; i < matriz.length; i++) {
            assertEquals(3, matriz[i].length);
            assertEquals(1.0, matriz[i][i], 1e-9);
            for (int j = 0; j < matriz.length; j++) {
                assertEquals(matriz[i][j], matriz[j][i], 1e-9);
                assertTrue(matriz[i][j] >= -1.0 && matriz[i][j] <= 1.0);
            }
        }
        assertTrue(matriz[0][1] > matriz[0][2],
                "El par de artículos del mismo tema debe tener mayor similitud en este caso de estudio");
    }
}
