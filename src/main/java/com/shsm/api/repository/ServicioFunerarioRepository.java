package com.shsm.api.repository;

import com.shsm.api.entity.ServicioFunerario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ServicioFunerarioRepository extends JpaRepository<ServicioFunerario, Long> {
    List<ServicioFunerario> findByContratoIdAndActivoTrue(Long contratoId);
}
