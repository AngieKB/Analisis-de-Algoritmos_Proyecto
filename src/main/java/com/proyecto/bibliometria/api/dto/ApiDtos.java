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
        NAIVE,
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

    public record SimilarityTraceRequest(
            @Positive int articleAId,
            @Positive int articleBId,
            @NotNull SimilarityAlgorithm algorithm) {
    }

    public record ArticleReference(int id, String title) {
    }

    public record SimilarityTraceResponse(
            ArticleReference articleA,
            ArticleReference articleB,
            SimilarityAlgorithm algorithm,
            double similarity,
            DynamicProgrammingTrace levenshteinTrace,
            DynamicProgrammingTrace lcsTrace,
            JaccardTrace jaccardTrace,
            CosineTfidfTrace cosineTfidfTrace,
            GloveTrace gloveTrace,
            MiniLmTrace miniLmTrace) {
    }

    public record DynamicProgrammingTrace(
            List<String> tokensA,
            List<String> tokensB,
            List<List<Integer>> matrix,
            String recurrence,
            List<String> longestCommonSubsequence,
            Integer rawDistanceOrLength,
            int normalizationLength,
            double similarity) {
    }

    public record JaccardTrace(
            List<String> setA,
            List<String> setB,
            List<String> intersection,
            List<String> union,
            int intersectionSize,
            int unionSize,
            double similarity) {
    }

    public record CosineTfidfTrace(
            int documentCount,
            String formula,
            List<TfidfTermTrace> terms,
            double dotProduct,
            double normSquaredA,
            double normSquaredB,
            double normA,
            double normB,
            double denominator,
            double similarity) {
    }

    public record TfidfTermTrace(
            String term,
            int documentFrequency,
            int termFrequencyA,
            int termFrequencyB,
            double idf,
            double weightA,
            double weightB,
            double dotProductContribution) {
    }

    public record MiniLmTrace(
            int dimensions,
            String embeddingSource,
            List<EmbeddingComponentTrace> components,
            double dotProduct,
            double normSquaredA,
            double normSquaredB,
            double normA,
            double normB,
            double denominator,
            double similarity) {
    }

    public record EmbeddingComponentTrace(
            int index,
            double valueA,
            double valueB,
            double product) {
    }

    public record GloveTrace(
            int dimensions,
            String averagingFormula,
            GloveVectorTrace vectorA,
            GloveVectorTrace vectorB,
            List<EmbeddingComponentTrace> components,
            double dotProduct,
            double normSquaredA,
            double normSquaredB,
            double normA,
            double normB,
            double denominator,
            double similarity) {
    }

    public record GloveVectorTrace(
            List<GloveWordVectorTrace> knownWordVectors,
            List<String> unknownTokens,
            List<Double> averageVector,
            int knownTokenCount) {
    }

    public record GloveWordVectorTrace(String token, List<Double> vector) {
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

    public record SearchTraceRequest(
            @NotBlank @Size(max = 200) String pattern,
            @NotNull SearchAlgorithm algorithm,
            boolean ignoreCase,
            @NotNull @Positive Integer articleId) {
    }

    public record SearchTraceResponse(
            String pattern,
            String normalizedPattern,
            SearchAlgorithm algorithm,
            boolean ignoreCase,
            int articleId,
            String title,
            String searchableText,
            boolean positionsAreZeroBased,
            List<Integer> positions,
            int occurrences,
            SearchMetrics metrics,
            SearchAlgorithmTrace trace) {
    }

    public record SearchAlgorithmTrace(
            List<Integer> prefixTable,
            List<BadCharacterEntry> badCharacterTable,
            List<Integer> goodSuffixTable,
            int hashBase,
            long hashModulus,
            long highestPower,
            long patternHash,
            List<SearchTraceStep> steps) {
    }

    public record BadCharacterEntry(String character, int lastPatternIndex) {
    }

    public record SearchTraceStep(
            int step,
            String action,
            Integer alignment,
            Integer textIndex,
            Integer patternIndex,
            String textCharacter,
            String patternCharacter,
            Boolean equal,
            Integer badCharacterShift,
            Integer goodSuffixShift,
            Integer shift,
            Long patternHash,
            Long windowHash,
            Long nextWindowHash,
            String explanation) {
    }

    public record ClusterMerge(int clusterA, int clusterB, double distance, int size) {
    }

    public record ClusterResponse(
            Linkage linkage,
            double copheneticCoefficient,
            List<ArticleResponse> articles,
            List<ClusterMerge> merges) {
    }

    public record ClusterTraceResponse(
            Linkage linkage,
            String linkageFormula,
            List<ArticleReference> articles,
            List<List<Double>> initialDistanceMatrix,
            List<ClusterTraceStep> steps,
            CopheneticTrace copheneticTrace) {
    }

    public record ClusterTraceStep(
            int step,
            List<ClusterSnapshot> activeClustersBefore,
            List<ClusterCandidate> candidates,
            ClusterCandidate selectedMerge,
            int newClusterId,
            int mergedSize,
            List<ClusterDistanceUpdate> distanceUpdates,
            List<ClusterSnapshot> activeClustersAfter) {
    }

    public record ClusterSnapshot(int clusterId, List<Integer> articleIds, int size) {
    }

    public record ClusterCandidate(int clusterA, int clusterB, double distance) {
    }

    public record ClusterDistanceUpdate(
            int newClusterId,
            int otherClusterId,
            int sizeA,
            int sizeB,
            double distanceA,
            double distanceB,
            String calculation,
            double updatedDistance) {
    }

    public record CopheneticTrace(
            int pairCount,
            double meanOriginalDistance,
            double meanCopheneticDistance,
            double covarianceSum,
            double originalVarianceSum,
            double copheneticVarianceSum,
            double coefficient,
            List<CopheneticPair> pairs) {
    }

    public record CopheneticPair(
            int articleAId,
            int articleBId,
            double originalDistance,
            double copheneticDistance,
            double centeredOriginalDistance,
            double centeredCopheneticDistance,
            double covarianceContribution) {
    }

    public record AlgorithmsResponse(
            List<SimilarityAlgorithm> similarity,
            List<SearchAlgorithm> search,
            List<Linkage> clustering) {
    }
}
