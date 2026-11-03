package r3_busqueda_test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.proyecto.bibliometria.r3_busqueda.BoyerMoore;
import com.proyecto.bibliometria.r3_busqueda.BusquedaIngenua;
import com.proyecto.bibliometria.r3_busqueda.MetricasBusqueda;

// Tests unitarios para el algoritmo Boyer-Moore.
// Cubre: tabla de caracter malo, tabla de sufijo bueno, busqueda basica,
// casos limite, pruebas de fuzz y consistencia con la busqueda ingenua (oraculo).

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

    // Consistencia: Boyer-Moore debe dar las mismas posiciones que la ingenua

    @Test
    void consistenciaConIngenua() {
        BusquedaIngenua ingenua = new BusquedaIngenua();
        String[] textos = {
                "the quick brown fox jumps over the lazy dog the",
                "ababababab", "aaaaab", "abcdefghij", "mississippi"
        };
        String[] patrones = { "the", "aba", "aab", "xyz", "issi" };

        for (String texto : textos) {
            for (String patron : patrones) {
                assertEquals(
                        ingenua.buscar(texto, patron),
                        bm.buscar(texto, patron),
                        "BM vs Ingenua: texto=\"" + texto + "\" patron=\"" + patron + "\"");
            }
        }
    }

    @Test
    void metricasSeparadas() {
        MetricasBusqueda m = new MetricasBusqueda();
        bm.buscar("the quick brown fox", "fox", m);
        assertTrue(m.preprocesamientoNs >= 0, "preprocesamientoNs debe ser >= 0");
        assertTrue(m.busquedaNs >= 0, "busquedaNs debe ser >= 0");
        assertTrue(m.comparaciones > 0, "debe haber al menos una comparacion");
    }

    // Bug fix: chars >= 256 (tipograficos de PDF) no deben saltar coincidencias

    @Test
    void caracteresFueraDeLatin1() {
        // \u2019 = comilla tipografica derecha ('), frecuente en PDFs.
        // Antes del fix, BM trataba \u2019 como "ausente del patron" y saltaba la
        // coincidencia.
        assertEquals(List.of(1), bm.buscar("\u2019\u2019a", "\u2019a"));
    }

    @Test
    void guionLargoDentDelPatron() {
        // \u2013 = en dash, otro caracter frecuente en abstracts de PDF
        assertEquals(List.of(0), bm.buscar("state\u2013of\u2013the\u2013art", "state\u2013of"));
    }

    // Fuzz contra BusquedaIngenua (oraculo) con alfabeto mixto ASCII + Unicode
    // Es la prueba mas solida de correctitud que existe:
    // 2000 pares (texto, patron) aleatorios, incluyendo chars >= 256.

    @Test
    void fuzzContraIngenua() {
        java.util.Random rnd = new java.util.Random(42);
        char[] alfabeto = { 'a', 'b', '\u2019', '\u00e9' }; // 'a','b',\u2019,e-acento
        BusquedaIngenua ing = new BusquedaIngenua();
        for (int t = 0; t < 2000; t++) {
            String texto = aleatorio(rnd, alfabeto, 1 + rnd.nextInt(40));
            String patron = aleatorio(rnd, alfabeto, 1 + rnd.nextInt(5));
            assertEquals(
                    ing.buscar(texto, patron),
                    bm.buscar(texto, patron),
                    "BM vs Ingenua: texto=\"" + texto + "\" patron=\"" + patron + "\"");
        }
    }

    private static String aleatorio(java.util.Random r, char[] a, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++)
            sb.append(a[r.nextInt(a.length)]);
        return sb.toString();
    }
}
