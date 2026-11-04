package com.proyecto.bibliometria.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.proyecto.bibliometria.api.dto.ApiDtos.ArticleResponse;
import com.proyecto.bibliometria.articulos.ArticuloService;

@RestController
@RequestMapping("/api/v1/articles")
public class ArticuloController {

    private final ArticuloService service;

    public ArticuloController(ArticuloService service) {
        this.service = service;
    }

    @GetMapping
    public List<ArticleResponse> listar() {
        return service.obtenerTodos().stream().map(ArticuloService::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ArticleResponse obtener(@PathVariable int id) {
        return ArticuloService.toResponse(service.obtener(id));
    }
}
