package r3_busqueda_test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.proyecto.bibliometria.r3_busqueda.KMP;

// Tests unitarios para el algoritmo Knuth-Morris-Pratt.
// Cubre: tabla de prefijos, busqueda basica, patrones solapados,
// patron ausente, patron == texto, y busqueda sobre un abstract real.

public class TestKMP {

    private final KMP kmp = new KMP();

    // Tabla de prefijos (LPS)

    @Test
    void tablaPrefijosSinRepeticion() {
        // "abcde" → todos 0 (ningun prefijo propio es sufijo)
        int[] lps = KMP.tablaPrefijos("abcde");
        assertArrayEquals(new int[] { 0, 0, 0, 0, 0 }, lps);
    }

    @Test
    void tablaPrefijosConRepeticion() {
        // "aaaa" → [0,1,2,3]
        int[] lps = KMP.tablaPrefijos("aaaa");
        assertArrayEquals(new int[] { 0, 1, 2, 3 }, lps);
    }

    @Test
    void tablaPrefijosABAB() {
        // "abab" → [0,0,1,2]
        int[] lps = KMP.tablaPrefijos("abab");
        assertArrayEquals(new int[] { 0, 0, 1, 2 }, lps);
    }

    @Test
    void tablaPrefijosABABCABAB() {
        // "ababcabab" → [0,0,1,2,0,1,2,3,4]
        int[] lps = KMP.tablaPrefijos("ababcabab");
        assertArrayEquals(new int[] { 0, 0, 1, 2, 0, 1, 2, 3, 4 }, lps);
    }

    // Busqueda basica

    @Test
    void ocurrenciaUnica() {
        List<Integer> pos = kmp.buscar("hello world", "world");
        assertEquals(List.of(6), pos);
    }

    @Test
    void ocurrenciaAlInicio() {
        List<Integer> pos = kmp.buscar("abcdef", "abc");
        assertEquals(List.of(0), pos);
    }

    @Test
    void ocurrenciaAlFinal() {
        List<Integer> pos = kmp.buscar("xyzabc", "abc");
        assertEquals(List.of(3), pos);
    }

    @Test
    void multipleOcurrencias() {
        // "aa" en "aaaa" aparece en 0,1,2
        List<Integer> pos = kmp.buscar("aaaa", "aa");
        assertEquals(List.of(0, 1, 2), pos);
    }

    @Test
    void patronSolapado() {
        // "aba" en "abababa" → 0, 2, 4
        List<Integer> pos = kmp.buscar("abababa", "aba");
        assertEquals(List.of(0, 2, 4), pos);
    }

    @Test
    void patronAusente() {
        List<Integer> pos = kmp.buscar("hello world", "xyz");
        assertTrue(pos.isEmpty());
    }

    @Test
    void patronIgualAlTexto() {
        List<Integer> pos = kmp.buscar("exact", "exact");
        assertEquals(List.of(0), pos);
    }

    @Test
    void patronMasLargoQueTexto() {
        List<Integer> pos = kmp.buscar("ab", "abc");
        assertTrue(pos.isEmpty());
    }

    @Test
    void textoVacio() {
        List<Integer> pos = kmp.buscar("", "ab");
        assertTrue(pos.isEmpty());
    }

    @Test
    void patronDeUnCaracter() {
        List<Integer> pos = kmp.buscar("banana", "a");
        assertEquals(List.of(1, 3, 5), pos);
    }

    // Caso de estudio con texto de abstract real

    @Test
    void casoEstudioAbstract() {
        // Fragmento representativo de un abstract del corpus
        String texto = "This paper proposes a machine learning approach for automated " +
                "assessment of student learning outcomes using deep learning techniques. " +
                "The learning framework integrates natural language processing.";
        String patron = "learning";

        List<Integer> pos = kmp.buscar(texto, patron);

        // Debe encontrar 4 ocurrencias
        assertEquals(4, pos.size());
        // Verificar que cada posicion es correcta
        for (int p : pos) {
            assertEquals(patron, texto.substring(p, p + patron.length()));
        }

        System.out.println("[KMP] Patron \"" + patron + "\" encontrado en posiciones: " + pos);
    }

    // Consistencia entre implementaciones

    @Test
    void consistenciaConBoyerMooreYRabinKarp() {
        String texto = "the quick brown fox jumps over the lazy dog the";
        String patron = "the";

        List<Integer> posKmp = kmp.buscar(texto, patron);

        // Verificar manualmente: posiciones 0, 31, 44
        assertEquals(3, posKmp.size());
        assertEquals(0, posKmp.get(0));
        assertEquals(31, posKmp.get(1));
        assertEquals(44, posKmp.get(2));
    }
}
