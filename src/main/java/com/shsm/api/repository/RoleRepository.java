package com.shsm.api.repository;

import com.shsm.api.entity.catalog.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByClave(String clave);
}
