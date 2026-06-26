package com.shsm.api.controller;

import com.shsm.api.service.GeminiOcrService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ocr")
@RequiredArgsConstructor
public class OcrController {

    private final GeminiOcrService geminiOcrService;

    /**
     * Recibe hasta 3 imágenes (INE frente, INE reverso, Acta Fiscal),
     * las envía a Gemini y devuelve los campos extraídos como JSON.
     */
    @PostMapping("/ine")
    public ResponseEntity<Map<String, String>> procesarDocumentos(
            @RequestParam(required = false) MultipartFile ineFrente,
            @RequestParam(required = false) MultipartFile ineReverso,
            @RequestParam(required = false) MultipartFile acta) {
        try {
            List<MultipartFile> imagenes = new ArrayList<>();
            if (ineFrente  != null && !ineFrente.isEmpty())  imagenes.add(ineFrente);
            if (ineReverso != null && !ineReverso.isEmpty()) imagenes.add(ineReverso);
            if (acta       != null && !acta.isEmpty())       imagenes.add(acta);

            if (imagenes.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Se requiere al menos una imagen."));
            }

            Map<String, String> datos = geminiOcrService.extraerDatos(imagenes);
            return ResponseEntity.ok(datos);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno."));
        }
    }
}
