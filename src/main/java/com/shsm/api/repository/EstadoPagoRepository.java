package com.shsm.api.repository;

import com.shsm.api.entity.catalog.EstadoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EstadoPagoRepository extends JpaRepository<EstadoPago, Long> {
    Optional<EstadoPago> findByClave(String clave);
    List<EstadoPago> findByActivoTrue();
}
