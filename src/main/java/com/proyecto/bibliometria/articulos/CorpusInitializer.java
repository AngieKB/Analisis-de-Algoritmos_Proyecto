package com.proyecto.bibliometria.articulos;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CorpusInitializer implements ApplicationRunner {

    private static final int CANTIDAD_ARTICULOS_ESPERADA = 20;

    private final ArticuloRepository repository;
    private final JdbcTemplate jdbcTemplate;
    private final File carpetaAbstracts;

    public CorpusInitializer(
            ArticuloRepository repository,
            JdbcTemplate jdbcTemplate,
            @Value("${app.corpus.directory:abstracts}") String carpetaAbstracts) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
        this.carpetaAbstracts = new File(carpetaAbstracts);
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        jdbcTemplate.queryForObject(
                "SELECT id FROM corpus_import_lock WHERE id = 1 FOR UPDATE",
                Integer.class);

        File[] archivos = carpetaAbstracts.listFiles(
                (directorio, nombre) -> nombre.toLowerCase(java.util.Locale.ROOT).endsWith(".txt"));
        if (archivos == null || archivos.length != CANTIDAD_ARTICULOS_ESPERADA) {
            throw new IOException("Se esperaban exactamente " + CANTIDAD_ARTICULOS_ESPERADA
                    + " abstracts .txt en " + carpetaAbstracts.getAbsolutePath());
        }
        Arrays.sort(archivos, (a, b) -> Integer.compare(idArchivo(a), idArchivo(b)));

        List<Articulo> corpus = Arrays.stream(archivos).map(this::leerArticulo).toList();
        Set<Integer> ids = new HashSet<>();
        corpus.forEach(articulo -> ids.add(articulo.getId()));
        if (ids.size() != CANTIDAD_ARTICULOS_ESPERADA) {
            throw new IOException("Los archivos de abstracts deben tener IDs numéricos únicos del 1 al 20");
        }
        for (int id = 1; id <= CANTIDAD_ARTICULOS_ESPERADA; id++) {
            if (!ids.contains(id)) {
                throw new IOException("Falta el abstract con ID " + id);
            }
        }

        long registrosExistentes = repository.count();
        if (registrosExistentes == 0) {
            repository.saveAll(corpus);
        } else if (registrosExistentes != CANTIDAD_ARTICULOS_ESPERADA) {
            throw new IllegalStateException("La base de datos contiene " + registrosExistentes
                    + " artículos; se esperaban " + CANTIDAD_ARTICULOS_ESPERADA
                    + ". No se modificaron los registros existentes.");
        } else {
            List<Articulo> almacenados = repository.findAllByOrderByIdAsc();
            for (int i = 0; i < corpus.size(); i++) {
                Articulo fuente = corpus.get(i);
                Articulo almacenado = almacenados.get(i);
                if (!fuente.getId().equals(almacenado.getId())
                        || !fuente.getTitulo().equals(almacenado.getTitulo())
                        || !fuente.getAbstractText().equals(almacenado.getAbstractText())) {
                    throw new IllegalStateException("El artículo " + fuente.getId()
                            + " en la base de datos no coincide con el corpus incluido. "
                            + "No se modificaron los registros existentes.");
                }
            }
        }
    }

    private Articulo leerArticulo(File archivo) {
        try {
            String nombre = archivo.getName();
            int separador = nombre.indexOf('.');
            int id = Integer.parseInt(nombre.substring(0, separador));
            String titulo = nombre.substring(separador + 1, nombre.length() - ".txt".length()).trim();
            String abstractText = Files.readString(archivo.toPath(), StandardCharsets.UTF_8).trim();
            if (titulo.isBlank() || abstractText.isBlank()) {
                throw new IOException("El título y el abstract deben tener contenido: " + archivo.getName());
            }
            return new Articulo(id, titulo, abstractText);
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("No se pudo importar el archivo " + archivo.getAbsolutePath(), e);
        }
    }

    private static int idArchivo(File archivo) {
        String nombre = archivo.getName();
        int separador = nombre.indexOf('.');
        if (separador <= 0) {
            throw new IllegalArgumentException("El archivo debe iniciar con un ID numérico: " + nombre);
        }
        try {
            return Integer.parseInt(nombre.substring(0, separador));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El archivo debe iniciar con un ID numérico: " + nombre, e);
        }
    }
}
