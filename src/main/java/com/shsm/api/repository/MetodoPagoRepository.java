package com.shsm.api.repository;

import com.shsm.api.entity.catalog.MetodoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MetodoPagoRepository extends JpaRepository<MetodoPago, Long> {
    List<MetodoPago> findByActivoTrue();
    Optional<MetodoPago> findByClave(String clave);
}
