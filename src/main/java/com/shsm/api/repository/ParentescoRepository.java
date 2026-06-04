package com.shsm.api.repository;

import com.shsm.api.entity.catalog.Parentesco;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ParentescoRepository extends JpaRepository<Parentesco, Long> {
    Optional<Parentesco> findByClave(String clave);
    List<Parentesco> findByActivoTrue();
}
