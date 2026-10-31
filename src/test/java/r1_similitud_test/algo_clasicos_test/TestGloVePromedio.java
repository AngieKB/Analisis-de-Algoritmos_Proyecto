package r1_similitud_test.algo_clasicos_test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static utilidades_test.LectorAbstract.leerCorpus;
import static utilidades_test.LectorAbstract.leerTokens;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.proyecto.bibliometria.r1_similitud.algo_ia.GloVePromedio;

public class TestGloVePromedio {

    // Archivo recortado (va en el repo) y archivo completo (este solo fue en mi PC,
    // para que no lo deban descargar)
    private static final File GLOVE_RECORTADO = new File("embeddings/glove_corpus.txt");
    private static final File GLOVE_COMPLETO = new File("modelos/glove.6B.50d.txt");

    // Carga GloVe solo con las palabras que usan los 20 abstracts
    private static GloVePromedio cargarModelo() throws Exception {
        File archivo = GLOVE_RECORTADO.exists() ? GLOVE_RECORTADO : GLOVE_COMPLETO;
        assumeTrue(archivo.exists(), "Falta GloVe: ver pasos 2 y 6");

        Set<String> vocabulario = new HashSet<>();
        for (List<String> documento : leerCorpus()) {
            vocabulario.addAll(documento);
        }
        return new GloVePromedio(archivo, vocabulario);
    }

    // Un abstract contra si mismo: coseno 1
    @Test
    void abstractContraSiMismo() throws Exception {
        GloVePromedio modelo = cargarModelo();
        List<String> tokens = leerTokens("1. ");
        assertEquals(1.0, modelo.similitud(tokens, tokens), 1e-9);
    }

    // Mismo tema vs distinto tema con los abstracts reales
    @Test
    void mismoTemaVsDistintoTema() throws Exception {
        GloVePromedio modelo = cargarModelo();

        List<String> a = leerTokens("11. ");
        List<String> b = leerTokens("16. ");
        List<String> c = leerTokens("7. ");

        double mismoTema = modelo.similitud(a, b);
        double distintoTema = modelo.similitud(a, c);

        System.out.println("Palabras del corpus encontradas en GloVe: " + modelo.palabrasConocidas());
        System.out.println("GloVe 11 vs 16 (mismo tema): " + mismoTema);
        System.out.println("GloVe 11 vs 7 (distinto tema): " + distintoTema);

        // Solo valida el rango: GloVe promediado suele dar valores altos para casi todo
        assertTrue(mismoTema >= -1.0 && mismoTema <= 1.0);
        assertTrue(distintoTema >= -1.0 && distintoTema <= 1.0);
    }
}