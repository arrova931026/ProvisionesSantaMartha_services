package com.shsm.api.repository;

import com.shsm.api.entity.Beneficiario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BeneficiarioRepository extends JpaRepository<Beneficiario, Long> {
    List<Beneficiario> findByContratoIdAndActivoTrue(Long contratoId);
    long countByContratoIdAndActivoTrue(Long contratoId);
}
