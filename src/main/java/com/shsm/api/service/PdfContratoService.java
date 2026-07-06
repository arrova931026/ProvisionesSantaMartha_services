package com.shsm.api.service;

public interface PdfContratoService {
    /** Genera el PDF del contrato y devuelve los bytes. */
    byte[] generarPdf(Long contratoId);

    /** Genera el PDF y lo envía por correo al titular. */
    void enviarPorCorreo(Long contratoId);
}
