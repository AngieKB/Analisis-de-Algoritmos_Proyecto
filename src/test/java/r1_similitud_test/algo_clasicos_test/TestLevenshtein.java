package r1_similitud_test.algo_clasicos_test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.proyecto.bibliometria.corpus.Preprocesador;
import com.proyecto.bibliometria.r1_similitud.algo_clasicos.Levenshtein;
import static utilidades_test.LectorAbstract.leerAbstract;

public class TestLevenshtein {

    // Caso pequeno con respuesta conocida
    @Test
    void casoPequenoConocido() {
        List<String> a = List.of("ai", "educacion");
        List<String> b = List.of("educacion");
        assertEquals(1, Levenshtein.distancia(a, b)); // borrar "ai"
    }

    // Un abstract contra si mismo: distancia 0 y similitud 1
    @Test
    void abstractContraSiMismo() throws Exception {
        Preprocesador prep = new Preprocesador();
        List<String> tokens = prep.tokenizar(leerAbstract("1. "));
        assertEquals(0, Levenshtein.distancia(tokens, tokens));
        assertEquals(1.0, Levenshtein.similitud(tokens, tokens), 1e-9);
    }

    // Dos abstracts reales distintos: similitud entre 0 y 1, y simetrica
    @Test
    void dosAbstractsReales() throws Exception {
        Preprocesador prep = new Preprocesador();
        List<String> a = prep.tokenizar(leerAbstract("11. "));
        List<String> b = prep.tokenizar(leerAbstract("16. "));

        int dAB = Levenshtein.distancia(a, b);
        int dBA = Levenshtein.distancia(b, a);
        double sim = Levenshtein.similitud(a, b);

        System.out.println("Tokens abstract 11: " + a.size());
        System.out.println("Tokens abstract 16: " + b.size());
        System.out.println("Distancia: " + dAB);
        System.out.println("Similitud: " + sim);

        assertEquals(dAB, dBA, "La distancia debe ser simetrica");
        assertTrue(sim >= 0.0 && sim <= 1.0);
        assertTrue(dAB > 0, "Dos abstracts distintos no deberian dar distancia 0");
    }
}