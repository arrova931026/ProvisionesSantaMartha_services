package com.shsm.api.repository;

import com.shsm.api.entity.catalog.EstadoContrato;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EstadoContratoRepository extends JpaRepository<EstadoContrato, Long> {
    Optional<EstadoContrato> findByClave(String clave);
    List<EstadoContrato> findByActivoTrue();
}
