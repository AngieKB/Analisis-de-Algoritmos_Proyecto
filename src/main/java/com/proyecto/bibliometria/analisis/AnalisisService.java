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
import com.proyecto.bibliometria.api.dto.ApiDtos.ClusterCandidate;
import com.proyecto.bibliometria.api.dto.ApiDtos.ClusterDistanceUpdate;
import com.proyecto.bibliometria.api.dto.ApiDtos.ClusterSnapshot;
import com.proyecto.bibliometria.api.dto.ApiDtos.ClusterTraceResponse;
import com.proyecto.bibliometria.api.dto.ApiDtos.ClusterTraceStep;
import com.proyecto.bibliometria.api.dto.ApiDtos.CopheneticPair;
import com.proyecto.bibliometria.api.dto.ApiDtos.CopheneticTrace;
import com.proyecto.bibliometria.api.dto.ApiDtos.Linkage;
import com.proyecto.bibliometria.api.dto.ApiDtos.SearchAlgorithm;
import com.proyecto.bibliometria.api.dto.ApiDtos.SearchMetrics;
import com.proyecto.bibliometria.api.dto.ApiDtos.SearchRequest;
import com.proyecto.bibliometria.api.dto.ApiDtos.SearchResponse;
import com.proyecto.bibliometria.api.dto.ApiDtos.SearchTraceRequest;
import com.proyecto.bibliometria.api.dto.ApiDtos.SearchTraceResponse;
import com.proyecto.bibliometria.api.dto.ApiDtos.SimilarityAlgorithm;
import com.proyecto.bibliometria.api.dto.ApiDtos.SimilarityPairResponse;
import com.proyecto.bibliometria.api.dto.ApiDtos.SimilarityRequest;
import com.proyecto.bibliometria.api.dto.ApiDtos.SimilarityResponse;
import com.proyecto.bibliometria.api.dto.ApiDtos.SimilarityTraceRequest;
import com.proyecto.bibliometria.api.dto.ApiDtos.SimilarityTraceResponse;
import com.proyecto.bibliometria.api.dto.ApiDtos.ArticleReference;
import com.proyecto.bibliometria.api.dto.ApiDtos.CosineTfidfTrace;
import com.proyecto.bibliometria.api.dto.ApiDtos.DynamicProgrammingTrace;
import com.proyecto.bibliometria.api.dto.ApiDtos.EmbeddingComponentTrace;
import com.proyecto.bibliometria.api.dto.ApiDtos.GloveTrace;
import com.proyecto.bibliometria.api.dto.ApiDtos.GloveVectorTrace;
import com.proyecto.bibliometria.api.dto.ApiDtos.GloveWordVectorTrace;
import com.proyecto.bibliometria.api.dto.ApiDtos.JaccardTrace;
import com.proyecto.bibliometria.api.dto.ApiDtos.MiniLmTrace;
import com.proyecto.bibliometria.api.dto.ApiDtos.TfidfTermTrace;
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
import com.proyecto.bibliometria.r3_busqueda.BusquedaIngenua;
import com.proyecto.bibliometria.r3_busqueda.KMP;
import com.proyecto.bibliometria.r3_busqueda.MetricasBusqueda;
import com.proyecto.bibliometria.r3_busqueda.RabinKarp;
import com.proyecto.bibliometria.r3_busqueda.TrazadorBusqueda;

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

    public SimilarityTraceResponse trazar(SimilarityTraceRequest request) {
        if (request.articleAId() == request.articleBId()) {
            throw new IllegalArgumentException("Selecciona dos artículos diferentes para generar la traza");
        }
        Articulo articuloA = articuloService.obtener(request.articleAId());
        Articulo articuloB = articuloService.obtener(request.articleBId());
        List<String> tokensA = preprocesador.tokenizar(articuloA.getAbstractText());
        List<String> tokensB = preprocesador.tokenizar(articuloB.getAbstractText());
        List<List<String>> corpusTokens = null;
        if (request.algorithm() == SimilarityAlgorithm.COSINE_TFIDF
                || request.algorithm() == SimilarityAlgorithm.GLOVE_AVERAGE) {
            corpusTokens = articuloService.obtenerTodos().stream()
                    .map(articulo -> preprocesador.tokenizar(articulo.getAbstractText()))
                    .toList();
        }

        DynamicProgrammingTrace levenshteinTrace = null;
        DynamicProgrammingTrace lcsTrace = null;
        JaccardTrace jaccardTrace = null;
        CosineTfidfTrace tfidfTrace = null;
        GloveTrace gloveTrace = null;
        MiniLmTrace miniLmTrace = null;
        double similarity = switch (request.algorithm()) {
            case LEVENSHTEIN -> {
                Levenshtein.Traza detalle = Levenshtein.trazar(tokensA, tokensB);
                levenshteinTrace = new DynamicProgrammingTrace(
                        detalle.tokensA(),
                        detalle.tokensB(),
                        matrizComoLista(detalle.matrix()),
                        "D[i][j] = min(D[i-1][j] + 1, D[i][j-1] + 1, D[i-1][j-1] + costo); costo=0 si los tokens coinciden, 1 si difieren.",
                        null,
                        detalle.editDistance(),
                        detalle.normalizationLength(),
                        detalle.similarity());
                yield detalle.similarity();
            }
            case LCS -> {
                LCS.Traza detalle = LCS.trazar(tokensA, tokensB);
                lcsTrace = new DynamicProgrammingTrace(
                        detalle.tokensA(),
                        detalle.tokensB(),
                        matrizComoLista(detalle.matrix()),
                        "L[i][j] = L[i-1][j-1] + 1 si los tokens coinciden; de lo contrario max(L[i-1][j], L[i][j-1]).",
                        detalle.longestCommonSubsequence(),
                        detalle.lcsLength(),
                        detalle.normalizationLength(),
                        detalle.similarity());
                yield detalle.similarity();
            }
            case JACCARD -> {
                Jaccard.Traza detalle = Jaccard.trazar(tokensA, tokensB);
                jaccardTrace = new JaccardTrace(
                        detalle.setA(),
                        detalle.setB(),
                        detalle.intersection(),
                        detalle.union(),
                        detalle.intersectionSize(),
                        detalle.unionSize(),
                        detalle.similarity());
                yield detalle.similarity();
            }
            case COSINE_TFIDF -> {
                CosenoTFIDF.DetalleCoseno detalle = new CosenoTFIDF(corpusTokens).traza(tokensA, tokensB);
                tfidfTrace = new CosineTfidfTrace(
                        detalle.documentCount(),
                        detalle.formula(),
                        detalle.terms().stream()
                                .map(term -> new TfidfTermTrace(
                                        term.term(),
                                        term.documentFrequency(),
                                        term.termFrequencyA(),
                                        term.termFrequencyB(),
                                        term.idf(),
                                        term.weightA(),
                                        term.weightB(),
                                        term.dotProductContribution()))
                                .toList(),
                        detalle.dotProduct(),
                        detalle.normSquaredA(),
                        detalle.normSquaredB(),
                        detalle.normA(),
                        detalle.normB(),
                        detalle.denominator(),
                        detalle.similarity());
                yield detalle.similarity();
            }
            case GLOVE_AVERAGE -> {
                GloVePromedio.DetalleCoseno detalle = obtenerGlove(corpusTokens).trazar(tokensA, tokensB);
                gloveTrace = new GloveTrace(
                        detalle.dimensions(),
                        detalle.averagingFormula(),
                        convertirVectorGlove(detalle.vectorA()),
                        convertirVectorGlove(detalle.vectorB()),
                        detalle.components().stream()
                                .map(component -> new EmbeddingComponentTrace(
                                        component.index(),
                                        component.valueA(),
                                        component.valueB(),
                                        component.product()))
                                .toList(),
                        detalle.dotProduct(),
                        detalle.normSquaredA(),
                        detalle.normSquaredB(),
                        detalle.normA(),
                        detalle.normB(),
                        detalle.denominator(),
                        detalle.similarity());
                yield detalle.similarity();
            }
            case MINILM -> {
                MiniLMEmbeddingsCache.DetalleCoseno detalle = miniLM.traza(
                        articuloA.getId(), articuloB.getId());
                miniLmTrace = new MiniLmTrace(
                        detalle.dimensions(),
                        detalle.embeddingSource(),
                        detalle.components().stream()
                                .map(component -> new EmbeddingComponentTrace(
                                        component.index(),
                                        component.valueA(),
                                        component.valueB(),
                                        component.product()))
                                .toList(),
                        detalle.dotProduct(),
                        detalle.normSquaredA(),
                        detalle.normSquaredB(),
                        detalle.normA(),
                        detalle.normB(),
                        detalle.denominator(),
                        detalle.similarity());
                yield detalle.similarity();
            }
        };

        return new SimilarityTraceResponse(
                new ArticleReference(articuloA.getId(), articuloA.getTitulo()),
                new ArticleReference(articuloB.getId(), articuloB.getTitulo()),
                request.algorithm(),
                similarity,
                levenshteinTrace,
                lcsTrace,
                jaccardTrace,
                tfidfTrace,
                gloveTrace,
                miniLmTrace);
    }

    private static List<List<Integer>> matrizComoLista(int[][] matriz) {
        return java.util.Arrays.stream(matriz)
                .map(fila -> java.util.Arrays.stream(fila).boxed().toList())
                .toList();
    }

    private static GloveVectorTrace convertirVectorGlove(GloVePromedio.DetalleVector vector) {
        return new GloveVectorTrace(
                vector.knownWordVectors().stream()
                        .map(word -> new GloveWordVectorTrace(word.token(), word.vector()))
                        .toList(),
                vector.unknownTokens(),
                vector.averageVector(),
                vector.knownTokenCount());
    }

    public SearchResponse buscar(SearchRequest request) {
        Buscador buscador = switch (request.algorithm()) {
            case NAIVE -> new BusquedaIngenua();
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

    public SearchTraceResponse trazarBusqueda(SearchTraceRequest request) {
        String pattern = request.pattern().trim();
        if (pattern.isEmpty()) {
            throw new IllegalArgumentException("El patrón de búsqueda no puede estar vacío");
        }
        Articulo articulo = articuloService.obtener(request.articleId());
        String searchableText = articulo.getAbstractText();
        String normalizedPattern = pattern;
        if (request.ignoreCase()) {
            searchableText = searchableText.toLowerCase(java.util.Locale.ROOT);
            normalizedPattern = pattern.toLowerCase(java.util.Locale.ROOT);
        }

        Buscador buscador = switch (request.algorithm()) {
            case NAIVE -> new BusquedaIngenua();
            case KMP -> new KMP();
            case BOYER_MOORE -> new BoyerMoore();
            case RABIN_KARP -> new RabinKarp();
        };
        MetricasBusqueda metricas = new MetricasBusqueda();
        List<Integer> positions = buscador.buscar(searchableText, normalizedPattern, metricas);
        TrazadorBusqueda.Resultado resultadoTraza = TrazadorBusqueda.trazar(
                searchableText, normalizedPattern, request.algorithm());
        if (!positions.equals(resultadoTraza.positions())) {
            throw new IllegalStateException("La traza de búsqueda no coincide con el resultado del algoritmo");
        }

        return new SearchTraceResponse(
                pattern,
                normalizedPattern,
                request.algorithm(),
                request.ignoreCase(),
                articulo.getId(),
                articulo.getTitulo(),
                searchableText,
                true,
                positions,
                positions.size(),
                SearchMetrics.from(metricas),
                resultadoTraza.trace());
    }

    public ClusterResponse agrupar(Linkage linkage) {
        List<Articulo> articulos = articuloService.obtenerTodos();
        List<List<String>> corpusTokens = articulos.stream()
                .map(articulo -> preprocesador.tokenizar(articulo.getAbstractText()))
                .toList();
        double[][] distancias = construirMatrizDistancias(corpusTokens);
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

    public ClusterTraceResponse trazarAgrupamiento(Linkage linkage) {
        List<Articulo> articulos = articuloService.obtenerTodos();
        List<List<String>> corpusTokens = articulos.stream()
                .map(articulo -> preprocesador.tokenizar(articulo.getAbstractText()))
                .toList();
        double[][] distancias = construirMatrizDistancias(corpusTokens);
        int n = articulos.size();
        double[][] matrizTrabajo = copiarMatriz(distancias);
        int[] idGrupo = new int[n];
        int[] tamano = new int[n];
        boolean[] activo = new boolean[n];
        List<List<Integer>> miembros = new ArrayList<>(2 * n - 1);
        for (int i = 0; i < n; i++) {
            idGrupo[i] = i;
            tamano[i] = 1;
            activo[i] = true;
            miembros.add(new ArrayList<>(List.of(i)));
        }

        List<ClusterTraceStep> pasos = new ArrayList<>(Math.max(0, n - 1));
        List<Fusion> fusiones = new ArrayList<>(Math.max(0, n - 1));
        for (int paso = 0; paso < n - 1; paso++) {
            List<ClusterSnapshot> activosAntes = snapshotsActivos(idGrupo, tamano, activo, miembros, articulos);
            List<ClusterCandidate> candidatos = new ArrayList<>();
            int mejorI = -1;
            int mejorJ = -1;
            double minimo = Double.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                if (!activo[i]) {
                    continue;
                }
                for (int j = i + 1; j < n; j++) {
                    if (activo[j]) {
                        double distancia = matrizTrabajo[i][j];
                        candidatos.add(new ClusterCandidate(idGrupo[i], idGrupo[j], distancia));
                        if (distancia < minimo) {
                            minimo = distancia;
                            mejorI = i;
                            mejorJ = j;
                        }
                    }
                }
            }
            if (mejorI < 0 || mejorJ < 0) {
                throw new IllegalStateException("No se encontró un par de clusters activo para fusionar");
            }

            int clusterA = idGrupo[mejorI];
            int clusterB = idGrupo[mejorJ];
            int nuevoId = n + paso;
            int tamanoA = tamano[mejorI];
            int tamanoB = tamano[mejorJ];
            List<ClusterDistanceUpdate> actualizaciones = new ArrayList<>();
            for (int k = 0; k < n; k++) {
                if (!activo[k] || k == mejorI || k == mejorJ) {
                    continue;
                }
                double distanciaA = matrizTrabajo[mejorI][k];
                double distanciaB = matrizTrabajo[mejorJ][k];
                double nuevaDistancia;
                String calculo;
                switch (linkage) {
                    case SINGLE -> {
                        nuevaDistancia = Math.min(distanciaA, distanciaB);
                        calculo = "min(" + distanciaA + ", " + distanciaB + ")";
                    }
                    case COMPLETE -> {
                        nuevaDistancia = Math.max(distanciaA, distanciaB);
                        calculo = "max(" + distanciaA + ", " + distanciaB + ")";
                    }
                    case AVERAGE -> {
                        nuevaDistancia = (tamanoA * distanciaA + tamanoB * distanciaB)
                                / (tamanoA + tamanoB);
                        calculo = "((" + tamanoA + " * " + distanciaA + ") + ("
                                + tamanoB + " * " + distanciaB + ")) / ("
                                + (tamanoA + tamanoB) + ")";
                    }
                    default -> throw new IllegalStateException("Método de enlace no soportado: " + linkage);
                }
                actualizaciones.add(new ClusterDistanceUpdate(
                        nuevoId,
                        idGrupo[k],
                        tamanoA,
                        tamanoB,
                        distanciaA,
                        distanciaB,
                        calculo,
                        nuevaDistancia));
                matrizTrabajo[mejorI][k] = nuevaDistancia;
                matrizTrabajo[k][mejorI] = nuevaDistancia;
            }

            List<Integer> miembrosFusionados = new ArrayList<>(miembros.get(clusterA));
            miembrosFusionados.addAll(miembros.get(clusterB));
            miembros.add(miembrosFusionados);
            fusiones.add(new Fusion(clusterA, clusterB, minimo, tamanoA + tamanoB));
            activo[mejorJ] = false;
            idGrupo[mejorI] = nuevoId;
            tamano[mejorI] = tamanoA + tamanoB;

            pasos.add(new ClusterTraceStep(
                    paso + 1,
                    activosAntes,
                    List.copyOf(candidatos),
                    new ClusterCandidate(clusterA, clusterB, minimo),
                    nuevoId,
                    tamanoA + tamanoB,
                    List.copyOf(actualizaciones),
                    snapshotsActivos(idGrupo, tamano, activo, miembros, articulos)));
        }

        CoeficienteCofenetico.Traza coeficiente = CoeficienteCofenetico.trazar(distancias, fusiones);
        List<CopheneticPair> paresCofeneticos = coeficiente.pairs().stream()
                .map(pair -> new CopheneticPair(
                        articulos.get(pair.articleAIndex()).getId(),
                        articulos.get(pair.articleBIndex()).getId(),
                        pair.originalDistance(),
                        pair.copheneticDistance(),
                        pair.centeredOriginalDistance(),
                        pair.centeredCopheneticDistance(),
                        pair.covarianceContribution()))
                .toList();

        return new ClusterTraceResponse(
                linkage,
                formulaEnlace(linkage),
                articulos.stream()
                        .map(articulo -> new ArticleReference(articulo.getId(), articulo.getTitulo()))
                        .toList(),
                matrizComoLista(distancias),
                List.copyOf(pasos),
                new CopheneticTrace(
                        coeficiente.pairCount(),
                        coeficiente.meanOriginalDistance(),
                        coeficiente.meanCopheneticDistance(),
                        coeficiente.covarianceSum(),
                        coeficiente.originalVarianceSum(),
                        coeficiente.copheneticVarianceSum(),
                        coeficiente.coefficient(),
                        paresCofeneticos));
    }

    private static double[][] construirMatrizDistancias(List<List<String>> corpusTokens) {
        int n = corpusTokens.size();
        CosenoTFIDF tfidf = new CosenoTFIDF(corpusTokens);
        double[][] distancias = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double distancia = Math.max(0.0, 1.0 - tfidf.similitud(corpusTokens.get(i), corpusTokens.get(j)));
                distancias[i][j] = distancia;
                distancias[j][i] = distancia;
            }
        }
        return distancias;
    }

    private static double[][] copiarMatriz(double[][] matriz) {
        double[][] copia = new double[matriz.length][];
        for (int i = 0; i < matriz.length; i++) {
            copia[i] = matriz[i].clone();
        }
        return copia;
    }

    private static List<List<Double>> matrizComoLista(double[][] matriz) {
        return java.util.Arrays.stream(matriz)
                .map(fila -> java.util.Arrays.stream(fila).boxed().toList())
                .toList();
    }

    private static List<ClusterSnapshot> snapshotsActivos(
            int[] idGrupo,
            int[] tamano,
            boolean[] activo,
            List<List<Integer>> miembros,
            List<Articulo> articulos) {
        List<ClusterSnapshot> resultado = new ArrayList<>();
        for (int slot = 0; slot < activo.length; slot++) {
            if (activo[slot]) {
                List<Integer> idsArticulos = miembros.get(idGrupo[slot]).stream()
                        .map(indice -> articulos.get(indice).getId())
                        .toList();
                resultado.add(new ClusterSnapshot(idGrupo[slot], idsArticulos, tamano[slot]));
            }
        }
        return List.copyOf(resultado);
    }

    private static String formulaEnlace(Linkage linkage) {
        return switch (linkage) {
            case SINGLE -> "d(A∪B,C) = min(d(A,C), d(B,C))";
            case COMPLETE -> "d(A∪B,C) = max(d(A,C), d(B,C))";
            case AVERAGE -> "d(A∪B,C) = (|A|·d(A,C) + |B|·d(B,C)) / (|A| + |B|)";
        };
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
