package com.chaula.dental.service.impl;

import com.chaula.dental.domain.ClinicLocation;
import com.chaula.dental.domain.Patient;
import com.chaula.dental.domain.PrescriptionItem;
import com.chaula.dental.domain.TreatmentAndBill;
import com.lowagie.text.Anchor;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Component
public class PrescriptionPdfGenerator {
    private static final List<String> FACILITIES = List.of(
            "Computer Dental", "RVG X-Ray", "RCT", "Implants", "Braces", "Dentures"
    );
    private static final String JUSTDIAL_URL = "https://jsdl.in/DT-23XT6URJ";
    private final ClinicPdfResources resources;
    private final PrescriptionAftercarePage aftercarePage;

    public PrescriptionPdfGenerator(ClinicPdfResources resources, PrescriptionAftercarePage aftercarePage) {
        this.resources = resources;
        this.aftercarePage = aftercarePage;
    }

    public byte[] generate(Patient patient, TreatmentAndBill treatment) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 30, 30, 26, 26);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, output);
            writer.setPageEvent(new ToothWatermark());
            document.open();
            addPrescriptionPage(document, patient, treatment);
            document.newPage();
            aftercarePage.addPage(document, treatment);
            document.close();
            return output.toByteArray();
        } catch (DocumentException exception) {
            throw new IllegalStateException("Unable to generate prescription PDF", exception);
        } finally {
            if (document.isOpen()) document.close();
        }
    }

    private void addPrescriptionPage(Document document, Patient patient, TreatmentAndBill treatment)
            throws DocumentException {
        addTitle(document, "CHAUHAN DENTAL CLINIC", 19, ClinicPdfSupport.BRAND);
        addTitle(document, "DR. B.S. CHAUHAN (डॉ. बी. एस. चौहान)", 11, Color.BLACK);
        Paragraph credentials = new Paragraph(
                "B.D.S (Noida) · Expert Dental Surgeon · Reg: A-5651",
                resources.font(8, Font.NORMAL, Color.DARK_GRAY));
        credentials.setAlignment(Element.ALIGN_CENTER);
        credentials.setSpacingAfter(10);
        document.add(credentials);
        addPatientDetails(document, patient, treatment);

        PdfPTable content = new PdfPTable(new float[]{1.1f, 3.9f});
        content.setWidthPercentage(100);
        content.setSplitRows(true);
        content.addCell(facilitiesSidebar());
        content.addCell(clinicalDetails(treatment));
        document.add(content);
        addPrescriptionFooter(document);
    }

    private void addPatientDetails(Document document, Patient patient, TreatmentAndBill treatment)
            throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{2.1f, 1.4f, 1.1f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(10);
        addInfoCell(table, "Patient", ClinicPdfSupport.safe(patient.getFullName()));
        addInfoCell(table, "Age / Sex", ClinicPdfSupport.ageAndSex(patient));
        addInfoCell(table, "Date", ClinicPdfSupport.billDate(treatment).toString());
        document.add(table);
    }

    private PdfPCell facilitiesSidebar() {
        PdfPCell sidebar = new PdfPCell();
        sidebar.setBackgroundColor(ClinicPdfSupport.PALE);
        sidebar.setPadding(9);
        sidebar.setBorderColor(new Color(220, 226, 226));
        sidebar.addElement(ClinicPdfSupport.section(
                "FACILITIES", resources.font(9, Font.BOLD, ClinicPdfSupport.BRAND)));
        for (String facility : FACILITIES) {
            Paragraph item = new Paragraph("• " + facility, resources.font(7, Font.NORMAL, Color.DARK_GRAY));
            item.setSpacingAfter(5);
            sidebar.addElement(item);
        }
        return sidebar;
    }

    private PdfPCell clinicalDetails(TreatmentAndBill treatment) {
        PdfPCell details = new PdfPCell();
        details.setPadding(9);
        details.setBorderColor(new Color(220, 226, 226));
        Font regular = resources.font(8, Font.NORMAL, Color.DARK_GRAY);
        Font heading = resources.font(9, Font.BOLD, ClinicPdfSupport.BRAND);
        details.addElement(ClinicPdfSupport.section("CHIEF COMPLAINTS / CLINICAL FINDINGS", heading));
        details.addElement(new Paragraph(ClinicPdfSupport.safe(treatment.getSymptomsAndDiagnosis()), regular));
        details.addElement(ClinicPdfSupport.section("PROCEDURE PERFORMED", heading));
        details.addElement(new Paragraph(ClinicPdfSupport.safe(treatment.getProcedurePerformed()), regular));
        details.addElement(ClinicPdfSupport.section("Rx MEDICINES", heading));
        details.addElement(medicineTable(treatment.getMedicines()));
        return details;
    }

    private PdfPTable medicineTable(List<PrescriptionItem> medicines) {
        PdfPTable table = new PdfPTable(new float[]{1.7f, 1.1f, 0.9f, 1.7f});
        table.setWidthPercentage(100);
        addHeaderCell(table, "Medicine");
        addHeaderCell(table, "Dosage");
        addHeaderCell(table, "Duration");
        addHeaderCell(table, "Instructions");
        List<PrescriptionItem> rows = medicines == null ? List.of() : medicines;
        if (rows.isEmpty()) {
            PdfPCell empty = ClinicPdfSupport.textCell(
                    "No medicines prescribed", resources.font(8, Font.NORMAL, Color.DARK_GRAY), Color.WHITE);
            empty.setColspan(4);
            table.addCell(empty);
        } else {
            for (PrescriptionItem medicine : rows) {
                addBodyCell(table, ClinicPdfSupport.safe(medicine.name()));
                addBodyCell(table, ClinicPdfSupport.safe(medicine.dosage()));
                addBodyCell(table, ClinicPdfSupport.safe(medicine.duration()));
                addBodyCell(table, ClinicPdfSupport.safe(medicine.instructions()));
            }
        }
        return table;
    }

    private void addPrescriptionFooter(Document document) throws DocumentException {
        PdfPTable footer = new PdfPTable(1);
        footer.setWidthPercentage(100);
        footer.setSpacingBefore(9);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(ClinicPdfSupport.BRAND);
        cell.setPadding(7);
        cell.setBorderColor(ClinicPdfSupport.BRAND);
        Font whiteSmall = resources.font(7, Font.NORMAL, Color.WHITE);
        cell.addElement(new Paragraph("Clinic 1 · " + ClinicLocation.SEC_44.address(), whiteSmall));
        cell.addElement(new Paragraph("Clinic 2 · " + ClinicLocation.SEC_82.address(), whiteSmall));
        Anchor link = new Anchor("Rate our clinic on JustDial", resources.font(7, Font.UNDERLINE, Color.WHITE));
        link.setReference(JUSTDIAL_URL);
        Paragraph linkLine = new Paragraph(link);
        linkLine.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(linkLine);
        footer.addCell(cell);
        document.add(footer);
    }

    private void addInfoCell(PdfPTable table, String label, String value) {
        Paragraph text = new Paragraph();
        text.add(new Phrase(label + ": ", resources.font(7, Font.BOLD, ClinicPdfSupport.BRAND)));
        text.add(new Phrase(value, resources.font(8, Font.NORMAL, Color.DARK_GRAY)));
        PdfPCell cell = new PdfPCell(text);
        cell.setPadding(6);
        cell.setBorderColor(new Color(220, 226, 226));
        table.addCell(cell);
    }

    private void addHeaderCell(PdfPTable table, String text) {
        table.addCell(ClinicPdfSupport.headerCell(
                text, resources.font(7, Font.BOLD, Color.WHITE), ClinicPdfSupport.BRAND));
    }

    private void addBodyCell(PdfPTable table, String text) {
        table.addCell(ClinicPdfSupport.textCell(
                text, resources.font(7, Font.NORMAL, Color.DARK_GRAY), Color.WHITE));
    }

    private void addTitle(Document document, String text, float size, Color color) throws DocumentException {
        Paragraph title = new Paragraph(text, resources.font(size, Font.BOLD, color));
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(3);
        document.add(title);
    }

    private final class ToothWatermark extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContentUnder();
            PdfGState state = new PdfGState();
            state.setStrokeOpacity(0.07f);
            canvas.saveState();
            canvas.setGState(state);
            canvas.setColorStroke(ClinicPdfSupport.BRAND);
            canvas.setLineWidth(5);
            float centerX = PageSize.A4.getWidth() / 2;
            float centerY = PageSize.A4.getHeight() / 2;
            canvas.moveTo(centerX - 30, centerY + 36);
            canvas.curveTo(centerX - 58, centerY + 53, centerX - 72, centerY + 20, centerX - 62, centerY - 5);
            canvas.curveTo(centerX - 55, centerY - 20, centerX - 48, centerY - 47, centerX - 35, centerY - 46);
            canvas.curveTo(centerX - 24, centerY - 45, centerX - 20, centerY - 22, centerX - 10, centerY - 22);
            canvas.curveTo(centerX, centerY - 22, centerX + 5, centerY - 45, centerX + 16, centerY - 46);
            canvas.curveTo(centerX + 31, centerY - 47, centerX + 36, centerY - 20, centerX + 43, centerY - 5);
            canvas.curveTo(centerX + 53, centerY + 20, centerX + 39, centerY + 53, centerX + 11, centerY + 36);
            canvas.curveTo(centerX - 1, centerY + 29, centerX - 17, centerY + 29, centerX - 30, centerY + 36);
            canvas.stroke();
            canvas.restoreState();
        }
    }
}
