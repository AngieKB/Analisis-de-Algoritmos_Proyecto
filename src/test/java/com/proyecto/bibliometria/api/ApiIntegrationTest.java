package com.proyecto.bibliometria.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listaLosVeinteArticulosDesdeLaBaseDeDatos() throws Exception {
        mockMvc.perform(get("/api/v1/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(20)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[19].id").value(20));
    }

    @Test
    void comparaTodosLosParesDeDosOMasArticulosConTodosLosAlgoritmos() throws Exception {
        mockMvc.perform(post("/api/v1/similarities")
                .contentType("application/json")
                .content("""
                        {
                          "articleIds": [1, 2, 3],
                          "algorithms": [
                            "LEVENSHTEIN", "LCS", "JACCARD",
                            "COSINE_TFIDF", "GLOVE_AVERAGE", "MINILM"
                          ]
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.articleIds", hasSize(3)))
                .andExpect(jsonPath("$.results", hasSize(18)))
                .andExpect(jsonPath("$.results[0].similarity").isNumber());
    }

    @Test
    void rechazaComparacionConUnSoloArticulo() throws Exception {
        mockMvc.perform(post("/api/v1/similarities")
                .contentType("application/json")
                .content("""
                        {
                          "articleIds": [1],
                          "algorithms": ["JACCARD"]
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void devuelveLaTrazaPasoAPasoDeCosenoTfidf() throws Exception {
        mockMvc.perform(post("/api/v1/similarities/trace")
                .contentType("application/json")
                .content("""
                        {
                          "articleAId": 11,
                          "articleBId": 16,
                          "algorithm": "COSINE_TFIDF"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.similarity").isNumber())
                .andExpect(jsonPath("$.cosineTfidfTrace.documentCount").value(20))
                .andExpect(jsonPath("$.cosineTfidfTrace.terms").isArray())
                .andExpect(jsonPath("$.cosineTfidfTrace.dotProduct").isNumber())
                .andExpect(jsonPath("$.cosineTfidfTrace.normA").isNumber())
                .andExpect(jsonPath("$.cosineTfidfTrace.normB").isNumber())
                .andExpect(jsonPath("$.miniLmTrace").doesNotExist());
    }

    @Test
    void devuelveLaTrazaDeCosenoSobreLos384ComponentesMiniLm() throws Exception {
        mockMvc.perform(post("/api/v1/similarities/trace")
                .contentType("application/json")
                .content("""
                        {
                          "articleAId": 11,
                          "articleBId": 16,
                          "algorithm": "MINILM"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.similarity").isNumber())
                .andExpect(jsonPath("$.miniLmTrace.dimensions").value(384))
                .andExpect(jsonPath("$.miniLmTrace.components", hasSize(384)))
                .andExpect(jsonPath("$.miniLmTrace.components[0].product").isNumber())
                .andExpect(jsonPath("$.miniLmTrace.dotProduct").isNumber())
                .andExpect(jsonPath("$.cosineTfidfTrace").doesNotExist());
    }

    @Test
    void devuelveTrazaParaTodosLosAlgoritmosYConservaElResultadoNormal() throws Exception {
        String[] algorithms = {
                "LEVENSHTEIN", "LCS", "JACCARD", "COSINE_TFIDF", "GLOVE_AVERAGE", "MINILM"
        };
        for (String algorithm : algorithms) {
            String traceJson = mockMvc.perform(post("/api/v1/similarities/trace")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "articleAId": 11,
                              "articleBId": 16,
                              "algorithm": "%s"
                            }
                            """.formatted(algorithm)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            JsonNode trace = objectMapper.readTree(traceJson);
            String detailKey = switch (algorithm) {
                case "LEVENSHTEIN" -> "levenshteinTrace";
                case "LCS" -> "lcsTrace";
                case "JACCARD" -> "jaccardTrace";
                case "COSINE_TFIDF" -> "cosineTfidfTrace";
                case "GLOVE_AVERAGE" -> "gloveTrace";
                case "MINILM" -> "miniLmTrace";
                default -> throw new IllegalStateException("Unexpected algorithm: " + algorithm);
            };
            org.junit.jupiter.api.Assertions.assertFalse(trace.get(detailKey).isNull(), algorithm);

            String resultJson = mockMvc.perform(post("/api/v1/similarities")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "articleIds": [11, 16],
                              "algorithms": ["%s"]
                            }
                            """.formatted(algorithm)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            JsonNode result = objectMapper.readTree(resultJson).at("/results/0/similarity");
            org.junit.jupiter.api.Assertions.assertEquals(
                    result.doubleValue(),
                    trace.get("similarity").doubleValue(),
                    1e-12,
                    "La traza debe terminar en el mismo resultado del endpoint regular para " + algorithm);
        }
    }

    @Test
    void buscaPatronesConLosTresAlgoritmosEnElCorpus() throws Exception {
        for (String algorithm : new String[] { "NAIVE", "KMP", "BOYER_MOORE", "RABIN_KARP" }) {
            mockMvc.perform(post("/api/v1/search")
                    .contentType("application/json")
                    .content("""
                            {
                              "pattern": "artificial intelligence",
                              "algorithm": "%s",
                              "ignoreCase": true
                            }
                            """.formatted(algorithm)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.positionsAreZeroBased").value(true))
                    .andExpect(jsonPath("$.results", hasSize(20)))
                    .andExpect(jsonPath("$.totalOccurrences").isNumber());
        }
    }

    @Test
    void devuelveTrazaDetalladaDeCadaAlgoritmoDeBusqueda() throws Exception {
        for (String algorithm : new String[] { "NAIVE", "KMP", "BOYER_MOORE", "RABIN_KARP" }) {
            String traceJson = mockMvc.perform(post("/api/v1/search/trace")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "pattern": "artificial intelligence",
                              "algorithm": "%s",
                              "ignoreCase": true,
                              "articleId": 1
                            }
                            """.formatted(algorithm)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            JsonNode trace = objectMapper.readTree(traceJson);
            JsonNode algorithmTrace = trace.get("trace");
            org.junit.jupiter.api.Assertions.assertTrue(algorithmTrace.get("steps").size() > 0, algorithm);
            org.junit.jupiter.api.Assertions.assertEquals(true, trace.get("positionsAreZeroBased").asBoolean());
            org.junit.jupiter.api.Assertions.assertEquals(
                    trace.get("occurrences").asInt(), trace.get("positions").size(), algorithm);

            if (algorithm.equals("KMP")) {
                org.junit.jupiter.api.Assertions.assertTrue(algorithmTrace.get("prefixTable").isArray());
            } else if (algorithm.equals("BOYER_MOORE")) {
                org.junit.jupiter.api.Assertions.assertTrue(algorithmTrace.get("badCharacterTable").isArray());
                org.junit.jupiter.api.Assertions.assertTrue(algorithmTrace.get("goodSuffixTable").isArray());
            } else if (algorithm.equals("RABIN_KARP")) {
                org.junit.jupiter.api.Assertions.assertEquals(256, algorithmTrace.get("hashBase").asInt());
                org.junit.jupiter.api.Assertions.assertTrue(algorithmTrace.get("hashModulus").isNumber());
            }

            String searchJson = mockMvc.perform(post("/api/v1/search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "pattern": "artificial intelligence",
                              "algorithm": "%s",
                              "ignoreCase": true
                            }
                            """.formatted(algorithm)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            JsonNode search = objectMapper.readTree(searchJson);
            org.junit.jupiter.api.Assertions.assertEquals(
                    search.at("/results/0/positions"), trace.get("positions"), algorithm);
        }
    }

    @Test
    void lasTrazasDeBusquedaCubrenSolapamientosYPatronesSinCoincidencias() throws Exception {
        for (String algorithm : new String[] { "NAIVE", "KMP", "BOYER_MOORE", "RABIN_KARP" }) {
            for (String pattern : new String[] { "a", "review", "zzzzzz" }) {
                mockMvc.perform(post("/api/v1/search/trace")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "pattern", pattern,
                                "algorithm", algorithm,
                                "ignoreCase", true,
                                "articleId", 1))))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.trace.steps").isArray())
                        .andExpect(jsonPath("$.occurrences").isNumber());
            }
        }
    }

    @Test
    void generaLosTresAgrupamientosJerarquicos() throws Exception {
        for (String linkage : new String[] { "SINGLE", "COMPLETE", "AVERAGE" }) {
            mockMvc.perform(get("/api/v1/clusters").param("linkage", linkage))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.merges", hasSize(19)))
                    .andExpect(jsonPath("$.articles", hasSize(20)))
                    .andExpect(jsonPath("$.copheneticCoefficient").isNumber());
        }
    }

    @Test
    void lasTrazasDeClusteringDetallanCadaPasoYCoincidenConElEndpointNormal() throws Exception {
        for (String linkage : new String[] { "SINGLE", "COMPLETE", "AVERAGE" }) {
            JsonNode trace = objectMapper.readTree(mockMvc.perform(
                    get("/api/v1/clusters/trace").param("linkage", linkage))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString());
            JsonNode regular = objectMapper.readTree(mockMvc.perform(
                    get("/api/v1/clusters").param("linkage", linkage))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString());

            org.junit.jupiter.api.Assertions.assertEquals(20, trace.get("articles").size());
            org.junit.jupiter.api.Assertions.assertEquals(20, trace.get("initialDistanceMatrix").size());
            org.junit.jupiter.api.Assertions.assertEquals(19, trace.get("steps").size());
            org.junit.jupiter.api.Assertions.assertEquals(190, trace.at("/copheneticTrace/pairCount").asInt());
            org.junit.jupiter.api.Assertions.assertEquals(190, trace.at("/copheneticTrace/pairs").size());

            for (int step = 0; step < trace.get("steps").size(); step++) {
                JsonNode traceStep = trace.get("steps").get(step);
                JsonNode selected = traceStep.get("selectedMerge");
                JsonNode regularMerge = regular.get("merges").get(step);
                org.junit.jupiter.api.Assertions.assertEquals(
                        regularMerge.get("clusterA").asInt(), selected.get("clusterA").asInt(), linkage);
                org.junit.jupiter.api.Assertions.assertEquals(
                        regularMerge.get("clusterB").asInt(), selected.get("clusterB").asInt(), linkage);
                org.junit.jupiter.api.Assertions.assertEquals(
                        regularMerge.get("distance").asDouble(), selected.get("distance").asDouble(), 1e-12, linkage);
                org.junit.jupiter.api.Assertions.assertEquals(
                        regularMerge.get("size").asInt(), traceStep.get("mergedSize").asInt(), linkage);
            }
        }
    }
}
