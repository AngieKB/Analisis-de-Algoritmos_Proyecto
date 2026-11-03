package r1_similitud_test.algo_clasicos_test;

import static org.junit.jupiter.api.Assertions.*;
import static utilidades_test.LectorAbstract.leerCorpus;
import static utilidades_test.LectorAbstract.leerTokens;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.proyecto.bibliometria.r1_similitud.algo_ia.Word2Vec;

// Estas pruebas verifican el modelo Word2Vec entrenado con los 20 abstracts.
// El entrenamiento es la operación más costosa, por eso se realiza una sola vez
// antes de las pruebas y se reutiliza el modelo resultante.
public class TestWord2Vec {

    // El modelo se comparte entre las pruebas para evitar repetir el entrenamiento.
    private static Word2Vec modelo;
    private static int dimension;

    // El modelo se entrena antes de ejecutar las pruebas.
    // La dimensión de 100 se usa como configuración experimental para este corpus pequeño.
    @BeforeAll
    static void entrenar() throws Exception {
        List<List<String>> corpus = leerCorpus();
        dimension = 100;
        modelo = new Word2Vec(corpus, dimension);
        System.out.println("Modelo Word2Vec entrenado con " + corpus.size() + " abstracts.");
    }

    // Se verifica que cada abstract se represente con la dimensión configurada
    // y que sus componentes sean valores finitos.
    @Test
    void vectorDelAbstractTieneDimensionYValoresValidos() throws Exception {
        List<String> tokens = leerTokens("1. ");
        double[] vector = modelo.vectorizar(tokens);

        assertEquals(dimension, vector.length, "El vector debe tener la dimensión configurada");
        for (double componente : vector) {
            assertTrue(Double.isFinite(componente), "Cada componente debe ser un valor finito");
        }
    }

    // Matemáticamente, el coseno de un vector conocido consigo mismo es 1:
    // cos(θ) = (v · v) / (|v| × |v|) = |v|² / |v|².
    @Test
    void abstractContraSiMismoTieneSimilitudUno() throws Exception {
        List<String> tokens = leerTokens("1. ");

        assertEquals(1.0, modelo.similitud(tokens, tokens), 1e-9,
                "La similitud de un abstract consigo mismo debe ser 1");
    }

    // Si no se conoce ninguna palabra, el vector queda en cero y no tiene dirección;
    // por eso su similitud coseno con cualquier vector también debe ser cero.
    @Test
    void tokensDesconocidosProducenVectorYCosenoCero() throws Exception {
        List<String> desconocidos = List.of("token_inexistente_word2vec");
        double[] vector = modelo.vectorizar(desconocidos);

        assertArrayEquals(new double[dimension], vector);
        assertEquals(0.0, modelo.similitud(desconocidos, leerTokens("1. ")), 1e-9);
    }

    // Se compara un par de artículos de tema similar con otro de tema diferente,
    // siguiendo el caso de estudio usado en las demás pruebas de similitud.
    // Este resultado es empírico y está limitado al corpus de 20 abstracts.
    @Test
    void mismoTemaTieneMayorSimilitudQueTemaDiferente() throws Exception {
        List<String> articulo11 = leerTokens("11. ");
        List<String> articulo16 = leerTokens("16. ");
        List<String> articulo7 = leerTokens("7. ");

        double mismoTema = modelo.similitud(articulo11, articulo16);
        double distintoTema = modelo.similitud(articulo11, articulo7);

        System.out.println("Word2Vec - Artículo 11 vs 16 (mismo tema): " + mismoTema);
        System.out.println("Word2Vec - Artículo 11 vs 7 (distinto tema): " + distintoTema);

        assertTrue(mismoTema > distintoTema,
                "El par de artículos del mismo tema debe obtener mayor similitud en este caso de estudio");
        assertTrue(mismoTema >= -1.0 && mismoTema <= 1.0,
                "La similitud del mismo tema debe estar en el rango [-1, 1]");
        assertTrue(distintoTema >= -1.0 && distintoTema <= 1.0,
                "La similitud del tema diferente debe estar en el rango [-1, 1]");
    }

    // La similitud coseno es simétrica: intercambiar los abstracts no debe cambiar el resultado.
    @Test
    void similitudEsSimetrica() throws Exception {
        List<String> a = leerTokens("3. ");
        List<String> b = leerTokens("5. ");

        assertEquals(modelo.similitud(a, b), modelo.similitud(b, a), 1e-9);
    }
}
