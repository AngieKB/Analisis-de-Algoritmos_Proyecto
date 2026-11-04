package com.proyecto.bibliometria.r1_similitud.algo_ia;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// Se cargan embeddings guardados para calcular similitudes sin volver a ejecutar MiniLM.
public final class MiniLMEmbeddings {

    private static final String ENCABEZADO = "id\ttitulo\tembedding_384";
    private static final int DIMENSION = 384;

    private final Map<Integer, Documento> documentos;

    public MiniLMEmbeddings(Path archivo) throws IOException {
        Objects.requireNonNull(archivo, "La ruta del archivo no puede ser nula");
        if (!Files.isRegularFile(archivo)) {
            throw new IOException("No se encontró el archivo de embeddings: " + archivo.toAbsolutePath());
        }

        List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
        if (lineas.isEmpty() || !ENCABEZADO.equals(lineas.get(0))) {
            throw new IOException("El archivo de embeddings no tiene el encabezado esperado: " + ENCABEZADO);
        }

        Map<Integer, Documento> cargados = new LinkedHashMap<>();
        for (int i = 1; i < lineas.size(); i++) {
            if (lineas.get(i).isBlank()) {
                continue;
            }
            Documento documento = parsearDocumento(lineas.get(i), i + 1);
            if (cargados.putIfAbsent(documento.id, documento) != null) {
                throw new IOException("ID duplicado en la línea " + (i + 1) + ": " + documento.id);
            }
        }
        if (cargados.isEmpty()) {
            throw new IOException("El archivo no contiene embeddings");
        }
        this.documentos = Collections.unmodifiableMap(cargados);
    }

    // Se conserva el orden de los documentos tal como aparece en el archivo.
    public List<Integer> ids() {
        return List.copyOf(documentos.keySet());
    }

    public String titulo(int id) {
        return buscarDocumento(id).titulo;
    }

    public double similitud(int idA, int idB) {
        return coseno(buscarDocumento(idA).vector, buscarDocumento(idB).vector);
    }

    public DetalleCoseno traza(int idA, int idB) {
        double[] a = buscarDocumento(idA).vector;
        double[] b = buscarDocumento(idB).vector;
        List<ComponenteCoseno> componentes = new ArrayList<>(DIMENSION);
        double productoPunto = 0.0;
        double normaCuadradaA = 0.0;
        double normaCuadradaB = 0.0;
        for (int i = 0; i < DIMENSION; i++) {
            double producto = a[i] * b[i];
            productoPunto += producto;
            normaCuadradaA += a[i] * a[i];
            normaCuadradaB += b[i] * b[i];
            componentes.add(new ComponenteCoseno(i, a[i], b[i], producto));
        }

        double normaA = Math.sqrt(normaCuadradaA);
        double normaB = Math.sqrt(normaCuadradaB);
        double denominador = normaA * normaB;
        double similitud = normaCuadradaA == 0.0 || normaCuadradaB == 0.0
                ? 0.0
                : Math.max(-1.0, Math.min(1.0, productoPunto / denominador));

        return new DetalleCoseno(
                DIMENSION,
                "Los embeddings MiniLM se generaron a partir de las listas de tokens preprocesadas "
                        + "y están almacenados en el archivo de vectores.",
                componentes,
                productoPunto,
                normaCuadradaA,
                normaCuadradaB,
                normaA,
                normaB,
                denominador,
                similitud);
    }

    // Se calculan las similitudes de dos o más documentos sin invocar el modelo.
    public double[][] matrizSimilitudes(List<Integer> ids) {
        Objects.requireNonNull(ids, "La lista de IDs no puede ser nula");
        if (ids.size() < 2) {
            throw new IllegalArgumentException("Se requieren al menos dos IDs para compararlos");
        }

        List<double[]> vectores = new ArrayList<>(ids.size());
        for (Integer id : ids) {
            if (id == null) {
                throw new IllegalArgumentException("La lista de IDs no puede contener valores nulos");
            }
            vectores.add(buscarDocumento(id).vector);
        }

        double[][] matriz = new double[ids.size()][ids.size()];
        for (int i = 0; i < vectores.size(); i++) {
            matriz[i][i] = 1.0;
            for (int j = i + 1; j < vectores.size(); j++) {
                double valor = coseno(vectores.get(i), vectores.get(j));
                matriz[i][j] = valor;
                matriz[j][i] = valor;
            }
        }
        return matriz;
    }

    private Documento buscarDocumento(int id) {
        Documento documento = documentos.get(id);
        if (documento == null) {
            throw new IllegalArgumentException("No existe un embedding guardado para el artículo " + id);
        }
        return documento;
    }

    private static Documento parsearDocumento(String linea, int numeroLinea) throws IOException {
        String[] columnas = linea.split("\\t", 3);
        if (columnas.length != 3) {
            throw new IOException("Formato incorrecto en la línea " + numeroLinea);
        }

        try {
            int id = Integer.parseInt(columnas[0]);
            String titulo = columnas[1];
            String[] componentes = columnas[2].split(",", -1);
            if (componentes.length != DIMENSION) {
                throw new IOException("Se esperaban " + DIMENSION + " componentes en la línea " + numeroLinea
                        + ", pero se encontraron " + componentes.length);
            }

            double[] vector = new double[DIMENSION];
            for (int i = 0; i < componentes.length; i++) {
                vector[i] = Double.parseDouble(componentes[i]);
                if (!Double.isFinite(vector[i])) {
                    throw new IOException("Componente no finito en la línea " + numeroLinea);
                }
            }
            return new Documento(id, titulo, vector);
        } catch (NumberFormatException e) {
            throw new IOException("ID o componente numérico inválido en la línea " + numeroLinea, e);
        }
    }

    private static double coseno(double[] a, double[] b) {
        double productoPunto = 0.0;
        double normaA = 0.0;
        double normaB = 0.0;
        for (int i = 0; i < DIMENSION; i++) {
            productoPunto += a[i] * b[i];
            normaA += a[i] * a[i];
            normaB += b[i] * b[i];
        }
        if (normaA == 0.0 || normaB == 0.0) {
            return 0.0;
        }
        double resultado = productoPunto / (Math.sqrt(normaA) * Math.sqrt(normaB));
        return Math.max(-1.0, Math.min(1.0, resultado));
    }

    public record DetalleCoseno(
            int dimensions,
            String embeddingSource,
            List<ComponenteCoseno> components,
            double dotProduct,
            double normSquaredA,
            double normSquaredB,
            double normA,
            double normB,
            double denominator,
            double similarity) {
    }

    public record ComponenteCoseno(
            int index,
            double valueA,
            double valueB,
            double product) {
    }

    private static final class Documento {
        private final int id;
        private final String titulo;
        private final double[] vector;

        private Documento(int id, String titulo, double[] vector) {
            this.id = id;
            this.titulo = titulo;
            this.vector = vector;
        }
    }
}
