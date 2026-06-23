package com.shsm.api.service;

public interface EmailService {
    void enviarRecuperacionContrasena(String destinatario, String nombreCompleto, String enlace);
}
