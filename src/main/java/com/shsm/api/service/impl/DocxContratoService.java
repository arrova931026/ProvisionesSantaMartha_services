package com.shsm.api.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.shsm.api.entity.Beneficiario;
import com.shsm.api.entity.Contrato;
import com.shsm.api.entity.Persona;
import com.shsm.api.exception.ResourceNotFoundException;
import com.shsm.api.repository.BeneficiarioRepository;
import com.shsm.api.repository.ContratoRepository;
import com.shsm.api.service.CalendarioCuotas;
import com.shsm.api.service.LocalFileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Llena las plantillas .docx de los contratos (Mutualista y Previsión de Humanos) con los datos del contrato. */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocxContratoService {

    private static final String TEMPLATE_MUTUALISTA = "contratos/mutualista.docx";
    private static final String TEMPLATE_MUTUALISTA_MASCOTAS = "contratos/mutualistamascotas.docx";
    private static final String TEMPLATE_PREVISION_MASCOTAS = "contratos/previsionmascotas.docx";
    private static final String TEMPLATE_PREVISION_HUMANOS = "contratos/previsionhumanos.docx";
    private static final String QR_REL_ID = "rIdQrVideo";
    private static final String QR_MEDIA = "word/media/qr_video.png";
    private static final int QR_EMU = 900000; // 2.5 cm

    private static final String RPR = "(?:<w:rPr>(?:(?!</w:rPr>).)*</w:rPr>)?";
    private static final Pattern CAMPO = Pattern.compile(
            "&lt;(?:</w:t></w:r><w:r>" + RPR + "<w:t[^>]*>)?(?:llenar campos?(?: solo si existe| con [^&<]*?)?\\s*|NOMBRE DEL CLIENTE)&gt;",
            Pattern.DOTALL);
    private static final Pattern QR_RUNS = Pattern.compile(
            "<w:r>" + RPR + "<w:t[^>]*>&lt;QR QUE DESCARGUE VIDEO DE CONSENTIMIENTO DEL CLIENTE&gt;</w:t></w:r>|"
            + "<w:r>" + RPR + "<w:t[^>]*>&lt;</w:t></w:r>"
            + "<w:r>" + RPR + "<w:t[^>]*>QR QUE DESCARGUE VIDEO DE CONSENTIMIENTO DEL CLIENTE</w:t></w:r>"
            + "<w:r>" + RPR + "<w:t[^>]*>&gt;</w:t></w:r>|"
            + "<w:r>" + RPR + "<w:t[^>]*>&lt;QR para descargar consentimiento&gt;</w:t></w:r>|"
            + "<w:r>" + RPR + "<w:br/><w:t>&lt;</w:t></w:r>"
            + "<w:r>" + RPR + "<w:t[^>]*>QR con consentimiente del cliente&gt;</w:t></w:r>",
            Pattern.DOTALL);

    private static final String INI = "\uE000";
    private static final String FIN = "\uE001";
    private static final Pattern RUN_MARCADO = Pattern.compile(
            "<w:r>(<w:rPr>(?:(?!</w:rPr>).)*</w:rPr>)?((?:<w:br/>|<w:t[^>]*>[^<]*</w:t>)+)</w:r>",
            Pattern.DOTALL);

    private static final String[] MESES = {
        "ENERO", "FEBRERO", "MARZO", "ABRIL", "MAYO", "JUNIO",
        "JULIO", "AGOSTO", "SEPTIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE"
    };

    private final ContratoRepository contratoRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final LocalFileStorageService storage;

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.public-api-url:}")
    private String publicApiUrl;

    @Transactional(readOnly = true)
    public byte[] generar(Long contratoId) {
        Contrato c = contratoRepository.findById(contratoId)
                .filter(ct -> ct.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Contrato", contratoId));

        Persona p = c.getPersona();
        LocalDate fecha = c.getFechaInicio() != null ? c.getFechaInicio() : LocalDate.now();

        List<String> valores = new ArrayList<>();
        int nBeneficiarios = 0;
        String template;
        if ("HUMANOS".equals(c.getPlan().getTipoPlan())) {
            template = TEMPLATE_PREVISION_HUMANOS;
            String nombre = nombreCompleto(p).toUpperCase();
            String precio = dinero(c.getPrecioContratado());
            String enganche = dinero(c.getPrecioContratado().multiply(BigDecimal.valueOf(0.10)));
            String mensualidad = dinero(c.getMensualidadPactada());
            int cuotas = CalendarioCuotas.generar(c.getFechaInicio(), c.getPlan().getDuracionMeses(),
                    c.getFrecuenciaPago()).size();
            valores.addAll(List.of(nombre, c.getPlan().getNombre().toUpperCase(), precio, enganche,
                    String.valueOf(cuotas), mensualidad, precio, enganche, mensualidad,
                    String.valueOf(fecha.getDayOfMonth()), MESES[fecha.getMonthValue() - 1],
                    String.valueOf(fecha.getYear()), nombre));
        } else if ("MASCOTAS".equals(c.getPlan().getTipoPlan())) {
            template = TEMPLATE_PREVISION_MASCOTAS;
            BigDecimal precio = c.getPrecioContratado();
            BigDecimal enganche = precio.multiply(BigDecimal.valueOf(0.10)).setScale(2, java.math.RoundingMode.HALF_UP);
            int cuotas = CalendarioCuotas.generar(c.getFechaInicio(), c.getPlan().getDuracionMeses(),
                    c.getFrecuenciaPago()).size();
            var m = c.getMascota();
            valores.addAll(List.of(nombreCompleto(p).toUpperCase(), dinero(enganche), dinero(precio.subtract(enganche)),
                    String.valueOf(cuotas), dinero(c.getMensualidadPactada()),
                    String.valueOf(fecha.getDayOfMonth()), MESES[fecha.getMonthValue() - 1],
                    String.valueOf(fecha.getYear()), nombreCompleto(p).toUpperCase(),
                    nvl(m.getNombre()), nvl(m.getEspecie()), nvl(m.getRaza()),
                    m.getPeso() != null ? m.getPeso().stripTrailingZeros().toPlainString() + " kg" : "",
                    nvl(m.getColorCaracteristicas())));
        } else if ("MUTUALISTA_MASCOTAS".equals(c.getPlan().getTipoPlan())) {
            template = TEMPLATE_MUTUALISTA_MASCOTAS;
            DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate fin = c.getFechaVencimiento() != null ? c.getFechaVencimiento() : fecha.plusYears(1);
            var m = c.getMascota();
            valores.addAll(List.of(nombreCompleto(p).toUpperCase(), fecha.format(df), fin.format(df),
                    String.valueOf(fecha.getDayOfMonth()), MESES[fecha.getMonthValue() - 1],
                    String.valueOf(fecha.getYear()), nombreCompleto(p).toUpperCase(),
                    nvl(m.getNombre()), nvl(m.getEspecie()), nvl(m.getRaza()),
                    m.getPeso() != null ? m.getPeso().stripTrailingZeros().toPlainString() + " kg" : "",
                    nvl(m.getColorCaracteristicas())));
        } else {
            template = TEMPLATE_MUTUALISTA;
            valores.add(nombreCompleto(p).toUpperCase());
            valores.add(String.valueOf(fecha.getDayOfMonth()));
            valores.add(MESES[fecha.getMonthValue() - 1]);
            valores.add(String.valueOf(fecha.getYear()));

            List<Beneficiario> beneficiarios = beneficiarioRepository
                    .findByContratoIdAndActivoTrue(contratoId).stream()
                    .filter(b -> !Boolean.TRUE.equals(b.getEsTitular()))
                    .toList();
            for (Beneficiario b : beneficiarios) {
                valores.add(nombreCompleto(b.getPersona()).toUpperCase());
                valores.add(b.getParentesco() != null ? b.getParentesco().getNombre().toUpperCase() : "");
            }
            nBeneficiarios = beneficiarios.size();
        }

        byte[] qr = storage.findVideoConsent(p.getId()).isPresent()
                ? generarQr(urlVideo(c))
                : null;

        try {
            return rellenar(template, valores, qr, nBeneficiarios);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el contrato", e);
        }
    }

    /** Genera el contrato y lo convierte a PDF con LibreOffice. */
    public byte[] generarPdf(Long contratoId) {
        byte[] docx = generar(contratoId);
        try {
            Path dir = Files.createTempDirectory("contrato");
            try {
                Path in = dir.resolve("contrato.docx");
                Files.write(in, docx);
                Process pr = new ProcessBuilder("soffice", "--headless", "--norestore",
                        "-env:UserInstallation=file://" + dir.resolve("profile").toAbsolutePath(),
                        "--convert-to", "pdf", "--outdir", dir.toString(), in.toString())
                        .redirectErrorStream(true).start();
                String salida = new String(pr.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                if (!pr.waitFor(90, TimeUnit.SECONDS)) {
                    pr.destroyForcibly();
                    throw new IllegalStateException("Tiempo agotado al convertir el contrato a PDF");
                }
                Path pdf = dir.resolve("contrato.pdf");
                if (!Files.exists(pdf)) {
                    throw new IllegalStateException("No se pudo convertir el contrato a PDF: " + salida);
                }
                return Files.readAllBytes(pdf);
            } finally {
                try (var w = Files.walk(dir)) {
                    w.sorted(java.util.Comparator.reverseOrder()).forEach(f -> f.toFile().delete());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el PDF del contrato", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    /** Firma HMAC que autoriza la descarga pública del video de una persona. */
    public String firmar(Long personaId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] h = mac.doFinal(("video:" + personaId).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean firmaValida(Long personaId, String firma) {
        return firma != null && MessageDigest.isEqual(
                firmar(personaId).getBytes(StandardCharsets.UTF_8),
                firma.getBytes(StandardCharsets.UTF_8));
    }

    private String urlVideo(Contrato c) {
        String base = publicApiUrl != null && !publicApiUrl.isBlank()
                ? publicApiUrl.replaceAll("/+$", "")
                : ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        return base + "/contratos/" + c.getId() + "/video-consentimiento?firma=" + firmar(c.getPersona().getId());
    }

    private static String nvl(String v) {
        return v == null ? "" : v;
    }

    private static String dinero(BigDecimal v) {
        return String.format(java.util.Locale.US, "%,.2f", v.setScale(2, java.math.RoundingMode.HALF_UP));
    }

    private static String nombreCompleto(Persona p) {
        StringBuilder sb = new StringBuilder(p.getNombre());
        if (p.getApPaterno() != null && !p.getApPaterno().isBlank()) sb.append(' ').append(p.getApPaterno());
        if (p.getApMaterno() != null && !p.getApMaterno().isBlank()) sb.append(' ').append(p.getApMaterno());
        return sb.toString().trim();
    }

    private static byte[] generarQr(String url) {
        try {
            BitMatrix m = new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, 400, 400,
                    Map.of(EncodeHintType.MARGIN, 1, EncodeHintType.CHARACTER_SET, "UTF-8"));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(m, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el código QR", e);
        }
    }

    private byte[] rellenar(String template, List<String> valores, byte[] qr, int beneficiarios) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream in = new ClassPathResource(template).getInputStream();
             ZipInputStream zin = new ZipInputStream(in);
             ZipOutputStream zout = new ZipOutputStream(out)) {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                byte[] data = zin.readAllBytes();
                if (e.getName().equals("word/document.xml")) {
                    data = procesarDocumento(new String(data, StandardCharsets.UTF_8), valores, qr != null, beneficiarios)
                            .getBytes(StandardCharsets.UTF_8);
                } else if (e.getName().equals("word/_rels/document.xml.rels") && qr != null) {
                    String rels = new String(data, StandardCharsets.UTF_8).replace("</Relationships>",
                            "<Relationship Id=\"" + QR_REL_ID + "\" "
                            + "Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\" "
                            + "Target=\"media/qr_video.png\"/></Relationships>");
                    data = rels.getBytes(StandardCharsets.UTF_8);
                }
                zout.putNextEntry(new ZipEntry(e.getName()));
                zout.write(data);
                zout.closeEntry();
            }
            if (qr != null) {
                zout.putNextEntry(new ZipEntry(QR_MEDIA));
                zout.write(qr);
                zout.closeEntry();
            }
        }
        return out.toByteArray();
    }

    private String procesarDocumento(String xml, List<String> valores, boolean conQr, int beneficiarios) {
        xml = ajustarBeneficiarios(xml, beneficiarios);
        Matcher m = CAMPO.matcher(xml);
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (m.find()) {
            String v = i < valores.size() ? valores.get(i) : "";
            i++;
            String marcado = v.isEmpty() ? "" : INI + escapar(v) + FIN;
            m.appendReplacement(sb, Matcher.quoteReplacement(marcado));
        }
        m.appendTail(sb);
        String res = negritas(sb.toString());
        res = QR_RUNS.matcher(res).replaceAll(Matcher.quoteReplacement(conQr ? dibujoQr() : ""));
        if (conQr && res.contains("name=\"QR video consentimiento\"")) {
            // El QR sustituye la línea de firma
            res = res.replaceFirst("(</w:drawing></w:r>)<w:r>" + RPR + "<w:br/></w:r><w:r>" + RPR + "<w:t[^>]*>_{5,}</w:t></w:r>", "$1");
            // Quita un párrafo vacío previo al QR para que no desborde a otra página
            res = res.replaceFirst("<w:p><w:pPr>(?:(?!</w:pPr>).)*</w:pPr><w:r>(?:<w:rPr>(?:(?!</w:rPr>).)*</w:rPr>)?</w:r></w:p>(?=<w:p><w:pPr>(?:(?!</w:pPr>).)*</w:pPr><w:r><w:drawing>)", "");
        }
        return res;
    }

    /** Divide cada run que contiene un valor marcado para que el valor quede en negritas. */
    private static String negritas(String xml) {
        Matcher m = RUN_MARCADO.matcher(xml);
        StringBuilder sb = new StringBuilder();
        Pattern parte = Pattern.compile("<w:br/>|<w:t[^>]*>([^<]*)</w:t>");
        while (m.find()) {
            if (!m.group(2).contains(INI)) {
                m.appendReplacement(sb, Matcher.quoteReplacement(m.group()));
                continue;
            }
            String rpr = m.group(1) == null ? "" : m.group(1);
            String rprBold = rpr.isEmpty() ? "<w:rPr><w:b/><w:bCs/></w:rPr>"
                    : rpr.replace("<w:rPr>", "<w:rPr><w:b/><w:bCs/>");
            StringBuilder r = new StringBuilder();
            Matcher pm = parte.matcher(m.group(2));
            while (pm.find()) {
                if (pm.group(1) == null) {
                    r.append("<w:r>").append(rpr).append("<w:br/></w:r>");
                    continue;
                }
                String texto = pm.group(1);
                int pos = 0;
                while (true) {
                    int i = texto.indexOf(INI, pos);
                    if (i < 0) break;
                    int f = texto.indexOf(FIN, i);
                    run(r, rpr, texto.substring(pos, i));
                    run(r, rprBold, texto.substring(i + 1, f));
                    pos = f + 1;
                }
                run(r, rpr, texto.substring(pos));
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(r.toString()));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static void run(StringBuilder sb, String rpr, String texto) {
        if (texto.isEmpty()) return;
        sb.append("<w:r>").append(rpr).append("<w:t xml:space=\"preserve\">").append(texto).append("</w:t></w:r>");
    }

    /** Deja solo las filas de beneficiarios existentes; sin beneficiarios elimina la tabla completa. */
    private static String ajustarBeneficiarios(String xml, int n) {
        int i = xml.indexOf("solo si existe");
        if (i < 0) return xml;
        int ini = xml.lastIndexOf("<w:tbl>", i);
        int fin = xml.indexOf("</w:tbl>", i) + "</w:tbl>".length();
        String tabla = xml.substring(ini, fin);
        if (n == 0) return xml.substring(0, ini) + xml.substring(fin);
        List<String> filas = new ArrayList<>();
        Matcher fm = Pattern.compile("<w:tr>(?:(?!</w:tr>).)*</w:tr>", Pattern.DOTALL).matcher(tabla);
        while (fm.find()) filas.add(fm.group());
        String datos = filas.get(filas.size() - 1);
        int primera = tabla.indexOf(filas.get(0));
        int ultima = tabla.lastIndexOf(filas.get(filas.size() - 1)) + datos.length();
        StringBuilder nueva = new StringBuilder(tabla.substring(0, primera));
        for (int k = 0; k < filas.size() - 2; k++) nueva.append(filas.get(k));
        for (int k = 0; k < n; k++) nueva.append(datos);
        nueva.append(tabla.substring(ultima));
        return xml.substring(0, ini) + nueva + xml.substring(fin);
    }

    private static String escapar(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String dibujoQr() {
        return "<w:r><w:drawing>"
            + "<wp:inline distT=\"0\" distB=\"0\" distL=\"0\" distR=\"0\">"
            + "<wp:extent cx=\"" + QR_EMU + "\" cy=\"" + QR_EMU + "\"/>"
            + "<wp:docPr id=\"9001\" name=\"QR video consentimiento\" descr=\"QR video de consentimiento\"/>"
            + "<a:graphic xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\">"
            + "<a:graphicData uri=\"http://schemas.openxmlformats.org/drawingml/2006/picture\">"
            + "<pic:pic xmlns:pic=\"http://schemas.openxmlformats.org/drawingml/2006/picture\">"
            + "<pic:nvPicPr><pic:cNvPr id=\"9001\" name=\"qr_video.png\"/><pic:cNvPicPr/></pic:nvPicPr>"
            + "<pic:blipFill><a:blip r:embed=\"" + QR_REL_ID + "\"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill>"
            + "<pic:spPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"" + QR_EMU + "\" cy=\"" + QR_EMU + "\"/></a:xfrm>"
            + "<a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom></pic:spPr>"
            + "</pic:pic></a:graphicData></a:graphic></wp:inline></w:drawing></w:r>";
    }
}
