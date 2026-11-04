package com.proyecto.bibliometria.config;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.proyecto.bibliometria.corpus.Preprocesador;
import com.proyecto.bibliometria.r1_similitud.algo_ia.MiniLMEmbeddingsCache;

@Configuration
public class AlgoritmoConfig {

    @Bean
    Preprocesador preprocesador() throws IOException {
        return new Preprocesador();
    }

    @Bean
    MiniLMEmbeddingsCache miniLMEmbeddingsCache(
            @Value("${app.minilm.embedding-file:embeddings/minilm_abstracts.txt}") String archivo)
            throws IOException {
        return new MiniLMEmbeddingsCache(Path.of(archivo));
    }
}
