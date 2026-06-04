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

    @Query("""
            SELECT c FROM Contrato c
            WHERE c.deletedAt IS NULL
              AND c.activo = true
              AND c.estado.clave = :estadoClave
            """)
    Page<Contrato> findByEstadoClave(@Param("estadoClave") String estadoClave, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Contrato c WHERE c.estado.clave = :clave AND c.activo = true")
    long countByEstadoClave(@Param("clave") String clave);
}
