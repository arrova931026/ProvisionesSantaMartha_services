package com.shsm.api.service;

public interface EmailService {
    void enviarRecuperacionContrasena(String destinatario, String nombreCompleto, String enlace);
    void enviarContrato(String destinatario, String nombreCompleto, String numeroContrato, byte[] pdfBytes);
}
