package com.shsm.api.service;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.shsm.api.entity.Contrato;
import com.shsm.api.repository.ContratoRepository;
import com.shsm.api.service.impl.PdfContratoServiceImpl;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfContratoMutualistaMascotasTest {
    @Mock ContratoRepository contratos;
    @Mock EmailService email;
    @InjectMocks PdfContratoServiceImpl service;

    @ParameterizedTest
    @CsvSource({"2026-10-29,2027-10-29", "2024-02-29,2025-02-28"})
    void pdfContieneMascotaImportesYVigenciaSinEngancheNiFamilia(String inicio, String fin)
            throws Exception {
        Contrato contrato = MutualistaMascotasFixtures.contrato();
        contrato.setFechaInicio(LocalDate.parse(inicio));
        contrato.setFechaVencimiento(LocalDate.parse(fin));
        when(contratos.findById(10L)).thenReturn(Optional.of(contrato));

        byte[] pdf = service.generarPdf(10L);

        assertThat(pdf).isNotEmpty();
        PdfReader reader = new PdfReader(pdf);
        StringBuilder contenido = new StringBuilder();
        try {
            assertThat(reader.getNumberOfPages()).isPositive();
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            for (int pagina = 1; pagina <= reader.getNumberOfPages(); pagina++) {
                contenido.append(extractor.getTextFromPage(pagina)).append(' ');
            }
        } finally {
            reader.close();
        }
        String texto = contenido.toString().replaceAll("\\s+", " ");
        assertThat(texto).contains(
                "CONTRATO MUTUALISTA DE PREVISION FUNERARIA PARA MASCOTAS",
                "Ana Perez Lopez", contrato.getNumeroContrato(), contrato.getPlan().getNombre(),
                "Vigencia: Inicio: " + inicio + ". Fin: " + fin,
                "Precio total: $365.00 MXN. Mensualidad pactada: $30.42 MXN.",
                "DATOS DE LA MASCOTA", "Nombre: Luna", "Especie: Canina", "Raza: Labrador",
                "Peso: 18.250 kg", "Color/Caracteristicas: Negra con mancha blanca",
                "servicio anticipado requiere liquidar las aportaciones pendientes");
        assertThat(texto.toLowerCase(Locale.ROOT))
                .doesNotContain("enganche", "familia", "beneficiario", "10%", "inhumacion");
        verify(contratos).findById(10L);
        verifyNoInteractions(email);
    }
}