package com.shsm.api.service.impl;

import com.shsm.api.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // @Async desactivado temporalmente para ver errores en consola
    @Override
    public void enviarRecuperacionContrasena(String destinatario, String nombreCompleto, String enlace) {
        log.info("Intentando enviar correo de recuperación a: {}", destinatario);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(from, "Sociedad Humanista Santa Martha");
            helper.setTo(destinatario);
            helper.setSubject("Recuperación de contraseña – Sociedad Humanista Santa Martha");
            helper.setText(construirHtml(nombreCompleto, enlace), true);

            mailSender.send(message);
            log.info("Correo de recuperación enviado exitosamente a: {}", destinatario);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("ERROR al enviar correo a {}: {}", destinatario, e.getMessage(), e);
        }
    }

    private String construirHtml(String nombre, String enlace) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head><meta charset="UTF-8"></head>
                <body style="font-family: 'Inter', Arial, sans-serif; background:#f4f6f8; margin:0; padding:0;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f8; padding:40px 0;">
                    <tr><td align="center">
                      <table width="520" cellpadding="0" cellspacing="0"
                             style="background:#ffffff; border-radius:12px; overflow:hidden;
                                    box-shadow:0 4px 24px rgba(0,0,0,0.08);">
                        <!-- HEADER -->
                        <tr>
                          <td style="background:#1e6fa3; padding:28px 40px; text-align:center;">
                            <h1 style="color:#ffffff; font-size:1.3rem; margin:0; font-weight:700;">
                              Sociedad Humanista Santa Martha
                            </h1>
                          </td>
                        </tr>
                        <!-- BODY -->
                        <tr>
                          <td style="padding:36px 40px;">
                            <p style="font-size:1rem; color:#2c3e50; margin-top:0;">Hola, <strong>%s</strong></p>
                            <p style="font-size:0.95rem; color:#555; line-height:1.6;">
                              Recibimos una solicitud para restablecer la contraseña de tu cuenta.
                              Haz clic en el botón de abajo para crear una nueva contraseña.
                            </p>
                            <div style="text-align:center; margin:32px 0;">
                              <a href="%s"
                                 style="background:#1e6fa3; color:#ffffff; text-decoration:none;
                                        padding:14px 32px; border-radius:50px; font-size:0.95rem;
                                        font-weight:600; display:inline-block;">
                                Restablecer contraseña
                              </a>
                            </div>
                            <p style="font-size:0.82rem; color:#888; line-height:1.6;">
                              Este enlace es válido por <strong>1 hora</strong>.<br>
                              Si no solicitaste restablecer tu contraseña, puedes ignorar este correo.
                            </p>
                            <hr style="border:none; border-top:1px solid #eee; margin:24px 0;">
                            <p style="font-size:0.78rem; color:#aaa; text-align:center; margin:0;">
                              Si el botón no funciona, copia y pega este enlace en tu navegador:<br>
                              <a href="%s" style="color:#1e6fa3; word-break:break-all;">%s</a>
                            </p>
                          </td>
                        </tr>
                        <!-- FOOTER -->
                        <tr>
                          <td style="background:#f8f9fa; padding:16px 40px; text-align:center;">
                            <p style="font-size:0.75rem; color:#aaa; margin:0;">
                              © 2026 Sociedad Humanista Santa Martha S.A. de C.V.
                            </p>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(nombre, enlace, enlace, enlace);
    }
}
