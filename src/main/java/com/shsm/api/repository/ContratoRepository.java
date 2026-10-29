package com.shsm.api.repository;

import com.shsm.api.entity.Contrato;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContratoRepository extends JpaRepository<Contrato, Long> {

    Optional<Contrato> findByNumeroContrato(String numeroContrato);

    List<Contrato> findByPersonaIdAndActivoTrue(Long personaId);

    boolean existsByIdAndPersonaId(Long id, Long personaId);

    @Query("""
            SELECT c FROM Contrato c
            WHERE c.deletedAt IS NULL
              AND c.activo = true
              AND c.estado.clave = :estadoClave
            """)
    Page<Contrato> findByEstadoClave(@Param("estadoClave") String estadoClave, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Contrato c WHERE c.estado.clave = :clave AND c.activo = true")
    long countByEstadoClave(@Param("clave") String clave);

    /** Contratos vigentes cuyo vencimiento cae dentro del rango dado (renovaciones próximas). */
    @Query("""
            SELECT COUNT(c) FROM Contrato c
            WHERE c.activo = true
              AND c.deletedAt IS NULL
              AND c.fechaVencimiento BETWEEN :desde AND :hasta
            """)
    long countRenovacionesProximas(@Param("desde") java.time.LocalDate desde,
                                   @Param("hasta") java.time.LocalDate hasta);

    /** Contratos vigentes con al menos un cobro vencido (morosos). */
    @Query("""
            SELECT COUNT(DISTINCT c) FROM Contrato c
            JOIN CobroProgramado cp ON cp.contrato = c
            WHERE c.activo = true
              AND c.deletedAt IS NULL
              AND c.estado.clave = 'VIGENTE'
              AND cp.estado.clave IN ('PENDIENTE', 'VENCIDO')
              AND cp.fechaProgramada < :hoy
            """)
    long countMorosos(@Param("hoy") java.time.LocalDate hoy);

    /** Contratos vigentes de una persona (para el modal de eliminación), con el plan cargado. */
    @Query("""
            SELECT c FROM Contrato c
            JOIN FETCH c.plan
            WHERE c.persona.id = :personaId
              AND c.activo = true
              AND c.deletedAt IS NULL
              AND c.estado.clave = 'VIGENTE'
            """)
    java.util.List<Contrato> findVigentesByPersonaId(@Param("personaId") Long personaId);
}
