package com.shsm.api.repository;

import com.shsm.api.entity.Pago;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByContratoIdOrderByFechaPagoDesc(Long contratoId);

    Page<Pago> findByContratoId(Long contratoId, Pageable pageable);

    boolean existsByReferenciaExterna(String referenciaExterna);

    @org.springframework.data.jpa.repository.Query(
        "SELECT p.cobro.id, p.fechaPago FROM Pago p " +
        "WHERE p.cobro.id IN :ids " +
        "AND p.fechaPago = (SELECT MAX(p2.fechaPago) FROM Pago p2 WHERE p2.cobro.id = p.cobro.id)")
    java.util.List<Object[]> findLatestFechasPagoByCobros(
        @org.springframework.data.repository.query.Param("ids") java.util.List<Long> ids);

    @Query("""
            SELECT SUM(p.montoPagado) FROM Pago p
            WHERE p.contrato.id = :contratoId
            """)
    BigDecimal sumMontoPagadoByContratoId(@Param("contratoId") Long contratoId);

    @Query("""
            SELECT p FROM Pago p
            WHERE p.fechaPago BETWEEN :inicio AND :fin
            ORDER BY p.fechaPago DESC
            """)
    List<Pago> findByRangoFechas(@Param("inicio") OffsetDateTime inicio,
                                  @Param("fin") OffsetDateTime fin);
}
