package com.shsm.api.repository;

import com.shsm.api.entity.catalog.CategoriaArticulo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CategoriaArticuloRepository extends JpaRepository<CategoriaArticulo, Long> {
    List<CategoriaArticulo> findByActivoTrue();
}
