package com.shsm.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
@Slf4j
public class GeminiOcrService {

    @Value("${gemini.api-key}")
    private String apiKey;

    private static final String GEMINI_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";

    private static final String PROMPT =
        "Analiza esta imagen de un documento de identidad mexicano (INE/IFE) o Constancia de " +
        "Situación Fiscal del SAT. Extrae los campos indicados y devuelve ÚNICAMENTE un JSON " +
        "válido con estas claves (deja la cadena vacía si el campo no aparece o no es legible):\n" +
        "{\n" +
        "  \"nombre\": \"\",\n" +
        "  \"apPaterno\": \"\",\n" +
        "  \"apMaterno\": \"\",\n" +
        "  \"curp\": \"\",\n" +
        "  \"rfc\": \"\",\n" +
        "  \"fechaNacimiento\": \"\",\n" +
        "  \"sexo\": \"\",\n" +
        "  \"calle\": \"\",\n" +
        "  \"numeroExt\": \"\",\n" +
        "  \"colonia\": \"\",\n" +
        "  \"municipio\": \"\",\n" +
        "  \"estado\": \"\",\n" +
        "  \"codigoPostal\": \"\"\n" +
        "}\n" +
        "Reglas:\n" +
        "- nombre: solo el(los) nombre(s), sin apellidos.\n" +
        "- curp: 18 caracteres en MAYÚSCULAS.\n" +
        "- rfc: con homoclave en MAYÚSCULAS.\n" +
        "- fechaNacimiento: formato YYYY-MM-DD.\n" +
        "- sexo: 'M' si masculino, 'F' si femenino.\n" +
        "- codigoPostal: exactamente 5 dígitos.\n" +
        "Responde SOLO con el JSON, sin código, sin explicaciones ni markdown.";

    /**
     * Deshabilita todos los filtros de seguridad para que Gemini no bloquee
     * documentos de identidad (INE, RFC) por contener datos personales.
     */
    private static final List<Map<String, String>> SAFETY_SETTINGS = List.of(
        Map.of("category", "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold", "BLOCK_NONE"),
        Map.of("category", "HARM_CATEGORY_HARASSMENT",        "threshold", "BLOCK_NONE"),
        Map.of("category", "HARM_CATEGORY_HATE_SPEECH",       "threshold", "BLOCK_NONE"),
        Map.of("category", "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold", "BLOCK_NONE"),
        Map.of("category", "HARM_CATEGORY_CIVIC_INTEGRITY",   "threshold", "BLOCK_NONE")
    );

    /** Fuerza respuesta en JSON puro para evitar bloques markdown. */
    private static final Map<String, Object> GENERATION_CONFIG = Map.of(
        "responseMimeType", "application/json"
    );

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Envía cada imagen a Gemini y fusiona los resultados.
     * Lanza excepción si ninguna imagen pudo procesarse.
     */
    public Map<String, String> extraerDatos(List<MultipartFile> imagenes) throws Exception {
        Map<String, String> resultado = new LinkedHashMap<>();
        int procesadas = 0;
        int exitosas   = 0;
        for (MultipartFile imagen : imagenes) {
            if (imagen == null || imagen.isEmpty()) continue;
            procesadas++;
            try {
                Map<String, String> parcial = llamarGemini(imagen);
                parcial.forEach((k, v) -> {
                    if (v != null && !v.isBlank() && !resultado.containsKey(k))
                        resultado.put(k, v);
                });
                exitosas++;
            } catch (Exception e) {
                log.warn("Gemini OCR falló para imagen '{}': {}", imagen.getOriginalFilename(), e.getMessage());
            }
        }
        if (procesadas > 0 && exitosas == 0) {
            throw new RuntimeException("Gemini no pudo procesar ninguna imagen. Verifica la calidad de la foto.");
        }
        return resultado;
    }

    private Map<String, String> llamarGemini(MultipartFile imagen) throws Exception {
        String base64   = Base64.getEncoder().encodeToString(imagen.getBytes());
        String mimeType = imagen.getContentType() != null ? imagen.getContentType() : "image/jpeg";

        Map<String, Object> inlineData = Map.of("mime_type", mimeType, "data", base64);
        Map<String, Object> imagePart  = Map.of("inline_data", inlineData);
        Map<String, Object> textPart   = Map.of("text", PROMPT);
        Map<String, Object> content    = Map.of("parts", List.of(imagePart, textPart));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents",        List.of(content));
        body.put("safetySettings",  SAFETY_SETTINGS);
        body.put("generationConfig", GENERATION_CONFIG);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
            GEMINI_URL + apiKey, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Gemini API respondió con código " + response.getStatusCode());
        }

        JsonNode root = objectMapper.readTree(response.getBody());

        // Bloqueo a nivel de prompt (RECITATION, SAFETY, etc.)
        String blockReason = root.path("promptFeedback").path("blockReason").asText(null);
        if (blockReason != null && !blockReason.isBlank()) {
            log.warn("Gemini bloqueó el prompt con razón: {} | imagen: {}", blockReason, imagen.getOriginalFilename());
            throw new RuntimeException("Gemini bloqueó la solicitud (razón: " + blockReason + ").");
        }

        // Verificar que haya al menos un candidato
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            log.warn("Gemini devolvió candidatos vacíos. Respuesta completa: {}", response.getBody());
            throw new RuntimeException("Gemini no devolvió ningún candidato en la respuesta.");
        }

        JsonNode candidate = candidates.get(0);

        // Verificar finishReason
        String finishReason = candidate.path("finishReason").asText("");
        if ("SAFETY".equals(finishReason) || "OTHER".equals(finishReason) || "RECITATION".equals(finishReason)) {
            log.warn("Gemini finalizó con razón '{}' para imagen '{}'", finishReason, imagen.getOriginalFilename());
            throw new RuntimeException("Gemini no procesó la imagen (finishReason: " + finishReason + ").");
        }

        // Extraer texto de la respuesta
        JsonNode parts = candidate.path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            throw new RuntimeException("Gemini devolvió respuesta sin partes de texto.");
        }
        String jsonText = parts.get(0).path("text").asText("").trim();

        if (jsonText.isBlank()) {
            throw new RuntimeException("Gemini devolvió texto vacío.");
        }

        // Limpiar bloques markdown si Gemini los incluyó de todas formas
        jsonText = jsonText.replaceAll("(?s)```json\\s*", "").replaceAll("```", "").trim();

        // Parsear y filtrar campos vacíos
        JsonNode datos = objectMapper.readTree(jsonText);
        Map<String, String> result = new LinkedHashMap<>();
        datos.fields().forEachRemaining(entry -> {
            String val = entry.getValue().asText("").trim();
            if (!val.isEmpty() && !val.equalsIgnoreCase("null") && !val.equalsIgnoreCase("vacío")) {
                result.put(entry.getKey(), val);
            }
        });
        return result;
    }
}
