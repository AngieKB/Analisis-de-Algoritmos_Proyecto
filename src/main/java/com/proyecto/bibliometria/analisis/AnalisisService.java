package com.proyecto.bibliometria.analisis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.proyecto.bibliometria.api.dto.ApiDtos.AlgorithmsResponse;
import com.proyecto.bibliometria.api.dto.ApiDtos.ArticleResponse;
import com.proyecto.bibliometria.api.dto.ApiDtos.ArticleSearchResult;
import com.proyecto.bibliometria.api.dto.ApiDtos.ClusterMerge;
import com.proyecto.bibliometria.api.dto.ApiDtos.ClusterResponse;
import com.proyecto.bibliometria.api.dto.ApiDtos.Linkage;
import com.proyecto.bibliometria.api.dto.ApiDtos.SearchAlgorithm;
import com.proyecto.bibliometria.api.dto.ApiDtos.SearchMetrics;
import com.proyecto.bibliometria.api.dto.ApiDtos.SearchRequest;
import com.proyecto.bibliometria.api.dto.ApiDtos.SearchResponse;
import com.proyecto.bibliometria.api.dto.ApiDtos.SimilarityAlgorithm;
import com.proyecto.bibliometria.api.dto.ApiDtos.SimilarityPairResponse;
import com.proyecto.bibliometria.api.dto.ApiDtos.SimilarityRequest;
import com.proyecto.bibliometria.api.dto.ApiDtos.SimilarityResponse;
import com.proyecto.bibliometria.articulos.Articulo;
import com.proyecto.bibliometria.articulos.ArticuloService;
import com.proyecto.bibliometria.corpus.Preprocesador;
import com.proyecto.bibliometria.r1_similitud.algo_clasicos.CosenoTFIDF;
import com.proyecto.bibliometria.r1_similitud.algo_clasicos.Jaccard;
import com.proyecto.bibliometria.r1_similitud.algo_clasicos.LCS;
import com.proyecto.bibliometria.r1_similitud.algo_clasicos.Levenshtein;
import com.proyecto.bibliometria.r1_similitud.algo_ia.GloVePromedio;
import com.proyecto.bibliometria.r1_similitud.algo_ia.MiniLMEmbeddingsCache;
import com.proyecto.bibliometria.r2_cluster.AverageLinkage;
import com.proyecto.bibliometria.r2_cluster.CoeficienteCofenetico;
import com.proyecto.bibliometria.r2_cluster.CompleteLinkage;
import com.proyecto.bibliometria.r2_cluster.Fusion;
import com.proyecto.bibliometria.r2_cluster.SingleLinkage;
import com.proyecto.bibliometria.r3_busqueda.BoyerMoore;
import com.proyecto.bibliometria.r3_busqueda.Buscador;
import com.proyecto.bibliometria.r3_busqueda.KMP;
import com.proyecto.bibliometria.r3_busqueda.MetricasBusqueda;
import com.proyecto.bibliometria.r3_busqueda.RabinKarp;

@Service
public class AnalisisService {

    private final ArticuloService articuloService;
    private final Preprocesador preprocesador;
    private final MiniLMEmbeddingsCache miniLM;
    private final String archivoGlove;
    private volatile GloVePromedio glove;

    public AnalisisService(
            ArticuloService articuloService,
            Preprocesador preprocesador,
            MiniLMEmbeddingsCache miniLM,
            @Value("${app.glove.embedding-file:embeddings/glove_corpus.txt}") String archivoGlove) {
        this.articuloService = articuloService;
        this.preprocesador = preprocesador;
        this.miniLM = miniLM;
        this.archivoGlove = archivoGlove;
    }

    public AlgorithmsResponse algoritmosDisponibles() {
        return new AlgorithmsResponse(
                List.of(SimilarityAlgorithm.values()),
                List.of(SearchAlgorithm.values()),
                List.of(Linkage.values()));
    }

    public SimilarityResponse comparar(SimilarityRequest request) {
        validarIdsUnicos(request.articleIds());
        List<Articulo> articulos = request.articleIds().stream().map(articuloService::obtener).toList();
        List<List<String>> tokens = articulos.stream()
                .map(articulo -> preprocesador.tokenizar(articulo.getAbstractText()))
                .toList();
        List<List<String>> corpusTokens = articuloService.obtenerTodos().stream()
                .map(articulo -> preprocesador.tokenizar(articulo.getAbstractText()))
                .toList();

        CosenoTFIDF tfidf = request.algorithms().contains(SimilarityAlgorithm.COSINE_TFIDF)
                ? new CosenoTFIDF(corpusTokens)
                : null;
        GloVePromedio modeloGlove = request.algorithms().contains(SimilarityAlgorithm.GLOVE_AVERAGE)
                ? obtenerGlove(corpusTokens)
                : null;

        List<SimilarityPairResponse> resultados = new ArrayList<>();
        for (int i = 0; i < articulos.size(); i++) {
            for (int j = i + 1; j < articulos.size(); j++) {
                for (SimilarityAlgorithm algoritmo : request.algorithms()) {
                    resultados.add(new SimilarityPairResponse(
                            articulos.get(i).getId(),
                            articulos.get(i).getTitulo(),
                            articulos.get(j).getId(),
                            articulos.get(j).getTitulo(),
                            algoritmo,
                            similitud(algoritmo, tokens.get(i), tokens.get(j),
                                    articulos.get(i).getId(), articulos.get(j).getId(), tfidf, modeloGlove)));
                }
            }
        }
        return new SimilarityResponse(request.articleIds(), resultados);
    }

    public SearchResponse buscar(SearchRequest request) {
        Buscador buscador = switch (request.algorithm()) {
            case KMP -> new KMP();
            case BOYER_MOORE -> new BoyerMoore();
            case RABIN_KARP -> new RabinKarp();
        };
        String pattern = request.pattern().trim();
        if (pattern.isEmpty()) {
            throw new IllegalArgumentException("El patrón de búsqueda no puede estar vacío");
        }

        List<ArticleSearchResult> resultados = new ArrayList<>();
        long total = 0;
        for (Articulo articulo : articuloService.obtenerTodos()) {
            String texto = articulo.getAbstractText();
            String patronBusqueda = pattern;
            if (request.ignoreCase()) {
                texto = texto.toLowerCase(java.util.Locale.ROOT);
                patronBusqueda = pattern.toLowerCase(java.util.Locale.ROOT);
            }
            MetricasBusqueda metricas = new MetricasBusqueda();
            List<Integer> posiciones = buscador.buscar(texto, patronBusqueda, metricas);
            resultados.add(new ArticleSearchResult(
                    articulo.getId(),
                    articulo.getTitulo(),
                    posiciones,
                    posiciones.size(),
                    SearchMetrics.from(metricas)));
            total += posiciones.size();
        }
        return new SearchResponse(pattern, request.algorithm(), true, resultados, total);
    }

    public ClusterResponse agrupar(Linkage linkage) {
        List<Articulo> articulos = articuloService.obtenerTodos();
        List<List<String>> corpusTokens = articulos.stream()
                .map(articulo -> preprocesador.tokenizar(articulo.getAbstractText()))
                .toList();
        CosenoTFIDF tfidf = new CosenoTFIDF(corpusTokens);
        double[][] distancias = new double[corpusTokens.size()][corpusTokens.size()];
        for (int i = 0; i < corpusTokens.size(); i++) {
            for (int j = i + 1; j < corpusTokens.size(); j++) {
                double distancia = Math.max(0.0, 1.0 - tfidf.similitud(corpusTokens.get(i), corpusTokens.get(j)));
                distancias[i][j] = distancia;
                distancias[j][i] = distancia;
            }
        }
        List<Fusion> fusiones = switch (linkage) {
            case SINGLE -> SingleLinkage.agrupar(distancias);
            case COMPLETE -> CompleteLinkage.agrupar(distancias);
            case AVERAGE -> AverageLinkage.agrupar(distancias);
        };
        List<ClusterMerge> merges = fusiones.stream()
                .map(fusion -> new ClusterMerge(
                        fusion.clusterA, fusion.clusterB, fusion.distancia, fusion.tamano))
                .toList();
        double coeficiente = CoeficienteCofenetico.calcular(distancias, fusiones);
        return new ClusterResponse(
                linkage,
                coeficiente,
                articulos.stream().map(ArticuloService::toResponse).toList(),
                merges);
    }

    private double similitud(
            SimilarityAlgorithm algorithm,
            List<String> a,
            List<String> b,
            int idA,
            int idB,
            CosenoTFIDF tfidf,
            GloVePromedio modeloGlove) {
        return switch (algorithm) {
            case LEVENSHTEIN -> Levenshtein.similitud(a, b);
            case LCS -> LCS.similitud(a, b);
            case JACCARD -> Jaccard.similitud(a, b);
            case COSINE_TFIDF -> tfidf.similitud(a, b);
            case GLOVE_AVERAGE -> modeloGlove.similitud(a, b);
            case MINILM -> miniLM.similitud(idA, idB);
        };
    }

    private GloVePromedio obtenerGlove(List<List<String>> corpusTokens) {
        GloVePromedio modelo = glove;
        if (modelo == null) {
            synchronized (this) {
                modelo = glove;
                if (modelo == null) {
                    Set<String> vocabulario = new HashSet<>();
                    corpusTokens.forEach(vocabulario::addAll);
                    try {
                        modelo = new GloVePromedio(new File(archivoGlove), vocabulario);
                    } catch (IOException e) {
                        throw new IllegalStateException("No se pudo cargar el archivo de embeddings GloVe "
                                + archivoGlove, e);
                    }
                    glove = modelo;
                }
            }
        }
        return modelo;
    }

    private static void validarIdsUnicos(List<Integer> ids) {
        if (ids.stream().distinct().count() != ids.size()) {
            throw new IllegalArgumentException("La lista de artículos no puede contener IDs duplicados");
        }
    }
}
