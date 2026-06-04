package com.shsm.api.repository;

import com.shsm.api.entity.CatalogoArticulo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CatalogoArticuloRepository extends JpaRepository<CatalogoArticulo, Long> {
    List<CatalogoArticulo> findByCategoriaIdAndActivoTrue(Long categoriaId);
    List<CatalogoArticulo> findByActivoTrue();
}
