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

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Envía cada imagen a Gemini y fusiona los resultados.
     * Los campos vacíos de una imagen son llenados por la siguiente si aplica.
     */
    public Map<String, String> extraerDatos(List<MultipartFile> imagenes) throws Exception {
        Map<String, String> resultado = new LinkedHashMap<>();
        for (MultipartFile imagen : imagenes) {
            if (imagen == null || imagen.isEmpty()) continue;
            try {
                Map<String, String> parcial = llamarGemini(imagen);
                parcial.forEach((k, v) -> {
                    if (v != null && !v.isBlank() && !resultado.containsKey(k))
                        resultado.put(k, v);
                });
            } catch (Exception e) {
                log.warn("Gemini OCR falló para imagen '{}': {}", imagen.getOriginalFilename(), e.getMessage());
            }
        }
        return resultado;
    }

    private Map<String, String> llamarGemini(MultipartFile imagen) throws Exception {
        String base64   = Base64.getEncoder().encodeToString(imagen.getBytes());
        String mimeType = imagen.getContentType() != null ? imagen.getContentType() : "image/jpeg";

        // Construir el cuerpo según la API de Gemini
        Map<String, Object> inlineData = Map.of("mime_type", mimeType, "data", base64);
        Map<String, Object> imagePart  = Map.of("inline_data", inlineData);
        Map<String, Object> textPart   = Map.of("text", PROMPT);
        Map<String, Object> content    = Map.of("parts", List.of(imagePart, textPart));
        Map<String, Object> body       = Map.of("contents", List.of(content));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
            GEMINI_URL + apiKey, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Gemini API respondió con " + response.getStatusCode());
        }

        // Extraer el texto de la respuesta
        JsonNode root     = objectMapper.readTree(response.getBody());
        String   jsonText = root.path("candidates").get(0)
                               .path("content").path("parts").get(0)
                               .path("text").asText();

        // Limpiar si Gemini envuelve la respuesta en ```json ... ```
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
