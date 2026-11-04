package com.proyecto.bibliometria.articulos;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticuloRepository extends JpaRepository<Articulo, Integer> {

    List<Articulo> findAllByOrderByIdAsc();
}
