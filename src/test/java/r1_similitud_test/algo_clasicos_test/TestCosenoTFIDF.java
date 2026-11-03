package r1_similitud_test.algo_clasicos_test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static utilidades_test.LectorAbstract.leerCorpus;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.proyecto.bibliometria.r1_similitud.algo_clasicos.CosenoTFIDF;
import static utilidades_test.LectorAbstract.leerTokens;

public class TestCosenoTFIDF {
    // Caso pequeño con respuesta conocida: sin comparten palabras da 0, iguales da
    // 1
    @Test
    void casoPequenoConocido() {
        List<List<String>> corpus = List.of(
                List.of("ai", "education"),
                List.of("assessment", "students"),
                List.of("ai", "assessment"));
        CosenoTFIDF modelo = new CosenoTFIDF(corpus);

        assertEquals(0.0, modelo.similitud(corpus.get(0), corpus.get(1)), 1e-9); // nada en comun
        assertEquals(1.0, modelo.similitud(corpus.get(0), corpus.get(0)), 1e-9); // identicos
    }

    // Mismo tema vs distinto tema con los abstracts reales
    @Test
    void mismoTemaVsDistintoTema() throws Exception {
        CosenoTFIDF modelo = new CosenoTFIDF(leerCorpus());

        List<String> a = leerTokens("11. ");
        List<String> b = leerTokens("16. ");
        List<String> c = leerTokens("7. ");

        double mismoTema = modelo.similitud(a, b);
        double distintoTema = modelo.similitud(a, c);

        System.out.println("Coseno TF-IDF 11 vs 16 (mismo tema): " + mismoTema);
        System.out.println("Coseno TF-IDF 11 vs 7 (distinto tema): " + distintoTema);

        assertTrue(mismoTema > distintoTema, "Mismo tema deberia parecerse mas");

    }

    @Test
    void mismoTemaVsDistintoTemaDetallado() throws Exception {
        CosenoTFIDF modelo = new CosenoTFIDF(leerCorpus());

        List<String> a = leerTokens("11. ");
        List<String> b = leerTokens("16. ");
        List<String> c = leerTokens("7. ");

        // Mostrar todos los cálculos de los abstracts 11 y 16
        modelo.imprimirDetalle("Abstract 11", a, "Abstract 16", b);

        // Mostrar todos los cálculos de los abstracts 11 y 7
        modelo.imprimirDetalle("Abstract 11", a, "Abstract 7", c);

        double mismoTema = modelo.similitud(a, b);
        double distintoTema = modelo.similitud(a, c);

        System.out.println("Coseno TF-IDF 11 vs 16: " + mismoTema);
        System.out.println("Coseno TF-IDF 11 vs 7: " + distintoTema);

        assertTrue(mismoTema > distintoTema,
                "Mismo tema deberia parecerse mas");
    }

}
