package utilidades_test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

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
}