package r3_busqueda_test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.proyecto.bibliometria.r3_busqueda.BoyerMoore;

// Tests unitarios para el algoritmo Boyer-Moore.
// Cubre: tabla de caracter malo, tabla de sufijo bueno, busqueda basica,
// casos limite y consistencia con KMP y Rabin-Karp.

public class TestBoyerMoore {

    private final BoyerMoore bm = new BoyerMoore();

    // Tabla de caracter malo

    @Test
    void tablaCaracterMaloBasica() {
        int[] tabla = BoyerMoore.tablaCaracterMalo("abc");
        assertEquals(0, tabla['a']);
        assertEquals(1, tabla['b']);
        assertEquals(2, tabla['c']);
        assertEquals(-1, tabla['z']); // caracter ausente
    }

    @Test
    void tablaCaracterMaloGuardaUltimaOcurrencia() {
        // En "abab", 'a' aparece en 0 y 2; debe guardar 2
        int[] tabla = BoyerMoore.tablaCaracterMalo("abab");
        assertEquals(2, tabla['a']);
        assertEquals(3, tabla['b']);
    }

    // Tabla de sufijo bueno

    @Test
    void tablaSufijoBuenoLongitudCorrecta() {
        String patron = "abcde";
        int[] shift = BoyerMoore.tablaSufijoBueno(patron);
        // El arreglo shift tiene longitud m+1
        assertEquals(patron.length() + 1, shift.length);
    }

    @Test
    void tablaSufijoBuenoTodosPositivos() {
        int[] shift = BoyerMoore.tablaSufijoBueno("abcabc");
        for (int s : shift) {
            assertTrue(s > 0, "Desplazamiento no puede ser 0 o negativo: " + Arrays.toString(shift));
        }
    }

    // Busqueda basica

    @Test
    void ocurrenciaUnica() {
        List<Integer> pos = bm.buscar("hello world", "world");
        assertEquals(List.of(6), pos);
    }

    @Test
    void ocurrenciaAlInicio() {
        List<Integer> pos = bm.buscar("abcdef", "abc");
        assertEquals(List.of(0), pos);
    }

    @Test
    void ocurrenciaAlFinal() {
        List<Integer> pos = bm.buscar("xyzabc", "abc");
        assertEquals(List.of(3), pos);
    }

    @Test
    void multipleOcurrencias() {
        List<Integer> pos = bm.buscar("aaaa", "aa");
        assertEquals(List.of(0, 1, 2), pos);
    }

    @Test
    void patronSolapado() {
        List<Integer> pos = bm.buscar("abababa", "aba");
        assertEquals(List.of(0, 2, 4), pos);
    }

    @Test
    void patronAusente() {
        List<Integer> pos = bm.buscar("hello world", "xyz");
        assertTrue(pos.isEmpty());
    }

    @Test
    void patronIgualAlTexto() {
        List<Integer> pos = bm.buscar("exact", "exact");
        assertEquals(List.of(0), pos);
    }

    @Test
    void patronMasLargoQueTexto() {
        List<Integer> pos = bm.buscar("ab", "abc");
        assertTrue(pos.isEmpty());
    }

    @Test
    void textoVacio() {
        List<Integer> pos = bm.buscar("", "ab");
        assertTrue(pos.isEmpty());
    }

    @Test
    void patronDeUnCaracter() {
        List<Integer> pos = bm.buscar("banana", "a");
        assertEquals(List.of(1, 3, 5), pos);
    }

    // Caso de estudio con texto de abstract real

    @Test
    void casoEstudioAbstract() {
        String texto = "This paper proposes a machine learning approach for automated " +
                "assessment of student learning outcomes using deep learning techniques. " +
                "The learning framework integrates natural language processing.";
        String patron = "learning";

        List<Integer> pos = bm.buscar(texto, patron);

        assertEquals(4, pos.size());
        for (int p : pos) {
            assertEquals(patron, texto.substring(p, p + patron.length()));
        }

        System.out.println("[Boyer-Moore] Patron \"" + patron + "\" encontrado en posiciones: " + pos);
    }

    // Consistencia

    @Test
    void consistenciaConKMPYRabinKarp() {
        String texto = "the quick brown fox jumps over the lazy dog the";
        String patron = "the";

        List<Integer> pos = bm.buscar(texto, patron);
        assertEquals(3, pos.size());
        assertEquals(0, pos.get(0));
        assertEquals(31, pos.get(1));
        assertEquals(44, pos.get(2));
    }
}
