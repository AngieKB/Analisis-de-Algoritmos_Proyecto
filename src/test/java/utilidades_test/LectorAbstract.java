package utilidades_test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.proyecto.bibliometria.corpus.Preprocesador;

// Utilidad compartida por los tests: lee los abstracts ya extraidos de los PDFs reales
public class LectorAbstract {

    private static final String CARPETA_ABSTRACTS = "abstracts";

    private LectorAbstract() {
    }

    // Lee el abstract (ya extraido de los PDFs reales) que empieza con el prefijo
    // dado
    // Ej: "1. " encuentra "1. Artificial intelligence literacy....txt"
    // Se usa "1. " con el espacio para no confundirlo con "10. ", "11. " y asi
    public static String leerAbstract(String prefijo) throws Exception {
        File[] archivos = new File(CARPETA_ABSTRACTS)
                .listFiles((d, n) -> n.startsWith(prefijo) && n.endsWith(".txt"));
        assertNotNull(archivos, "No existe la carpeta " + CARPETA_ABSTRACTS);
        assertEquals(1, archivos.length, "Debe haber exactamente 1 abstract con prefijo " + prefijo);
        return Files.readString(archivos[0].toPath(), StandardCharsets.UTF_8);
    }

    // Lee los 20 abstracts y devuelve los tokens de cada uno (sirve para calcular
    // el IDF)
    public static List<List<String>> leerCorpus() throws Exception {
        Preprocesador prep = new Preprocesador();
        File[] archivos = new File(CARPETA_ABSTRACTS).listFiles((d, n) -> n.endsWith(".txt"));
        assertNotNull(archivos, "No existe la carpeta " + CARPETA_ABSTRACTS);

        List<List<String>> corpus = new ArrayList<>();
        for (File f : archivos) {
            corpus.add(prep.tokenizar(Files.readString(f.toPath(), StandardCharsets.UTF_8)));
        }
        return corpus;
    }

    // Lee el abstract que empieza con el prefijo dado y lo devuelve ya tokenizado
    // Ej: leerTokens("11. ") = tokens del abstract numero 11
    public static List<String> leerTokens(String prefijo) throws Exception {
        return new Preprocesador().tokenizar(leerAbstract(prefijo));
    }
}