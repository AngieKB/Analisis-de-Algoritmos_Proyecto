package com.proyecto.bibliometria.articulos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "articles")
public class Articulo {

    @Id
    private Integer id;

    @Column(nullable = false, length = 500)
    private String titulo;

    @Column(name = "abstract_text", nullable = false, columnDefinition = "text")
    private String abstractText;

    protected Articulo() {
    }

    public Articulo(Integer id, String titulo, String abstractText) {
        this.id = id;
        this.titulo = titulo;
        this.abstractText = abstractText;
    }

    public Integer getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getAbstractText() {
        return abstractText;
    }
}
