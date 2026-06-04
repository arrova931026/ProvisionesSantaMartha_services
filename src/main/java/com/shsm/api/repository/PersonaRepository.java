package com.shsm.api.repository;

import com.shsm.api.entity.Persona;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PersonaRepository extends JpaRepository<Persona, Long> {

    Optional<Persona> findByCorreo(String correo);

    Optional<Persona> findByCurp(String curp);

    Optional<Persona> findByRfc(String rfc);

    @Query("""
            SELECT p FROM Persona p
            WHERE p.deletedAt IS NULL
              AND p.activo = true
              AND (
                LOWER(p.nombre)    LIKE LOWER(CONCAT('%', :q, '%')) OR
                LOWER(p.apPaterno) LIKE LOWER(CONCAT('%', :q, '%')) OR
                LOWER(p.apMaterno) LIKE LOWER(CONCAT('%', :q, '%')) OR
                p.correo           LIKE LOWER(CONCAT('%', :q, '%')) OR
                p.curp             LIKE UPPER(CONCAT('%', :q, '%'))
              )
            """)
    Page<Persona> buscar(@Param("q") String q, Pageable pageable);
}
