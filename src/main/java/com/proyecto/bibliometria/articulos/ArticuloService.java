package com.proyecto.bibliometria.articulos;

import java.util.List;

import org.springframework.stereotype.Service;

import com.proyecto.bibliometria.api.dto.ApiDtos.ArticleResponse;

@Service
public class ArticuloService {

    private final ArticuloRepository repository;

    public ArticuloService(ArticuloRepository repository) {
        this.repository = repository;
    }

    public List<Articulo> obtenerTodos() {
        return repository.findAllByOrderByIdAsc();
    }

    public Articulo obtener(int id) {
        return repository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("No existe el artículo con ID " + id));
    }

    public static ArticleResponse toResponse(Articulo articulo) {
        return new ArticleResponse(articulo.getId(), articulo.getTitulo(), articulo.getAbstractText());
    }
}
