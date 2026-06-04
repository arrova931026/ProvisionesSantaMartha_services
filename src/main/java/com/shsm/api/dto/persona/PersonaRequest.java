package com.shsm.api.dto.persona;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record PersonaRequest(

        @NotBlank(message = "El nombre es requerido")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        @NotBlank(message = "El apellido paterno es requerido")
        @Size(max = 100, message = "El apellido paterno no puede superar 100 caracteres")
        String apPaterno,

        @Size(max = 100, message = "El apellido materno no puede superar 100 caracteres")
        String apMaterno,

        LocalDate fechaNacimiento,

        @Pattern(regexp = "[MFO]", message = "El sexo debe ser M, F u O")
        String sexo,

        @Pattern(regexp = "^[A-Z]{4}\\d{6}[HM][A-Z]{5}[A-Z0-9]\\d$",
                 message = "CURP inválida")
        String curp,

        @Pattern(regexp = "^[A-ZÑ&]{3,4}\\d{6}[A-Z0-9]{3}$",
                 message = "RFC inválido")
        String rfc,

        @Size(max = 20, message = "El teléfono no puede superar 20 caracteres")
        String telefono,

        @Size(max = 20, message = "El teléfono alternativo no puede superar 20 caracteres")
        String telefonoAlt,

        @Email(message = "El correo no tiene formato válido")
        @Size(max = 254, message = "El correo no puede superar 254 caracteres")
        String correo,

        String calle,
        String numeroExt,
        String numeroInt,
        String colonia,
        String municipio,
        String estado,

        @Size(max = 5, message = "El código postal debe tener 5 dígitos")
        String codigoPostal,

        String pais
) {}
