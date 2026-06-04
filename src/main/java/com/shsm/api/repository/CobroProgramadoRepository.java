package com.shsm.api.repository;

import com.shsm.api.entity.CobroProgramado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CobroProgramadoRepository extends JpaRepository<CobroProgramado, Long> {

    List<CobroProgramado> findByContratoIdOrderByNumeroMensualidad(Long contratoId);

    @Query("""
            SELECT cp FROM CobroProgramado cp
            WHERE cp.contrato.id = :contratoId
              AND cp.estado.clave IN ('PENDIENTE', 'VENCIDO')
            ORDER BY cp.fechaProgramada ASC
            """)
    List<CobroProgramado> findPendientesByContrato(@Param("contratoId") Long contratoId);

    @Query("""
            SELECT cp FROM CobroProgramado cp
            WHERE cp.estado.clave = 'PENDIENTE'
              AND cp.fechaProgramada <= :fecha
            """)
    List<CobroProgramado> findPendientesHastaFecha(@Param("fecha") LocalDate fecha);
}
