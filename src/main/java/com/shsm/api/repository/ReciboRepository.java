package com.shsm.api.repository;

import com.shsm.api.entity.Recibo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ReciboRepository extends JpaRepository<Recibo, Long> {
    Optional<Recibo> findByFolioRecibo(String folioRecibo);
    Optional<Recibo> findByPagoId(Long pagoId);
}
