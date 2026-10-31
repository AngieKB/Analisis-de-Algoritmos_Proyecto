package r1_similitud_test.algo_clasicos_test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static utilidades_test.LectorAbstract.leerTokens;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.proyecto.bibliometria.r1_similitud.algo_clasicos.LCS;

public class TestLCS {
    // Caso pequeño con respuesta conocida: LCS = [ai, educacion, evaluacion] = 3
    @Test
    void casoPequenoConocido() {
        List<String> a = List.of("ai", "mejora", "educacion", "evaluacion");
        List<String> b = List.of("ai", "educacion", "nueva", "evaluacion");
        assertEquals(3, LCS.longitud(a, b));
    }

    // Un abstract contra si mismo: LCS = todos sus tokens, similitud 1
    @Test
    void abstractContraSiMismo() throws Exception {
        List<String> tokens = leerTokens("1. ");
        assertEquals(tokens.size(), LCS.longitud(tokens, tokens));
        // Ese 1e-9 basicamente dice que debe ser tan exacto, que si
        // la diferencia entre el esperado y actual es menor o igual a
        // 0.000000001, es exitoso
        assertEquals(1.0, LCS.similitud(tokens, tokens), 1e-9);
    }

    // Mismo tema vs distinto tema
    @Test
    void mismoTemaVsDistintoTema() throws Exception {
        List<String> a = leerTokens("11. ");
        List<String> b = leerTokens("16. ");
        List<String> c = leerTokens("7. ");

        System.out.println("LCS 11 vs 16 (mismo tema): " + LCS.similitud(a, b));
        System.out.println("LCS 11 vs 7 (distinto tema): " + LCS.similitud(a, c));
    }
}
