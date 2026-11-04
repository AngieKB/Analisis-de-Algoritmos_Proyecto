package com.proyecto.bibliometria.api.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.UniqueElements;

import com.proyecto.bibliometria.r3_busqueda.MetricasBusqueda;

public final class ApiDtos {

    private ApiDtos() {
    }

    public enum SimilarityAlgorithm {
        LEVENSHTEIN,
        LCS,
        JACCARD,
        COSINE_TFIDF,
        GLOVE_AVERAGE,
        MINILM
    }

    public enum SearchAlgorithm {
        KMP,
        BOYER_MOORE,
        RABIN_KARP
    }

    public enum Linkage {
        SINGLE,
        COMPLETE,
        AVERAGE
    }

    public record ArticleResponse(int id, String title, String abstractText) {
    }

    public record SimilarityRequest(
            @NotEmpty @Size(min = 2, max = 20) @UniqueElements List<@NotNull @Positive Integer> articleIds,
            @NotEmpty @UniqueElements List<@NotNull SimilarityAlgorithm> algorithms) {
    }

    public record SimilarityPairResponse(
            int articleAId,
            String articleATitle,
            int articleBId,
            String articleBTitle,
            SimilarityAlgorithm algorithm,
            double similarity) {
    }

    public record SimilarityResponse(List<Integer> articleIds, List<SimilarityPairResponse> results) {
    }

    public record SearchRequest(
            @NotBlank @Size(max = 200) String pattern,
            @NotNull SearchAlgorithm algorithm,
            boolean ignoreCase) {
    }

    public record SearchMetrics(
            long preprocessingNanoseconds,
            long searchNanoseconds,
            long comparisons,
            long hashOperations,
            long hashCollisions) {
        public static SearchMetrics from(MetricasBusqueda metrics) {
            return new SearchMetrics(
                    metrics.preprocesamientoNs,
                    metrics.busquedaNs,
                    metrics.comparaciones,
                    metrics.operacionesHash,
                    metrics.colisionesHash);
        }
    }

    public record ArticleSearchResult(
            int articleId,
            String title,
            List<Integer> positions,
            int occurrences,
            SearchMetrics metrics) {
    }

    public record SearchResponse(
            String pattern,
            SearchAlgorithm algorithm,
            boolean positionsAreZeroBased,
            List<ArticleSearchResult> results,
            long totalOccurrences) {
    }

    public record ClusterMerge(int clusterA, int clusterB, double distance, int size) {
    }

    public record ClusterResponse(
            Linkage linkage,
            double copheneticCoefficient,
            List<ArticleResponse> articles,
            List<ClusterMerge> merges) {
    }

    public record AlgorithmsResponse(
            List<SimilarityAlgorithm> similarity,
            List<SearchAlgorithm> search,
            List<Linkage> clustering) {
    }
}
