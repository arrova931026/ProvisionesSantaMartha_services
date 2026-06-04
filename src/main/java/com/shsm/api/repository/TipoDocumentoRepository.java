package com.shsm.api.repository;

import com.shsm.api.entity.catalog.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TipoDocumentoRepository extends JpaRepository<TipoDocumento, Long> {
    List<TipoDocumento> findByActivoTrue();
}
