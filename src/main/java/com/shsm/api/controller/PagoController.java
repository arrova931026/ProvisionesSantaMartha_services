package com.shsm.api.controller;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.resources.preference.Preference;
import com.shsm.api.dto.pago.PagoRequest;
import com.shsm.api.dto.pago.PagoResponse;
import com.shsm.api.repository.CobroProgramadoRepository;
import com.shsm.api.service.PagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@RestController
@RequestMapping("/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService;
    private final CobroProgramadoRepository cobroRepository;

    @Value("${mercadopago.access-token}")
    private String mpAccessToken;

    @Value("${mercadopago.back-url-base}")
    private String mpBackUrlBase;

    @Value("${mercadopago.webhook-secret:}")
    private String mpWebhookSecret;

    /** Crea una preferencia de Checkout Pro para el cobro indicado */
    @Transactional(readOnly = true)
    @PostMapping("/cobros/{cobroId}/preferencia-mp")
    public ResponseEntity<Map<String, String>> crearPreferenciaMP(
            @PathVariable Long cobroId) {

        var cobro = cobroRepository.findById(cobroId)
                .orElseThrow(() -> new RuntimeException("Cobro no encontrado: " + cobroId));

        try {
            MercadoPagoConfig.setAccessToken(mpAccessToken);

            var item = PreferenceItemRequest.builder()
                    .title("Mensualidad #" + cobro.getNumeroMensualidad()
                           + " - " + cobro.getContrato().getNumeroContrato())
                    .quantity(1)
                    .unitPrice(cobro.getMonto())
                    .currencyId("MXN")
                    .build();

            // back_urls solo si el dominio es real (MP rechaza localhost)
            boolean esLocal = mpBackUrlBase.contains("localhost") || mpBackUrlBase.contains("127.0.0.1");

            var requestBuilder = PreferenceRequest.builder()
                    .items(List.of(item))
                    .externalReference(cobroId.toString())
                    .statementDescriptor("SHSM");

            if (!esLocal) {
                requestBuilder.backUrls(PreferenceBackUrlsRequest.builder()
                        .success(mpBackUrlBase + "?pago=exitoso&cobro=" + cobroId)
                        .failure(mpBackUrlBase + "?pago=fallido&cobro=" + cobroId)
                        .pending(mpBackUrlBase + "?pago=pendiente&cobro=" + cobroId)
                        .build())
                    .autoReturn("approved");
            }

            Preference preference = new PreferenceClient().create(requestBuilder.build());

            return ResponseEntity.ok(Map.of(
                    "id",              preference.getId(),
                    "initPoint",       preference.getInitPoint(),
                    "sandboxInitPoint",preference.getSandboxInitPoint()
            ));
        } catch (MPApiException e) {
            // Error real de la API de Mercado Pago — incluye el JSON de respuesta
            String detalle = e.getApiResponse() != null ? e.getApiResponse().getContent() : e.getMessage();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "MP API " + e.getStatusCode() + ": " + detalle));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "Error al crear preferencia MP: " + e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENTE')")
    public ResponseEntity<Page<PagoResponse>> listar(
            @RequestParam(required = false) Long contratoId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(pagoService.listar(contratoId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PagoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(pagoService.obtener(id));
    }

    @GetMapping("/contrato/{contratoId}")
    public ResponseEntity<List<PagoResponse>> listarPorContrato(@PathVariable Long contratoId) {
        return ResponseEntity.ok(pagoService.listarPorContrato(contratoId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENTE')")
    public ResponseEntity<PagoResponse> registrar(
            @Valid @RequestBody PagoRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pagoService.registrar(request, userDetails.getUsername()));
    }

    /**
     * Webhook de MercadoPago — recibe notificaciones de pago aprobado.
     * Soporta el formato nuevo (JSON body con type=payment) y el IPN legacy (query params).
     * Valida la firma HMAC-SHA256 si MP_WEBHOOK_SECRET está configurado.
     * Siempre devuelve 200 para que MP no reintente innecesariamente.
     */
    @PostMapping("/webhook-mp")
    public ResponseEntity<Void> webhookMP(
            @RequestHeader(value = "x-signature",   required = false) String xSignature,
            @RequestHeader(value = "x-request-id",  required = false) String xRequestId,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String id,
            @RequestBody(required = false) Map<String, Object> body) {
        try {
            Long   paymentId = null;
            String dataId    = null;

            // Formato nuevo (Webhooks): { "type": "payment", "data": { "id": "123" } }
            if (body != null && "payment".equals(body.get("type"))) {
                Object data = body.get("data");
                if (data instanceof Map<?, ?> dataMap) {
                    Object did = dataMap.get("id");
                    if (did != null) { dataId = did.toString(); paymentId = Long.valueOf(dataId); }
                }
            }

            // Formato IPN legacy: ?topic=payment&id=123
            if (paymentId == null && "payment".equals(topic) && id != null) {
                dataId = id;
                paymentId = Long.parseLong(id);
            }

            if (paymentId != null) {
                // Validar firma si el secret está configurado
                if (dataId != null && xSignature != null
                        && !validarFirmaMP(xSignature, xRequestId, dataId)) {
                    Logger.getLogger(PagoController.class.getName())
                          .warning("Webhook MP rechazado: firma inválida");
                    return ResponseEntity.ok().build(); // 200 para que MP no reintente
                }
                pagoService.procesarWebhookMP(paymentId);
            }
        } catch (Exception e) {
            Logger.getLogger(PagoController.class.getName())
                  .warning("Error en webhook MP: " + e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    /** Valida la firma HMAC-SHA256 enviada por MercadoPago en el header x-signature. */
    private boolean validarFirmaMP(String xSignature, String xRequestId, String dataId) {
        if (mpWebhookSecret == null || mpWebhookSecret.isBlank()) return true; // sin secret → omitir
        try {
            String ts = null, v1 = null;
            for (String part : xSignature.split(",")) {
                String[] kv = part.trim().split("=", 2);
                if (kv.length == 2) {
                    if ("ts".equals(kv[0].trim())) ts = kv[1].trim();
                    if ("v1".equals(kv[0].trim())) v1 = kv[1].trim();
                }
            }
            if (ts == null || v1 == null) return false;

            String mensaje = "id:" + dataId
                    + ";request-id:" + (xRequestId != null ? xRequestId : "")
                    + ";ts:" + ts + ";";

            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(mpWebhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = hmac.doFinal(mensaje.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString().equals(v1);
        } catch (Exception e) {
            return false;
        }
    }
}
