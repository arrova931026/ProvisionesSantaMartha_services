package com.shsm.api.config;

import com.shsm.api.entity.catalog.TipoDocumento;
import com.shsm.api.repository.TipoDocumentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Garantiza que todos los tipos de documento requeridos existen en la base de datos
 * al arrancar la aplicación. Esto resuelve el caso en que el volumen Docker ya estaba
 * inicializado con una versión anterior del seed que no contenía ciertos tipos.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogDataInitializer implements ApplicationRunner {

    private final TipoDocumentoRepository tipoDocumentoRepository;

    private static final List<String[]> TIPOS_DOCUMENTO = List.of(
            new String[]{"INE",             "Credencial INE / IFE"},
            new String[]{"CURP",            "CURP"},
            new String[]{"ACTA_NAC",        "Acta de nacimiento"},
            new String[]{"CONTRATO_PDF",    "Contrato firmado (PDF)"},
            new String[]{"COMPROBANTE_DOM", "Comprobante de domicilio"},
            new String[]{"RFC",             "RFC / Constancia de situación fiscal"},
            new String[]{"FOTO_PERFIL",     "Fotografía de perfil"},
            new String[]{"ACTA_DEF",        "Acta de defunción"},
            new String[]{"OTRO",            "Otro documento"}
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int insertados = 0;
        for (String[] tipo : TIPOS_DOCUMENTO) {
            if (tipoDocumentoRepository.findByClave(tipo[0]).isEmpty()) {
                TipoDocumento t = new TipoDocumento();
                t.setClave(tipo[0]);
                t.setNombre(tipo[1]);
                tipoDocumentoRepository.save(t);
                insertados++;
                log.info("CatalogDataInitializer: tipo de documento '{}' insertado.", tipo[0]);
            }
        }
        if (insertados > 0) {
            log.info("CatalogDataInitializer: {} tipo(s) de documento sincronizado(s).", insertados);
        }
    }
}
