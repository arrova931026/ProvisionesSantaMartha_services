package com.shsm.api.service.impl;

import com.shsm.api.entity.CobroProgramado;
import com.shsm.api.entity.Notificacion;
import com.shsm.api.entity.Persona;
import com.shsm.api.entity.Usuario;
import com.shsm.api.repository.CobroProgramadoRepository;
import com.shsm.api.repository.NotificacionRepository;
import com.shsm.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppNotificationService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final RestClient.Builder restClientBuilder;
    private final CobroProgramadoRepository cobroRepository;
    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;

    @Value("${app.whatsapp.enabled:false}")
    private boolean enabled;
    @Value("${app.whatsapp.graph-api-version:v23.0}")
    private String graphApiVersion;
    @Value("${app.whatsapp.phone-number-id:}")
    private String phoneNumberId;
    @Value("${app.whatsapp.access-token:}")
    private String accessToken;
    @Value("${app.whatsapp.language:es_MX}")
    private String language;
    @Value("${app.whatsapp.reminder-days-before:5}")
    private int reminderDaysBefore;
    @Value("${app.whatsapp.time-zone:America/Mexico_City}")
    private String timeZone;
    @Value("${app.whatsapp.template-upcoming:shsm_pago_proximo}")
    private String upcomingTemplate;
    @Value("${app.whatsapp.template-overdue:shsm_pago_vencido}")
    private String overdueTemplate;

    @Scheduled(cron = "${app.whatsapp.reminder-cron:0 0 10 * * *}",
            zone = "${app.whatsapp.time-zone:America/Mexico_City}")
    public void enviarRecordatoriosDiarios() {
        LocalDate hoy = LocalDate.now(ZoneId.of(timeZone));
        cobroRepository.findCobrosProximosParaWhatsApp(hoy, hoy.plusDays(reminderDaysBefore))
                .forEach(cobro -> procesarAviso(cobro, false));
        cobroRepository.findCobrosVencidosParaWhatsApp(hoy)
                .forEach(cobro -> procesarAviso(cobro, true));
    }

    private void procesarAviso(CobroProgramado cobro, boolean vencido) {
        Persona persona = cobro.getContrato().getPersona();
        Optional<Usuario> usuarioOpt = usuarioRepository.findByPersonaId(persona.getId());
        if (usuarioOpt.isEmpty()) return;

        String tipo = vencido ? "PAGO_VENCIDO" : "PAGO_PROXIMO";
        String referencia = "cobro-" + cobro.getId() + (vencido ? "-vencido" : "-proximo");
        Notificacion notificacion = notificacionRepository.findByReferenciaExterna(referencia)
                .orElseGet(Notificacion::new);
        if (Boolean.TRUE.equals(notificacion.getEnviada())) return;

        notificacion.setUsuario(usuarioOpt.get());
        notificacion.setTipo(tipo);
        notificacion.setTitulo(vencido ? "Pago vencido" : "Próximo pago");
        notificacion.setCuerpo(cuerpo(cobro, vencido));
        notificacion.setCanal("APP");
        notificacion.setReferenciaExterna(referencia);
        notificacion.setEnviada(false);
        notificacion = notificacionRepository.save(notificacion);

        if (!Boolean.TRUE.equals(persona.getWhatsappOptIn()) || !enabled) return;

        String destinatario = normalizarTelefono(persona.getTelefono());
        if (destinatario == null) {
            log.warn("WhatsApp omitido: teléfono principal inválido para persona {}", persona.getId());
            return;
        }

        String template = vencido ? overdueTemplate : upcomingTemplate;
        List<String> parametros = List.of(
                nombre(persona),
                String.valueOf(cobro.getNumeroMensualidad()),
            cobro.getContrato().getNumeroContrato(),
                dinero(cobro.getMonto()),
                fechaVencimiento(cobro).format(DATE_FORMAT)
        );

        try {
            enviarPlantilla(destinatario, template, parametros);
            notificacion.setCanal("APP+WHATSAPP");
            notificacion.setEnviada(true);
            notificacion.setFechaEnvio(OffsetDateTime.now());
            notificacionRepository.save(notificacion);
        } catch (Exception ex) {
            log.warn("No se pudo enviar plantilla WhatsApp {} para cobro {}: {}",
                    template, cobro.getId(), ex.getMessage());
        }
    }

    private void enviarPlantilla(String destinatario, String templateName, List<String> values) {
        if (phoneNumberId.isBlank() || accessToken.isBlank()) {
            throw new IllegalStateException("Falta configurar WHATSAPP_PHONE_NUMBER_ID o WHATSAPP_ACCESS_TOKEN");
        }
        List<Map<String, String>> parameters = values.stream()
                .map(value -> Map.of("type", "text", "text", value))
                .toList();
        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "to", destinatario,
                "type", "template",
                "template", Map.of(
                        "name", templateName,
                        "language", Map.of("code", language),
                        "components", List.of(Map.of("type", "body", "parameters", parameters))
                )
        );

        restClientBuilder.build()
                .post()
                .uri("https://graph.facebook.com/{version}/{phoneNumberId}/messages",
                        graphApiVersion, phoneNumberId)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    private String normalizarTelefono(String telefono) {
        if (telefono == null) return null;
        String digits = telefono.replaceAll("\\D", "");
        if (digits.length() == 10) return "52" + digits;
        if (digits.length() == 12 && digits.startsWith("52")) return digits;
        if (digits.length() == 13 && digits.startsWith("521")) return "52" + digits.substring(3);
        return null;
    }

    private String cuerpo(CobroProgramado cobro, boolean vencido) {
        return (vencido ? "Pago vencido" : "Próximo pago") + " " + cobro.getNumeroMensualidad()
                + " del contrato " + cobro.getContrato().getNumeroContrato()
                + ", " + dinero(cobro.getMonto()) + ", fecha "
                + fechaVencimiento(cobro).format(DATE_FORMAT) + ".";
    }

    private LocalDate fechaVencimiento(CobroProgramado cobro) {
        return cobro.getFechaLimite() != null ? cobro.getFechaLimite() : cobro.getFechaProgramada();
    }

    private String nombre(Persona persona) {
        return persona.getNombre() + (persona.getApPaterno() == null ? "" : " " + persona.getApPaterno());
    }

    private String dinero(BigDecimal monto) {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX")).format(monto);
    }
}