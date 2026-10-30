package r1_similitud_test.algo_clasicos_test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.proyecto.bibliometria.corpus.Preprocesador;
import com.proyecto.bibliometria.r1_similitud.algo_clasicos.Jaccard;
import static utilidades_test.LectorAbstract.leerAbstract;

public class TestJaccard {
    // Caso pequeno con respuesta conocida: 2 en comun de 5 en total = 0.4
    @Test
    void casoPequenoConocido() {
        List<String> a = List.of("ai", "education", "learning");
        List<String> b = List.of("ai", "education", "assessment", "students");
        // Ese 1e-9 basicamente dice que debe ser tan exacto, que si
        // la diferencia entre el esperado y actual es menor o igual a
        // 0.000000001, es exitoso
        assertEquals(0.4, Jaccard.similitud(a, b), 1e-9);
    }

    // Mismo tema vs distinto tema
    @Test
    void mismoTemaVsDistintoTema() throws Exception {
        Preprocesador prep = new Preprocesador();
        List<String> a = prep.tokenizar(leerAbstract("11. "));
        List<String> b = prep.tokenizar(leerAbstract("16. "));
        List<String> c = prep.tokenizar(leerAbstract("7. "));

        double mismoTema = Jaccard.similitud(a, b);
        double distintoTema = Jaccard.similitud(a, c);

        System.out.println("Jaccard 11 vs 16 (mismo tema): " + mismoTema);
        System.out.println("Jaccard 11 vs 7 (distinto tema): " + distintoTema);

        assertTrue(mismoTema > distintoTema, "Mismo tema deberia parecerse mas");
    }
}
