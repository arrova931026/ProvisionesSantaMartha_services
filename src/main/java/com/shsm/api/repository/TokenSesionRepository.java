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

    /** Marca como usados todos los tokens REFRESH activos del usuario (p. ej. tras restablecer contraseña). */
    @Modifying
    @Query("UPDATE TokenSesion t SET t.usado = true WHERE t.usuario.id = :usuarioId AND t.tipo = 'REFRESH' AND t.usado = false")
    void revocarRefreshesDeUsuario(@Param("usuarioId") Long usuarioId);
}
