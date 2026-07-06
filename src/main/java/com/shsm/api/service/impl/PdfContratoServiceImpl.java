package com.shsm.api.service.impl;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.shsm.api.entity.Contrato;
import com.shsm.api.exception.ResourceNotFoundException;
import com.shsm.api.repository.ContratoRepository;
import com.shsm.api.service.EmailService;
import com.shsm.api.service.PdfContratoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfContratoServiceImpl implements PdfContratoService {

    private final ContratoRepository contratoRepository;
    private final EmailService emailService;

    private static final String[] MESES = {
        "ENERO","FEBRERO","MARZO","ABRIL","MAYO","JUNIO",
        "JULIO","AGOSTO","SEPTIEMBRE","OCTUBRE","NOVIEMBRE","DICIEMBRE"
    };

    @Override
    @Transactional(readOnly = true)
    public byte[] generarPdf(Long contratoId) {
        Contrato c = contratoRepository.findById(contratoId)
                .filter(ct -> ct.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Contrato", contratoId));

        return buildPdf(c);
    }

    @Override
    @Transactional(readOnly = true)
    public void enviarPorCorreo(Long contratoId) {
        Contrato c = contratoRepository.findById(contratoId)
                .filter(ct -> ct.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Contrato", contratoId));

        byte[] pdf = buildPdf(c);
        String correo  = c.getPersona().getCorreo();
        String nombre  = c.getPersona().getNombre() + " " + c.getPersona().getApPaterno();
        if (correo != null && !correo.isBlank()) {
            emailService.enviarContrato(correo, nombre, c.getNumeroContrato(), pdf);
        }
    }

    // ── PDF builder ───────────────────────────────────────────────────────────

    private byte[] buildPdf(Contrato c) {
        // Datos del contrato
        String titular = c.getPersona().getNombre() + " "
                + c.getPersona().getApPaterno()
                + (c.getPersona().getApMaterno() != null
                   ? " " + c.getPersona().getApMaterno() : "");
        String planNombre   = c.getPlan().getNombre();
        BigDecimal precio   = c.getPrecioContratado();
        BigDecimal mensual  = c.getMensualidadPactada();
        BigDecimal enganche = precio.multiply(BigDecimal.valueOf(0.10)).setScale(2, RoundingMode.HALF_UP);

        LocalDate hoy = LocalDate.now();
        String dia    = String.valueOf(hoy.getDayOfMonth());
        String mes    = MESES[hoy.getMonthValue() - 1];
        String anio   = String.format("%02d", hoy.getYear() % 100); // "26" para 2026

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.LETTER, 50, 50, 60, 40);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font titleFont  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font boldFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Font smallFont  = FontFactory.getFont(FontFactory.HELVETICA, 8);

            // ── Encabezado ──────────────────────────────────────────────────
            Paragraph header = new Paragraph("SOCIEDAD HUMANISTA SANTA MARTHA S.A. DE C.V.", titleFont);
            header.setAlignment(Element.ALIGN_CENTER);
            doc.add(header);

            Paragraph subHeader = new Paragraph("CONTRATO FAMILIAR", boldFont);
            subHeader.setAlignment(Element.ALIGN_CENTER);
            subHeader.setSpacingAfter(12);
            doc.add(subHeader);

            // ── Presentación ────────────────────────────────────────────────
            String presentacion = "CONTRATO FAMILIAR QUE SUSCRIBEN POR UNA PARTE SOCIEDAD HUMANISTA SANTA MARTHA S.A. DE C.V., "
                    + "QUIEN EN LO SUCESIVO SE LE DENOMINARÁ COMO \"LA EMPRESA\", REPRESENTADA EN ESTE ACTO POR EL "
                    + "LICENCIADO ALBERTO ESTEBAN VARGAS RICO, EN SU CARÁCTER DE ADMINISTRADOR ÚNICO, Y POR LA OTRA PARTE "
                    + "EL (LA) C. " + titular.toUpperCase() + ", A QUIEN EN LO SUCESIVO SE LE DENOMINARÁ COMO \"CLIENTE\", "
                    + "A QUIENES CONJUNTAMENTE SE LES DENOMINARÁ COMO \"LAS PARTES\", EN TENOR DE LAS SIGUIENTES "
                    + "DECLARACIONES Y CLÁUSULAS.";
            Paragraph presP = new Paragraph(presentacion, normalFont);
            presP.setAlignment(Element.ALIGN_JUSTIFIED);
            presP.setSpacingAfter(10);
            doc.add(presP);

            // ── Declaraciones ────────────────────────────────────────────────
            addSection(doc, boldFont, normalFont, "I", "DECLARA \"LA EMPRESA\" A TRAVÉS DE SU ADMINISTRADOR ÚNICO:",
                new String[]{
                    "I.I   QUE ES UNA EMPRESA LEGALMENTE CONSTITUIDA CUYO DOMICILIO FISCAL SE ENCUENTRA EN CIPRÉS 905 "
                    + "COLONIA CHAPULTEPEC, EN LA CIUDAD DE POZA RICA DE HIDALGO, VERACRUZ, DEDICADA A LOS SERVICIOS "
                    + "FUNERARIOS DE INHUMACIÓN Y CREMACIÓN EN LA ZONA DE POZA RICA DE HIDALGO Y ÁREA METROPOLITANA.",

                    "I.II  QUE LOS SERVICIOS DE INHUMACIÓN MENCIONADOS CONSISTEN EN LA RECOLECCIÓN DEL CADÁVER, EQUIPO "
                    + "PARA INSTALAR CAPILLA ARDIENTE EN EL DOMICILIO QUE SEÑALE EL CLIENTE O SUS DEUDOS, CARROSA PARA "
                    + "EL TRASLADO DENTRO DE LA CIUDAD DE POZA RICA Y ÁREA METROPOLITANA, PERSONAL CAPACITADO PARA LA "
                    + "ATENCIÓN DEL SERVICIO FUNERARIO, ATAÚD METÁLICO DE PRIMERA, NO INCLUYENDO LOTE DE PANTEÓN NI "
                    + "PAGO DE IMPUESTOS, ACTAS Y PERMISOS.",

                    "I.III QUE LOS SERVICIOS DE CREMACIÓN MENCIONADOS CONSISTEN EN LA RECOLECCIÓN DEL CADÁVER, EQUIPO "
                    + "PARA INSTALAR CAPILLA ARDIENTE EN EL DOMICILIO, CARROSA PARA EL TRASLADO DENTRO DE LA CIUDAD Y "
                    + "ÁREA METROPOLITANA, PERSONAL CAPACITADO PARA LA ATENCIÓN DEL SERVICIO FUNERARIO, RENTA DE ATAÚD "
                    + "DE MADERA DE PINO BARNIZADO, CREMACIÓN DE CADÁVER, URNA BÁSICA PARA RESTOS ÁRIDOS Y CONSTANCIA "
                    + "DE CREMACIÓN. LOS IMPUESTOS, ACTAS Y PERMISOS SON A CARGO DEL CLIENTE O SUS DEUDOS."
                }
            );

            addSection(doc, boldFont, normalFont, "II", "DECLARA EL(LA) \"CLIENTE\":",
                new String[]{
                    "II.I  ESTAR DE ACUERDO EN REALIZAR EL PAGO DE SU PLAN DE PREVISIÓN FUNERARIA "
                    + planNombre.toUpperCase()
                    + " CON UN COSTO DE $" + fmtMonto(precio)
                    + ", PAGANDO UN ENGANCHE DEL 10% DEL PRECIO DEL SERVICIO CONTRATADO DE $" + fmtMonto(enganche)
                    + " Y LAS " + c.getPlan().getDuracionMeses() + " MENSUALIDADES DE $" + fmtMonto(mensual) + "."
                }
            );

            addSection(doc, boldFont, normalFont, "III", "DECLARAN \"LAS PARTES\":",
                new String[]{
                    "III.I  ESTE CONTRATO ES PARA EL TITULAR DEL PRESENTE, PERO TAMBIÉN ES TRANSFERIBLE PARA FAMILIARES "
                    + "O AMIGOS DEL CLIENTE.",
                    "III.II QUE AL FALLECIMIENTO DEL CLIENTE, LA EMPRESA SE OBLIGA A BRINDAR EL SERVICIO CONTRATADO YA SEA "
                    + "DE INHUMACIÓN O CREMACIÓN, DANDO POR TERMINADAS LAS RESPONSABILIDADES DE LA EMPRESA.",
                    "III.III QUE NO TIENEN INCONVENIENTE EN LIQUIDAR LOS PORTADORES DEL CONTRATO A LA HORA DE SOLICITAR EL "
                    + "SERVICIO FUNERARIO CONTRATADO EN CASO DE NO ESTAR PAGADO EN SU TOTALIDAD."
                }
            );

            // ── Cláusulas ────────────────────────────────────────────────────
            Paragraph clausulasTitle = new Paragraph("CLÁUSULAS", boldFont);
            clausulasTitle.setSpacingBefore(8);
            clausulasTitle.setSpacingAfter(4);
            doc.add(clausulasTitle);

            String[] clausulas = {
                "PRIMERA:  LA VIGENCIA DEL PRESENTE CONTRATO ES A PARTIR DEL PAGO DEL ENGANCHE HASTA EL TIEMPO EN QUE SE REQUIERA EL SERVICIO FUNERARIO CONTRATADO.",
                "SEGUNDA:  \"LA EMPRESA\" SE HARÁ CARGO DE LA GESTIÓN DE TRÁMITES ANTE LAS DEPENDENCIAS CORRESPONDIENTES, QUEDANDO EL PAGO DE IMPUESTOS Y DERECHOS A CARGO DEL \"CLIENTE\".",
                "TERCERA:  EL VALOR DE ESTE CONTRATO ES DE LA CANTIDAD DE $" + fmtMonto(precio)
                        + " ENGANCHE 10% $" + fmtMonto(enganche) + " MENSUALIDAD $" + fmtMonto(mensual) + ".",
                "CUARTA:   LAS PARTES CONVIENEN QUE PARA LA INTERPRETACIÓN Y CUMPLIMIENTO DE ESTE CONTRATO SE SOMETEN A LA "
                        + "COMPETENCIA DE LOS TRIBUNALES CON RESIDENCIA EN EL MUNICIPIO DE POZA RICA DE HIDALGO, VERACRUZ, "
                        + "RENUNCIANDO EXPRESAMENTE A CUALQUIER OTRO FUERO PRESENTE O FUTURO. ENTERADAS LAS PARTES DEL CONTENIDO, "
                        + "ALCANCE Y FUERZA LEGAL DEL PRESENTE CONTRATO Y POR NO CONTENER DOLO, ERROR, MALA FE, NI CLÁUSULA "
                        + "CONTRARIA A DERECHO, LO FIRMAN AL CALCE, SIENDO EL DÍA " + dia + " DEL MES DE " + mes
                        + " DEL AÑO DOS MIL " + anio + ", EN LA CIUDAD DE POZA RICA DE HIDALGO, VERACRUZ."
            };

            for (String cl : clausulas) {
                Paragraph p = new Paragraph(cl, normalFont);
                p.setAlignment(Element.ALIGN_JUSTIFIED);
                p.setSpacingAfter(5);
                doc.add(p);
            }

            // ── Firmas ────────────────────────────────────────────────────────
            doc.add(Chunk.NEWLINE);
            doc.add(Chunk.NEWLINE);

            PdfPTable sigTable = new PdfPTable(2);
            sigTable.setWidthPercentage(90);
            sigTable.setSpacingBefore(15);

            PdfPCell c1 = sigCell("SOCIEDAD HUMANISTA SANTA MARTHA S.A. DE C.V.\n"
                    + "LICENCIADO ALBERTO ESTEBAN VARGAS RICO\nADMINISTRADOR ÚNICO", boldFont, smallFont);
            PdfPCell c2 = sigCell("EL(LA) CLIENTE\n" + titular.toUpperCase() + "\nFIRMA", boldFont, smallFont);
            sigTable.addCell(c1);
            sigTable.addCell(c2);
            doc.add(sigTable);

            // ── Número de contrato ────────────────────────────────────────────
            doc.add(Chunk.NEWLINE);
            Paragraph numC = new Paragraph("Contrato Nº: " + c.getNumeroContrato(), smallFont);
            numC.setAlignment(Element.ALIGN_RIGHT);
            doc.add(numC);

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generando PDF del contrato {}: {}", c.getId(), e.getMessage(), e);
            throw new RuntimeException("No se pudo generar el PDF del contrato", e);
        }
    }

    private void addSection(Document doc, Font boldFont, Font normalFont, String num, String titulo, String[] textos) throws DocumentException {
        Paragraph t = new Paragraph("DECLARACIÓN " + num + " – " + titulo, boldFont);
        t.setSpacingBefore(8);
        t.setSpacingAfter(3);
        doc.add(t);
        for (String texto : textos) {
            Paragraph p = new Paragraph(texto, normalFont);
            p.setAlignment(Element.ALIGN_JUSTIFIED);
            p.setIndentationLeft(15);
            p.setSpacingAfter(3);
            doc.add(p);
        }
    }

    private PdfPCell sigCell(String text, Font boldFont, Font smallFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(8);
        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) {
            Font f = (i == 0) ? boldFont : smallFont;
            Paragraph p = new Paragraph(lines[i], f);
            p.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(p);
        }
        // firma line
        Paragraph linea = new Paragraph("_______________________________", smallFont);
        linea.setAlignment(Element.ALIGN_CENTER);
        linea.setSpacingBefore(20);
        cell.addElement(linea);
        return cell;
    }

    private String fmtMonto(BigDecimal monto) {
        return String.format(Locale.US, "%,.2f", monto);
    }
}
