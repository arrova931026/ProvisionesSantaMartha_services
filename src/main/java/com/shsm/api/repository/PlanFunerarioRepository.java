package com.shsm.api.repository;

import com.shsm.api.entity.PlanFunerario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface PlanFunerarioRepository extends JpaRepository<PlanFunerario, Long> {
    List<PlanFunerario> findByActivoTrue();

    List<PlanFunerario> findByTipoPlan(String tipoPlan);

    List<PlanFunerario> findByTipoPlanAndActivoTrue(String tipoPlan);

    @Query("select distinct p from PlanFunerario p left join fetch p.articulos pa left join fetch pa.articulo a where p.activo = true")
    List<PlanFunerario> findActiveWithArticles();

    @Query("select distinct p from PlanFunerario p left join fetch p.articulos pa left join fetch pa.articulo a where p.id = :id")
    java.util.Optional<PlanFunerario> findByIdWithArticles(Long id);

    @Query("select distinct p from PlanFunerario p left join fetch p.articulos pa left join fetch pa.articulo a where p.tipoPlan = :tipoPlan")
    List<PlanFunerario> findByTipoPlanWithArticles(String tipoPlan);
}
