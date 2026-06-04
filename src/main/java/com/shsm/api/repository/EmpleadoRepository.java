package com.shsm.api.repository;

import com.shsm.api.entity.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {
    Optional<Empleado> findByNumEmpleado(String numEmpleado);
    List<Empleado> findBySucursalIdAndActivoTrue(Long sucursalId);
    List<Empleado> findByActivoTrue();
}
