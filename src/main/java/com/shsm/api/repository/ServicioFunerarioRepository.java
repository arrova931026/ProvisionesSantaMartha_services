package com.shsm.api.repository;

import com.shsm.api.entity.ServicioFunerario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ServicioFunerarioRepository extends JpaRepository<ServicioFunerario, Long> {
    List<ServicioFunerario> findByContratoIdAndActivoTrue(Long contratoId);

    /** Servicios programados pero aún no realizados (fecha_servicio futura o nula). */
    @Query("""
            SELECT COUNT(s) FROM ServicioFunerario s
            WHERE s.activo = true
              AND (s.fechaServicio IS NULL OR s.fechaServicio >= CURRENT_DATE)
            """)
    long countEnProceso();

    /** Servicios cuyo fallecimiento ocurrió en el mes/año indicado. */
    @Query("""
            SELECT COUNT(s) FROM ServicioFunerario s
            WHERE s.activo = true
              AND EXTRACT(YEAR FROM s.fechaFallecimiento) = :anio
              AND EXTRACT(MONTH FROM s.fechaFallecimiento) = :mes
            """)
    long countByMes(@Param("anio") int anio, @Param("mes") int mes);
}
