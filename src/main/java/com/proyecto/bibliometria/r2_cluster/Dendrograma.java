package com.proyecto.bibliometria.r2_cluster;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Dendrograma {

    // Dibuja un dendrograma horizontal en un archivo SVG a partir de la lista de
    // fusiones
    // Hojas a la izquierda (altura 0), la raiz a la derecha (distancia maxima)
    // La posicion horizontal de una fusion es proporcional a su distancia
    // Complejidad: O(n), n = cantidad de abstracts (n hojas y n-1 fusiones)

    private static final int ALTO_FILA = 28; // separacion vertical entre hojas
    private static final int MARGEN_IZQ = 430; // espacio para los nombres de los abstracts
    private static final int MARGEN_SUP = 60;
    private static final int ANCHO_ARBOL = 700; // ancho que ocupa el arbol
    private static final int MARGEN_DER = 60;
    private static final int MAX_CARACTERES_NOMBRE = 55;

    public static void guardarSVG(List<Fusion> fusiones, List<String> nombres, String titulo, double escalaMax,
            File salida)
            throws IOException {
        int n = nombres.size();

        // 1) Orden de las hojas: recorrer el arbol desde la raiz (id 2n-2)
        // Asi cada grupo queda contiguo y las lineas no se cruzan
        List<Integer> orden = new ArrayList<>();
        ordenarHojas(2 * n - 2, n, fusiones, orden);

        // 2) Posicion vertical (y) y altura (distancia) de cada nodo
        // Ids 0..n-1 son hojas, ids n..2n-2 son los grupos creados por las fusiones
        double[] y = new double[2 * n - 1];
        double[] altura = new double[2 * n - 1]; // las hojas valen 0
        for (int i = 0; i < n; i++) {
            y[orden.get(i)] = MARGEN_SUP + i * ALTO_FILA;
        }

        // Si escalaMax > 0 se usa esa escala fija (para comparar los 3 dendrogramas)
        // Si vale 0, se usa la distancia maxima de este arbol
        double maxAltura = escalaMax;
        if (maxAltura <= 0) {
            for (Fusion f : fusiones) {
                maxAltura = Math.max(maxAltura, f.distancia);
            }
        }
        if (maxAltura == 0)
            maxAltura = 1; // evita dividir entre 0

        int alto = MARGEN_SUP + n * ALTO_FILA + 50;
        int ancho = MARGEN_IZQ + ANCHO_ARBOL + MARGEN_DER;

        StringBuilder svg = new StringBuilder();
        svg.append(String.format(Locale.US,
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\">%n",
                ancho, alto, ancho, alto));
        svg.append("<rect width=\"100%\" height=\"100%\" fill=\"white\"/>\n"); // fondo blanco
        svg.append(String.format(Locale.US,
                "<text x=\"%d\" y=\"30\" font-size=\"18\" font-weight=\"bold\" font-family=\"Arial\">%s</text>%n",
                MARGEN_IZQ, escapar(titulo)));

        // 3) Nombres de las hojas, a la izquierda
        for (int i = 0; i < n; i++) {
            int id = orden.get(i);
            svg.append(String.format(Locale.US,
                    "<text x=\"%d\" y=\"%.1f\" text-anchor=\"end\" font-size=\"12\" font-family=\"Arial\">%s</text>%n",
                    MARGEN_IZQ - 8, y[id] + 4, escapar(recortar(nombres.get(id)))));
        }

        // 4) Una "U" por cada fusion
        for (int k = 0; k < fusiones.size(); k++) {
            Fusion f = fusiones.get(k);
            int id = n + k; // id del grupo nuevo

            altura[id] = f.distancia;
            y[id] = (y[f.clusterA] + y[f.clusterB]) / 2.0; // en medio de sus dos hijos

            double xA = x(altura[f.clusterA], maxAltura); // donde estaba el hijo A
            double xB = x(altura[f.clusterB], maxAltura);
            double xK = x(altura[id], maxAltura); // donde ocurre la fusion

            svg.append(linea(xA, y[f.clusterA], xK, y[f.clusterA])); // rama horizontal de A
            svg.append(linea(xB, y[f.clusterB], xK, y[f.clusterB])); // rama horizontal de B
            svg.append(linea(xK, y[f.clusterA], xK, y[f.clusterB])); // union vertical
        }

        // 5) Eje de distancias, abajo (5 marcas)
        double yEje = MARGEN_SUP + n * ALTO_FILA;
        svg.append(linea(x(0, maxAltura), yEje, x(maxAltura, maxAltura), yEje));
        for (int t = 0; t <= 4; t++) {
            double valor = maxAltura * t / 4.0;
            double xt = x(valor, maxAltura);
            svg.append(linea(xt, yEje, xt, yEje + 5));
            svg.append(String.format(Locale.US,
                    "<text x=\"%.1f\" y=\"%.1f\" text-anchor=\"middle\" font-size=\"11\" font-family=\"Arial\">%.3f</text>%n",
                    xt, yEje + 20, valor));
        }
        svg.append(String.format(Locale.US,
                "<text x=\"%.1f\" y=\"%.1f\" text-anchor=\"middle\" font-size=\"12\" font-family=\"Arial\">Distancia de fusion</text>%n",
                x(maxAltura / 2, maxAltura), yEje + 40));

        svg.append("</svg>\n");
        Files.writeString(salida.toPath(), svg.toString(), StandardCharsets.UTF_8);
    }

    // Recorre el arbol desde el nodo "id" y agrega las hojas en el orden en que
    // aparecen
    private static void ordenarHojas(int id, int n, List<Fusion> fusiones, List<Integer> orden) {
        if (id < n) { // es una hoja (un abstract original)
            orden.add(id);
            return;
        }
        Fusion f = fusiones.get(id - n); // el grupo n+k nacio en la fusion k
        ordenarHojas(f.clusterA, n, fusiones, orden);
        ordenarHojas(f.clusterB, n, fusiones, orden);
    }

    // Posicion horizontal: distancia 0 = borde izquierdo del arbol, maxAltura =
    // borde derecho
    private static double x(double distancia, double maxAltura) {
        return MARGEN_IZQ + distancia / maxAltura * ANCHO_ARBOL;
    }

    private static String linea(double x1, double y1, double x2, double y2) {
        return String.format(Locale.US,
                "<line x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke=\"#333\" stroke-width=\"1.5\"/>%n",
                x1, y1, x2, y2);
    }

    private static String recortar(String nombre) {
        if (nombre.length() <= MAX_CARACTERES_NOMBRE)
            return nombre;
        return nombre.substring(0, MAX_CARACTERES_NOMBRE) + "...";
    }

    // Los caracteres & < > " rompen un archivo SVG/XML si no se escapan
    private static String escapar(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}