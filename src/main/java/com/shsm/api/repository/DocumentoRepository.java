package com.shsm.api.repository;

import com.shsm.api.entity.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentoRepository extends JpaRepository<Documento, Long> {
    List<Documento> findByContratoIdAndActivoTrue(Long contratoId);
    List<Documento> findByPersonaIdAndActivoTrue(Long personaId);
}
