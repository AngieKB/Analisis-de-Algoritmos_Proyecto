package com.proyecto.bibliometria.corpus;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtractorPDF {

    private static final String FALLO = "FALLO EXTRACCION DE ABSTRACT";

    // Desde la palabra "Abstract" hasta "Keywords" / "Introduction".
    // El PDF 9 no tiene Keywords o Introduction como título despues del Abstract.
    private static final Pattern PATRON_ABSTRACT = Pattern.compile(
            "(?is)(?:\\ba\\s?b\\s?s\\s?t\\s?r\\s?a\\s?c\\s?t\\b|\\bresumen\\b)[\\s:.\\-—]*(.*?)(?<=[.\\n])\\s*(?:k\\s?e\\s?y\\s?w\\s?o\\s?r\\s?d\\s?s?|index terms|(?:1\\.?|i\\.?)?\\s*introduction)\\b");

    // Lee la primera página del PDF (Todos los 20 PDFs tienen el abstract en la
    // primera página)
    public static String extraerTexto(File pdf) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(1);
            return stripper.getText(doc);
        }
    }

    private static final Pattern ENCABEZADO = Pattern.compile("(?i)\\ba\\s?b\\s?s\\s?t\\s?r\\s?a\\s?c\\s?t\\b");
    private static final Pattern INTRO = Pattern.compile("(?i)(?:\\d\\.\\s*|^\\s*)introduction\\b");
    private static final int LARGO_LINEA = 80; // menos que esto = línea corta (keywords o fin de párrafo) (Esto sirve
                                               // para el PDF 9 que no tiene el título despues del abstract)

    // Plan B: formato donde las Keywords van antes del texto del abstract (Esto es
    // para el PDF 7 y 9, que no sirve el Plan A)
    private static String extraerKeywordsPrimero(String texto) {
        Matcher m = ENCABEZADO.matcher(texto);
        if (!m.find())
            return null;

        String[] lineas = texto.substring(m.end()).split("\\r?\\n");
        StringBuilder sb = new StringBuilder();
        boolean empezo = false;

        for (String linea : lineas) {
            String t = linea.trim();

            if (!empezo) {
                if (t.length() < LARGO_LINEA)
                    continue; // saltar keywords
                empezo = true;
            }

            boolean fin = false;
            Matcher mi = INTRO.matcher(t);
            if (mi.find()) {
                t = t.substring(0, mi.start()).trim(); // cortar "1. Introduction"
                fin = true;
            } else if (t.length() < LARGO_LINEA && t.endsWith(".")) {
                fin = true; // última línea del párrafo
            }

            if (t.endsWith("-"))
                sb.append(t, 0, t.length() - 1); // une palabra cortada
            else
                sb.append(t).append(' ');

            if (fin)
                break;
        }
        String abs = sb.toString().replaceAll("\\s+", " ").trim();
        return abs.isEmpty() ? null : abs;
    }

    // Devuelve el abstract limpio, o mensaje de fallo si no lo encontró
    public static String extraerAbstract(String texto) {
        Matcher m = PATRON_ABSTRACT.matcher(texto);
        String abs = "";
        if (m.find()) {
            abs = m.group(1).replaceAll("-\\r?\\n", "").replaceAll("\\s+", " ").trim();
        }
        if (abs.isEmpty()) {
            String alt = extraerKeywordsPrimero(texto); // Plan B
            return alt != null ? alt : FALLO;
        }
        return abs;
    }

    // Procesa todos los PDFs de una carpeta y guarda los resultados en la carpeta
    // destino.
    // En nuestro caso, la carpeta de los PDFs se llama "Articulos" y se guardaran
    // los abstracts en la carpeta "abstracts" y los textos crudos en la carpeta
    // "crudos"
    public static void procesarCarpeta(String carpetaPdfs, String carpetaAbstracts, String carpetaCrudos) {
        File[] pdfs = new File(carpetaPdfs).listFiles((d, nombre) -> nombre.toLowerCase().endsWith(".pdf"));
        if (pdfs == null || pdfs.length == 0) {
            System.out.println("No se encontraron PDFs en: " + carpetaPdfs);
            return;
        }
        new File(carpetaAbstracts).mkdirs();
        new File(carpetaCrudos).mkdirs();

        int nuevos = 0, omitidos = 0, fallos = 0;
        for (File pdf : pdfs) {
            String base = pdf.getName().replaceAll("(?i)\\.pdf$", "");
            File destino = new File(carpetaAbstracts, base + ".txt");

            // Si ya existe (extraído antes o escrito a mano), no se recalcula
            if (destino.exists()) {
                System.out.println("SALTA " + pdf.getName() + " (ya existe)");
                omitidos++;
                continue;
            }

            try {
                String texto = extraerTexto(pdf);
                Files.writeString(new File(carpetaCrudos, base + ".txt").toPath(), texto, StandardCharsets.UTF_8);

                String abs = extraerAbstract(texto);
                if (!abs.equals(FALLO)) {
                    Files.writeString(destino.toPath(), abs, StandardCharsets.UTF_8);
                    System.out.println("OK    " + pdf.getName() + " (" + abs.length() + " caracteres)");
                    nuevos++;
                } else {
                    System.out.println("FALLO " + pdf.getName() + "  -> crear a mano: " + destino.getPath());
                    fallos++;
                }
            } catch (IOException e) {
                System.out.println("ERROR " + pdf.getName() + ": " + e.getMessage());
                fallos++;
            }
        }
        System.out.println("\nNuevos: " + nuevos + " | Omitidos: " + omitidos + " | Fallos: " + fallos);
    }
}