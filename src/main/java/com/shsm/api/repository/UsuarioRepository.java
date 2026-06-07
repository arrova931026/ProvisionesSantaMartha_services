package com.shsm.api.repository;

import com.shsm.api.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);

    /**
     * Retorna únicamente socios con rol CLIENTE activos (no dados de baja),
     * con búsqueda opcional por username, nombre completo o correo.
     *
     * @param q        texto de búsqueda; puede ser null o vacío para traer todos
     * @param pageable paginación y ordenamiento
     */
    @Query("""
            SELECT u FROM Usuario u
            JOIN FETCH u.persona p
            JOIN FETCH u.rol r
            WHERE u.activo = true
              AND u.deletedAt IS NULL
              AND r.clave = 'CLIENTE'
              AND (
                :q IS NULL OR :q = ''
                OR LOWER(u.username)    LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(p.nombre)      LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(p.apPaterno)   LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(p.correo)      LIKE LOWER(CONCAT('%', :q, '%'))
              )
            """)
    Page<Usuario> buscarActivos(@Param("q") String q, Pageable pageable);
}
