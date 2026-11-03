package r3_busqueda_test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.proyecto.bibliometria.r3_busqueda.RabinKarp;

// Tests unitarios para el algoritmo Rabin-Karp.
// Cubre: busqueda basica, colisiones de hash (verificacion caracter a caracter),
// casos limite y consistencia con KMP y Boyer-Moore.

public class TestRabinKarp {

    private final RabinKarp rk = new RabinKarp();

    // Busqueda basica

    @Test
    void ocurrenciaUnica() {
        List<Integer> pos = rk.buscar("hello world", "world");
        assertEquals(List.of(6), pos);
    }

    @Test
    void ocurrenciaAlInicio() {
        List<Integer> pos = rk.buscar("abcdef", "abc");
        assertEquals(List.of(0), pos);
    }

    @Test
    void ocurrenciaAlFinal() {
        List<Integer> pos = rk.buscar("xyzabc", "abc");
        assertEquals(List.of(3), pos);
    }

    @Test
    void multipleOcurrencias() {
        List<Integer> pos = rk.buscar("aaaa", "aa");
        assertEquals(List.of(0, 1, 2), pos);
    }

    @Test
    void patronSolapado() {
        List<Integer> pos = rk.buscar("abababa", "aba");
        assertEquals(List.of(0, 2, 4), pos);
    }

    @Test
    void patronAusente() {
        List<Integer> pos = rk.buscar("hello world", "xyz");
        assertTrue(pos.isEmpty());
    }

    @Test
    void patronIgualAlTexto() {
        List<Integer> pos = rk.buscar("exact", "exact");
        assertEquals(List.of(0), pos);
    }

    @Test
    void patronMasLargoQueTexto() {
        List<Integer> pos = rk.buscar("ab", "abc");
        assertTrue(pos.isEmpty());
    }

    @Test
    void textoVacio() {
        List<Integer> pos = rk.buscar("", "ab");
        assertTrue(pos.isEmpty());
    }

    @Test
    void patronDeUnCaracter() {
        List<Integer> pos = rk.buscar("banana", "a");
        assertEquals(List.of(1, 3, 5), pos);
    }

    // Prueba de robustez ante posibles colisiones
    // Texto con muchos caracteres repetidos maximiza el riesgo de falsos positivos

    @Test
    void textosRepetitivosSinColisionFalsa() {
        // "aaab" en "aaaaab" → posicion 2
        List<Integer> pos = rk.buscar("aaaaab", "aaab");
        assertEquals(List.of(2), pos);
    }

    @Test
    void hashRollingCorrectoEnTextoLargo() {
        // Patron al final de un texto largo
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++)
            sb.append('x');
        sb.append("target");
        String texto = sb.toString();

        List<Integer> pos = rk.buscar(texto, "target");
        assertEquals(List.of(1000), pos);
    }

    // Caso de estudio con texto de abstract real

    @Test
    void casoEstudioAbstract() {
        String texto = "This paper proposes a machine learning approach for automated " +
                "assessment of student learning outcomes using deep learning techniques. " +
                "The learning framework integrates natural language processing.";
        String patron = "learning";

        List<Integer> pos = rk.buscar(texto, patron);

        assertEquals(4, pos.size());
        for (int p : pos) {
            assertEquals(patron, texto.substring(p, p + patron.length()));
        }

        System.out.println("[Rabin-Karp] Patron \"" + patron + "\" encontrado en posiciones: " + pos);
    }

    // Consistencia entre los tres algoritmos

    @Test
    void consistenciaConKMPYBoyerMoore() {
        String texto = "the quick brown fox jumps over the lazy dog the";
        String patron = "the";

        List<Integer> pos = rk.buscar(texto, patron);
        assertEquals(3, pos.size());
        assertEquals(0, pos.get(0));
        assertEquals(31, pos.get(1));
        assertEquals(44, pos.get(2));
    }
}
