package com.proyecto.bibliometria.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
    void buscaPatronesConLosTresAlgoritmosEnElCorpus() throws Exception {
        for (String algorithm : new String[] { "KMP", "BOYER_MOORE", "RABIN_KARP" }) {
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
    void generaLosTresAgrupamientosJerarquicos() throws Exception {
        for (String linkage : new String[] { "SINGLE", "COMPLETE", "AVERAGE" }) {
            mockMvc.perform(get("/api/v1/clusters").param("linkage", linkage))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.merges", hasSize(19)))
                    .andExpect(jsonPath("$.articles", hasSize(20)))
                    .andExpect(jsonPath("$.copheneticCoefficient").isNumber());
        }
    }
}
