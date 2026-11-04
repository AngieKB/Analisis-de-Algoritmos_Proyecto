package com.proyecto.bibliometria.api;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.proyecto.bibliometria.api.dto.ApiDtos.AlgorithmsResponse;
import com.proyecto.bibliometria.api.dto.ApiDtos.ClusterResponse;
import com.proyecto.bibliometria.api.dto.ApiDtos.Linkage;
import com.proyecto.bibliometria.api.dto.ApiDtos.SearchRequest;
import com.proyecto.bibliometria.api.dto.ApiDtos.SearchResponse;
import com.proyecto.bibliometria.api.dto.ApiDtos.SimilarityRequest;
import com.proyecto.bibliometria.api.dto.ApiDtos.SimilarityResponse;
import com.proyecto.bibliometria.analisis.AnalisisService;

@RestController
@RequestMapping("/api/v1")
public class AnalisisController {

    private final AnalisisService service;

    public AnalisisController(AnalisisService service) {
        this.service = service;
    }

    @GetMapping("/algorithms")
    public AlgorithmsResponse algoritmos() {
        return service.algoritmosDisponibles();
    }

    @PostMapping("/similarities")
    public SimilarityResponse comparar(@Valid @RequestBody SimilarityRequest request) {
        return service.comparar(request);
    }

    @PostMapping("/search")
    public SearchResponse buscar(@Valid @RequestBody SearchRequest request) {
        return service.buscar(request);
    }

    @GetMapping("/clusters")
    public ClusterResponse agrupar(@RequestParam Linkage linkage) {
        return service.agrupar(linkage);
    }
}
