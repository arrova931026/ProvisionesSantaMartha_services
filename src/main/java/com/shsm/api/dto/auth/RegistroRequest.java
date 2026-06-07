package com.shsm.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record RegistroRequest(

        @NotBlank(message = "El nombre es requerido")
        @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
        String nombre,

        @NotBlank(message = "El apellido paterno es requerido")
        @Size(min = 2, max = 100, message = "El apellido paterno debe tener entre 2 y 100 caracteres")
        String apPaterno,

        String apMaterno,

        @NotNull(message = "La fecha de nacimiento es requerida")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate fechaNacimiento,

        @NotBlank(message = "El sexo es requerido")
        @Pattern(regexp = "^[MF]$", message = "El sexo debe ser M o F")
        String sexo,

        @NotBlank(message = "El CURP es requerido")
        @Pattern(regexp = "^[A-Z]{4}\\d{6}[HM][A-Z]{5}[A-Z0-9]\\d$",
                 message = "CURP inválido")
        String curp,

        @NotBlank(message = "El teléfono es requerido")
        @Pattern(regexp = "^\\d{10}$", message = "El teléfono debe tener 10 dígitos")
        String telefono,

        @NotBlank(message = "El correo es requerido")
        @Email(message = "Correo electrónico inválido")
        String correo,

        @NotBlank(message = "El nombre de usuario es requerido")
        @Size(min = 4, max = 100, message = "El usuario debe tener entre 4 y 100 caracteres")
        String username,

        @NotBlank(message = "La contraseña es requerida")
        @Size(min = 8, message = "La contraseña debe tener mínimo 8 caracteres")
        String password
) {}
