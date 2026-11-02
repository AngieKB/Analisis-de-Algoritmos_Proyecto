package com.proyecto.bibliometria.corpus;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CorpusAbstracts {

    // Los abstracts del corpus con su nombre, en un orden fijo (1, 2, 3 ... 20)
    // El indice i de "nombres" corresponde al indice i de "tokens"
    // y tambien a la fila i de la matriz de distancias

    public final List<String> nombres = new ArrayList<>();
    public final List<List<String>> tokens = new ArrayList<>();

    public CorpusAbstracts(String carpetaAbstracts) throws IOException {
        File[] archivos = new File(carpetaAbstracts).listFiles((d, n) -> n.endsWith(".txt"));
        if (archivos == null || archivos.length == 0) {
            throw new IOException("No hay abstracts en: " + carpetaAbstracts);
        }

        // Esto es solo para ordenarlos de 1 a 20 y que el 2 no quede antes del 10
        Arrays.sort(archivos, (x, y) -> Integer.compare(numero(x), numero(y)));

        Preprocesador prep = new Preprocesador();
        for (File f : archivos) {
            nombres.add(f.getName().replaceAll("(?i)\\.txt$", ""));
            tokens.add(prep.tokenizar(Files.readString(f.toPath(), StandardCharsets.UTF_8)));
        }
    }

    // Numero con que empieza el nombre del archivo
    private static int numero(File f) {
        return Integer.parseInt(f.getName().substring(0, f.getName().indexOf('.')));
    }

    public int tamano() {
        return nombres.size();
    }
}