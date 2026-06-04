package com.shsm.api.repository;

import com.shsm.api.entity.PlanFunerario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlanFunerarioRepository extends JpaRepository<PlanFunerario, Long> {
    List<PlanFunerario> findByActivoTrue();
}
