package com.proyecto.bibliometria.r3_busqueda;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.proyecto.bibliometria.api.dto.ApiDtos.BadCharacterEntry;
import com.proyecto.bibliometria.api.dto.ApiDtos.SearchAlgorithm;
import com.proyecto.bibliometria.api.dto.ApiDtos.SearchAlgorithmTrace;
import com.proyecto.bibliometria.api.dto.ApiDtos.SearchTraceStep;

public final class TrazadorBusqueda {

    private static final int HASH_BASE = 256;
    private static final long HASH_MODULUS = 1_000_000_007L;

    private TrazadorBusqueda() {
    }

    public record Resultado(List<Integer> positions, SearchAlgorithmTrace trace) {
    }

    public static Resultado trazar(String text, String pattern, SearchAlgorithm algorithm) {
        return switch (algorithm) {
            case NAIVE -> ingenua(text, pattern);
            case KMP -> kmp(text, pattern);
            case BOYER_MOORE -> boyerMoore(text, pattern);
            case RABIN_KARP -> rabinKarp(text, pattern);
        };
    }

    private static Resultado ingenua(String text, String pattern) {
        List<Integer> positions = new ArrayList<>();
        List<SearchTraceStep> steps = new ArrayList<>();
        int step = 1;
        int n = text.length();
        int m = pattern.length();
        if (m == 0 || m > n) {
            return noWindow(positions, "El patrón está vacío o es más largo que el texto; no se prueban alineamientos.");
        }
        for (int alignment = 0; alignment <= n - m; alignment++) {
            int j = 0;
            while (j < m) {
                boolean equal = text.charAt(alignment + j) == pattern.charAt(j);
                steps.add(comparison(step++, "COMPARISON", alignment, alignment + j, j, text, pattern, equal,
                        "Fuerza bruta compara de izquierda a derecha; cada nuevo intento avanza una posición."));
                if (!equal) {
                    break;
                }
                j++;
            }
            if (j == m) {
                positions.add(alignment);
                steps.add(event(step++, "MATCH", alignment, 1, null, null, null,
                        "Coincidencia exacta; se conserva el avance de una posición para permitir solapamientos."));
            } else {
                steps.add(event(step++, "SHIFT", alignment, 1, null, null, null,
                        "El patrón se desplaza una posición y las comparaciones comienzan nuevamente desde el inicio."));
            }
        }
        return result(positions, null, null, null, 0, 0, 0, steps);
    }

    private static Resultado kmp(String text, String pattern) {
        List<Integer> positions = new ArrayList<>();
        List<SearchTraceStep> steps = new ArrayList<>();
        if (pattern.isEmpty() || pattern.length() > text.length()) {
            return noWindow(positions, "El patrón está vacío o es más largo que el texto; KMP retorna sin preprocesar.");
        }
        List<Integer> prefix = java.util.Arrays.stream(KMP.tablaPrefijos(pattern)).boxed().toList();
        int step = 1;
        int i = 0;
        int j = 0;
        while (i < text.length() && !pattern.isEmpty()) {
            int alignment = i - j;
            boolean equal = text.charAt(i) == pattern.charAt(j);
            steps.add(comparison(step++, equal ? "COMPARISON_MATCH" : "COMPARISON_MISMATCH",
                    alignment, i, j, text, pattern, equal,
                    "KMP reutiliza la tabla LPS para evitar retroceder en el texto."));
            if (equal) {
                i++;
                j++;
                if (j == pattern.length()) {
                    int match = i - pattern.length();
                    positions.add(match);
                    int priorAlignment = i - pattern.length();
                    int fallback = prefix.get(j - 1);
                    steps.add(event(step++, "MATCH", match, null, null, null, null,
                            "Coincidencia completa. Para permitir solapamientos, j vuelve a LPS[m-1] = " + fallback + "."));
                    j = fallback;
                    int nextAlignment = i - j;
                    steps.add(event(step++, "FALLBACK", priorAlignment, nextAlignment - priorAlignment,
                            null, null, null,
                            "El índice i permanece en " + i + " y el patrón se realinea usando la tabla LPS."));
                }
            } else if (j > 0) {
                int previousJ = j;
                j = prefix.get(j - 1);
                steps.add(event(step++, "FALLBACK", alignment, previousJ - j, null, null, null,
                        "j cambia de " + previousJ + " a LPS[" + (previousJ - 1) + "] = " + j
                                + "; i no retrocede."));
            } else {
                i++;
                steps.add(event(step++, "SHIFT", alignment, 1, null, null, null,
                        "LPS no permite retroceso (j = 0); se avanza un carácter del texto."));
            }
        }
        return result(positions, prefix, null, null, 0, 0, 0, steps);
    }

    private static Resultado boyerMoore(String text, String pattern) {
        List<Integer> positions = new ArrayList<>();
        List<SearchTraceStep> steps = new ArrayList<>();
        if (pattern.isEmpty() || pattern.length() > text.length()) {
            return noWindow(positions,
                    "El patrón está vacío o es más largo que el texto; Boyer-Moore retorna sin preprocesar.");
        }
        int[] good = BoyerMoore.tablaSufijoBueno(pattern);
        Map<Character, Integer> lastOccurrences = new LinkedHashMap<>();
        for (int i = 0; i < pattern.length(); i++) {
            lastOccurrences.put(pattern.charAt(i), i);
        }
        List<BadCharacterEntry> badTable = lastOccurrences.entrySet().stream()
                .map(entry -> new BadCharacterEntry(String.valueOf(entry.getKey()), entry.getValue()))
                .toList();
        List<Integer> goodTable = java.util.Arrays.stream(good).boxed().toList();
        int step = 1;
        int n = text.length();
        int m = pattern.length();
        int alignment = 0;
        while (alignment <= n - m && m > 0) {
            int j = m - 1;
            while (j >= 0) {
                boolean equal = pattern.charAt(j) == text.charAt(alignment + j);
                steps.add(comparison(step++, equal ? "COMPARISON_MATCH" : "COMPARISON_MISMATCH",
                        alignment, alignment + j, j, text, pattern, equal,
                        "Boyer-Moore compara el patrón de derecha a izquierda."));
                if (!equal) {
                    break;
                }
                j--;
            }
            if (j < 0) {
                positions.add(alignment);
                int shift = good[0];
                steps.add(event(step++, "MATCH", alignment, shift, null, null, null,
                        "Coincidencia completa; el desplazamiento de sufijo bueno mantiene la búsqueda de solapamientos."));
                alignment += shift;
            } else {
                char badCharacter = text.charAt(alignment + j);
                Integer previous = lastOccurrences.get(badCharacter);
                int lastIndex;
                if (badCharacter < 256) {
                    lastIndex = previous == null ? -1 : previous;
                } else {
                    lastIndex = -1;
                    for (int k = j - 1; k >= 0; k--) {
                        if (pattern.charAt(k) == badCharacter) {
                            lastIndex = k;
                            break;
                        }
                    }
                }
                int badShift = j - lastIndex;
                int goodShift = good[j + 1];
                int shift = Math.max(badShift, goodShift);
                steps.add(new SearchTraceStep(step++, "SHIFT", alignment, alignment + j, j,
                        String.valueOf(badCharacter), String.valueOf(pattern.charAt(j)), false,
                        badShift, goodShift, shift, null, null, null,
                        "Regla de carácter malo: " + badShift + "; regla de sufijo bueno: " + goodShift
                                + ". Se aplica max(" + badShift + ", " + goodShift + ") = " + shift + "."));
                alignment += shift;
            }
        }
        return result(positions, null, badTable, goodTable, 0, 0, 0, steps);
    }

    private static Resultado rabinKarp(String text, String pattern) {
        List<Integer> positions = new ArrayList<>();
        List<SearchTraceStep> steps = new ArrayList<>();
        int n = text.length();
        int m = pattern.length();
        if (m == 0 || m > n) {
            return noWindow(positions, "El patrón está vacío o es más largo que el texto; Rabin-Karp retorna sin hashes.");
        }
        long highestPower = 1;
        for (int i = 0; i < m - 1; i++) {
            highestPower = (highestPower * HASH_BASE) % HASH_MODULUS;
        }
        long patternHash = 0;
        long windowHash = 0;
        for (int i = 0; i < m && i < n; i++) {
            patternHash = (patternHash * HASH_BASE + pattern.charAt(i)) % HASH_MODULUS;
            windowHash = (windowHash * HASH_BASE + text.charAt(i)) % HASH_MODULUS;
        }
        int step = 1;
        for (int alignment = 0; alignment <= n - m && m > 0; alignment++) {
            long currentHash = windowHash;
            steps.add(new SearchTraceStep(step++, "WINDOW_HASH", alignment, alignment, null,
                    null, null, null, null, null, null, patternHash, currentHash, null,
                    "Se compara hashPatrón = " + patternHash + " con hashVentana = " + currentHash + "."));
            if (patternHash == currentHash) {
                int j = 0;
                while (j < m) {
                    boolean equal = text.charAt(alignment + j) == pattern.charAt(j);
                    steps.add(comparison(step++, equal ? "VERIFICATION_MATCH" : "VERIFICATION_MISMATCH",
                            alignment, alignment + j, j, text, pattern, equal,
                            "Los hashes coinciden; se verifica carácter a carácter para descartar colisiones."));
                    if (!equal) {
                        break;
                    }
                    j++;
                }
                if (j == m) {
                    positions.add(alignment);
                    steps.add(event(step++, "MATCH", alignment, 1, patternHash, currentHash, null,
                            "La verificación exacta confirma la coincidencia."));
                } else {
                    steps.add(event(step++, "HASH_COLLISION", alignment, 1, patternHash, currentHash, null,
                            "Los hashes coincidieron, pero la verificación encontró caracteres distintos."));
                }
            }
            if (alignment < n - m) {
                int outgoing = text.charAt(alignment);
                int incoming = text.charAt(alignment + m);
                long nextHash = (HASH_BASE * (windowHash - (long) outgoing * highestPower) + incoming)
                        % HASH_MODULUS;
                if (nextHash < 0) {
                    nextHash += HASH_MODULUS;
                }
                steps.add(new SearchTraceStep(step++, "ROLLING_HASH", alignment, alignment, null,
                        String.valueOf(text.charAt(alignment)), String.valueOf(text.charAt(alignment + m)),
                        null, null, null, 1, patternHash, windowHash, nextHash,
                        "hashNuevo = (BASE × (hashAnterior − carácterSaliente × BASE^(m−1)) + carácterEntrante) mod MOD."));
                windowHash = nextHash;
            }
        }
        return result(positions, null, null, null, HASH_BASE, HASH_MODULUS, highestPower, patternHash, steps);
    }

    private static Resultado result(
            List<Integer> positions,
            List<Integer> prefixTable,
            List<BadCharacterEntry> badTable,
            List<Integer> goodTable,
            int hashBase,
            long modulus,
            long highestPower,
            List<SearchTraceStep> steps) {
        return result(positions, prefixTable, badTable, goodTable, hashBase, modulus, highestPower, 0, steps);
    }

    private static Resultado noWindow(List<Integer> positions, String explanation) {
        return result(positions, null, null, null, 0, 0, 0,
                List.of(new SearchTraceStep(1, "NO_SEARCH", null, null, null, null, null, null,
                        null, null, null, null, null, null, explanation)));
    }

    private static Resultado result(
            List<Integer> positions,
            List<Integer> prefixTable,
            List<BadCharacterEntry> badTable,
            List<Integer> goodTable,
            int hashBase,
            long modulus,
            long highestPower,
            long patternHash,
            List<SearchTraceStep> steps) {
        return new Resultado(List.copyOf(positions), new SearchAlgorithmTrace(
                prefixTable, badTable, goodTable, hashBase, modulus, highestPower, patternHash, List.copyOf(steps)));
    }

    private static SearchTraceStep comparison(
            int step,
            String action,
            int alignment,
            int textIndex,
            int patternIndex,
            String text,
            String pattern,
            boolean equal,
            String explanation) {
        return new SearchTraceStep(step, action, alignment, textIndex, patternIndex,
                String.valueOf(text.charAt(textIndex)), String.valueOf(pattern.charAt(patternIndex)),
                equal, null, null, null, null, null, null, explanation);
    }

    private static SearchTraceStep event(
            int step,
            String action,
            int alignment,
            Integer shift,
            Long patternHash,
            Long windowHash,
            Long nextWindowHash,
            String explanation) {
        return new SearchTraceStep(step, action, alignment, null, null, null, null, null,
                null, null, shift, patternHash, windowHash, nextWindowHash, explanation);
    }
}
