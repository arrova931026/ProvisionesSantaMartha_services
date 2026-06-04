package com.shsm.api.controller;

import com.shsm.api.entity.catalog.*;
import com.shsm.api.entity.Sucursal;
import com.shsm.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/catalogos")
@RequiredArgsConstructor
public class CatalogoController {

    private final RoleRepository roleRepository;
    private final ParentescoRepository parentescoRepository;
    private final EstadoContratoRepository estadoContratoRepository;
    private final EstadoPagoRepository estadoPagoRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final CategoriaArticuloRepository categoriaArticuloRepository;
    private final SucursalRepository sucursalRepository;

    @GetMapping("/roles")
    public ResponseEntity<List<Role>> roles() {
        return ResponseEntity.ok(roleRepository.findAll());
    }

    @GetMapping("/parentescos")
    public ResponseEntity<List<Parentesco>> parentescos() {
        return ResponseEntity.ok(parentescoRepository.findByActivoTrue());
    }

    @GetMapping("/estados-contrato")
    public ResponseEntity<List<EstadoContrato>> estadosContrato() {
        return ResponseEntity.ok(estadoContratoRepository.findByActivoTrue());
    }

    @GetMapping("/estados-pago")
    public ResponseEntity<List<EstadoPago>> estadosPago() {
        return ResponseEntity.ok(estadoPagoRepository.findByActivoTrue());
    }

    @GetMapping("/metodos-pago")
    public ResponseEntity<List<MetodoPago>> metodosPago() {
        return ResponseEntity.ok(metodoPagoRepository.findByActivoTrue());
    }

    @GetMapping("/tipos-documento")
    public ResponseEntity<List<TipoDocumento>> tiposDocumento() {
        return ResponseEntity.ok(tipoDocumentoRepository.findByActivoTrue());
    }

    @GetMapping("/categorias-articulo")
    public ResponseEntity<List<CategoriaArticulo>> categoriasArticulo() {
        return ResponseEntity.ok(categoriaArticuloRepository.findByActivoTrue());
    }

    @GetMapping("/sucursales")
    public ResponseEntity<List<Sucursal>> sucursales() {
        return ResponseEntity.ok(sucursalRepository.findByActivoTrue());
    }
}
