package com.shsm.api.repository;

import com.shsm.api.entity.Auditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
    Page<Auditoria> findByTablaAndRegistroId(String tabla, Long registroId, Pageable pageable);
    Page<Auditoria> findByUsuarioId(Long usuarioId, Pageable pageable);
}
