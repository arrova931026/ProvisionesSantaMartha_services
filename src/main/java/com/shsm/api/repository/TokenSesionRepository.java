package com.shsm.api.repository;

import com.shsm.api.entity.TokenSesion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface TokenSesionRepository extends JpaRepository<TokenSesion, Long> {

    Optional<TokenSesion> findByToken(String token);

    @Modifying
    @Query("DELETE FROM TokenSesion t WHERE t.expiraEn < :now OR t.usado = true")
    void eliminarExpirados(@Param("now") OffsetDateTime now);
}
