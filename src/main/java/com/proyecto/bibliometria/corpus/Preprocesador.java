package com.proyecto.bibliometria.corpus;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class Preprocesador {

    // La longitud minima es 2 por si usa palabras como AI, la cual es muy
    // importante y no deberia ser filtrada
    private static final int LONGITUD_MINIMA = 2;
    private final Set<String> stopwords;

    public Preprocesador() throws IOException {
        this.stopwords = cargarStopwords();
    }

    // Lee stopwords.txt desde src/main/resources
    private static Set<String> cargarStopwords() throws IOException {
        try (InputStream in = Preprocesador.class.getResourceAsStream("/stopwords.txt")) {
            if (in == null) {
                throw new IOException("No se encontró stopwords.txt en resources");
            }
            Set<String> set = new HashSet<>();
            String contenido = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            for (String linea : contenido.split("\\r?\\n")) { // Divide el archivo por lineas
                if (linea.trim().startsWith("#"))
                    continue; // Si la linea empieza con "#", se salta (son los comentarios)
                for (String palabra : linea.trim().split("\\s+")) { // Divide la linea por palabras
                    if (!palabra.isEmpty())
                        set.add(palabra.toLowerCase(Locale.ROOT)); // Agrega la palabra al set en minusculas
                }
            }
            return set;
        }
    }

    // Texto a lista de tokens limpios para poder contarlos luego
    public List<String> tokenizar(String texto) {
        // Normaliza ligaduras y guiones suaves que vienen de los PDFs
        texto = texto.replace("\u00AD", "").replace("ﬁ", "fi").replace("ﬂ", "fl").replace("ﬀ", "ff")
                .replace("ﬃ", "ffi").replace("ﬄ", "ffl");

        // Convierte el texto a minusculas y reemplaza cualquier caracter que no sea
        // una letra o espacio por un espacio
        String limpio = texto.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\s]", " ");

        List<String> tokens = new ArrayList<>();

        // Divide la linea por palabras
        for (String t : limpio.split("\\s+")) {
            // Si la palabra tiene longitud minima y no es una stopword, se agrega al token
            if (t.length() >= LONGITUD_MINIMA && !stopwords.contains(t)) {
                tokens.add(t);
            }
        }
        return tokens;
    }

    // Lee todos los .txt de la carpeta y junta los tokens de todo el corpus
    public List<String> procesarCorpus(String carpetaAbstracts) throws IOException {
        // n es el nombre del archivo. Lo pasa a minusculas y verifica si termina en
        // .txt
        File[] archivos = new File(carpetaAbstracts).listFiles((d, n) -> n.toLowerCase().endsWith(".txt"));
        if (archivos == null || archivos.length == 0) {
            throw new IOException("No hay abstracts en: " + carpetaAbstracts); // Por si no encuentra los abstracts
        }

        List<String> todos = new ArrayList<>();
        for (File f : archivos) {
            String texto = Files.readString(f.toPath(), StandardCharsets.UTF_8); // Lee los abstracts
            List<String> tokens = tokenizar(texto); // Tokeniza los abstracts
            todos.addAll(tokens); // Agrega los tokens
        }
        return todos;
    }
}