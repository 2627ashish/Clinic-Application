package com.chaula.dental.service.impl;

import com.chaula.dental.domain.BillItem;
import com.chaula.dental.domain.ClinicLocation;
import com.chaula.dental.domain.Patient;
import com.chaula.dental.domain.PrescriptionItem;
import com.chaula.dental.domain.TreatmentAndBill;
import com.chaula.dental.service.PdfGenerationService;
import com.lowagie.text.Anchor;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Service
public class OpenPdfGenerationServiceImpl implements PdfGenerationService {
    private static final Color BRAND = new Color(21, 94, 82);
    private static final Color SIDEBAR = new Color(242, 245, 245);
    private static final List<String> FACILITIES = List.of(
            "Computer Dental", "RVG X-Ray", "Root Canal Treatment", "Silver / Tooth-Coloured Filling",
            "Cleaning", "Extractions", "Dentures", "Crown & Bridge", "Orthodontics", "Jaw Surgery"
    );
    private final BaseFont baseFont;

    public OpenPdfGenerationServiceImpl() {
        try (var input = new ClassPathResource("fonts/NotoSansDevanagari.ttf").getInputStream()) {
            byte[] fontBytes = input.readAllBytes();
            this.baseFont = BaseFont.createFont(
                    "NotoSansDevanagari.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fontBytes, null);
        } catch (DocumentException | IOException exception) {
            throw new IllegalStateException("Unable to load the embedded Devanagari font", exception);
        }
    }

    @Override
    public byte[] generateVisitSummaryPdf(Patient patient, TreatmentAndBill treatment) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 30, 30, 26, 26);
        try {
            PdfWriter.getInstance(document, output);
            document.open();
            Font regular = font(9, Font.NORMAL, Color.DARK_GRAY);
            Font small = font(7, Font.NORMAL, Color.DARK_GRAY);
            addHeader(document);
            addPatientDetails(document, patient, regular);
            addClinicalLayout(document, treatment, regular, small);
            addFooter(document, small);
            document.close();
            return output.toByteArray();
        } catch (DocumentException exception) {
            throw new IllegalStateException("Unable to generate visit summary PDF", exception);
        } finally {
            if (document.isOpen()) document.close();
        }
    }

    private void addHeader(Document document) throws DocumentException {
        Font title = font(19, Font.BOLD, BRAND);
        Font doctor = font(11, Font.BOLD, Color.BLACK);
        Font credentials = font(8, Font.NORMAL, Color.DARK_GRAY);
        Paragraph heading = new Paragraph("CHAUHAN DENTAL CLINIC", title);
        heading.setAlignment(Element.ALIGN_CENTER);
        heading.setSpacingAfter(4);
        document.add(heading);
        Paragraph doctorLine = new Paragraph("DR. B.S. CHAUHAN (डॉ. बी. एस. चौहान)", doctor);
        doctorLine.setAlignment(Element.ALIGN_CENTER);
        document.add(doctorLine);
        Paragraph credentialsLine = new Paragraph(
                "B.D.S (Noida) · Expert Dental Surgeon · Reg : A-5651", credentials);
        credentialsLine.setAlignment(Element.ALIGN_CENTER);
        credentialsLine.setSpacingAfter(10);
        document.add(credentialsLine);
    }

    private void addPatientDetails(Document document, Patient patient, Font regular) throws DocumentException {
        PdfPTable patientTable = new PdfPTable(new float[]{2, 1, 1});
        patientTable.setWidthPercentage(100);
        addCell(patientTable, "Patient: " + safe(patient.getFullName()), regular, Color.WHITE);
        addCell(patientTable, "Phone: " + safe(patient.getPhoneNumber()), regular, Color.WHITE);
        addCell(patientTable, "Age/Sex: " + (patient.getAge() == null ? "—" : patient.getAge())
                + "/" + sexCode(patient.getGender()), regular, Color.WHITE);
        patientTable.setSpacingAfter(9);
        document.add(patientTable);
    }

    private void addClinicalLayout(
            Document document,
            TreatmentAndBill treatment,
            Font regular,
            Font small
    ) throws DocumentException {
        PdfPTable layout = new PdfPTable(new float[]{1, 3});
        layout.setWidthPercentage(100);
        layout.setSplitRows(true);
        PdfPCell sidebar = new PdfPCell();
        sidebar.setBackgroundColor(SIDEBAR);
        sidebar.setPadding(9);
        sidebar.setBorderColor(new Color(220, 226, 226));
        sidebar.addElement(section("FACILITIES", regular));
        for (String facility : FACILITIES) {
            Paragraph item = new Paragraph("• " + facility, small);
            item.setSpacingAfter(3);
            sidebar.addElement(item);
        }
        layout.addCell(sidebar);

        PdfPCell details = new PdfPCell();
        details.setPadding(9);
        details.setBorderColor(new Color(220, 226, 226));
        details.addElement(section("CHIEF COMPLAINTS / DIAGNOSIS", regular));
        details.addElement(new Paragraph(safe(treatment.getSymptomsAndDiagnosis()), regular));
        details.addElement(section("PROCEDURE DONE", regular));
        details.addElement(new Paragraph(safe(treatment.getProcedurePerformed()), regular));
        details.addElement(section("Rx MEDICINES", regular));
        details.addElement(medicineTable(treatment.getMedicines(), small));
        details.addElement(section("AFTERCARE GUIDELINES / देखभाल के निर्देश", regular));
        details.addElement(new Paragraph(safe(treatment.getAftercareGuidelines()), regular));
        details.addElement(section("BILL SUMMARY", regular));
        details.addElement(billTable(treatment, small));
        layout.addCell(details);
        document.add(layout);
    }

    private PdfPTable medicineTable(List<PrescriptionItem> medicines, Font regular) {
        PdfPTable table = new PdfPTable(new float[]{2, 1.2f, 1, 2});
        table.setWidthPercentage(100);
        addHeaderCell(table, "Medicine");
        addHeaderCell(table, "Dosage");
        addHeaderCell(table, "Duration");
        addHeaderCell(table, "Instructions");
        for (PrescriptionItem medicine : safeList(medicines)) {
            table.addCell(safe(medicine.name()));
            table.addCell(safe(medicine.dosage()));
            table.addCell(safe(medicine.duration()));
            table.addCell(safe(medicine.instructions()));
        }
        if (safeList(medicines).isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("No medicines prescribed", regular));
            empty.setColspan(4);
            table.addCell(empty);
        }
        return table;
    }

    private PdfPTable billTable(TreatmentAndBill treatment, Font small) {
        PdfPTable table = new PdfPTable(new float[]{3, 1});
        table.setWidthPercentage(100);
        addHeaderCell(table, "Item");
        addHeaderCell(table, "INR");
        for (BillItem item : safeList(treatment.getBillItems())) {
            table.addCell(safe(item.description()));
            table.addCell(money(item.amount()));
        }
        table.addCell(new Phrase("Total · Paid · Balance", small));
        BigDecimal total = amount(treatment.getTotalAmount());
        BigDecimal paid = amount(treatment.getPaidAmount());
        table.addCell(new Phrase(money(total) + " · " + money(paid) + " · " + money(total.subtract(paid)), small));
        return table;
    }

    private void addFooter(Document document, Font small) throws DocumentException {
        PdfPTable footer = new PdfPTable(1);
        footer.setWidthPercentage(100);
        footer.setSpacingBefore(9);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(BRAND);
        cell.setPadding(8);
        cell.setBorderColor(BRAND);
        cell.addElement(new Paragraph("CLINIC 1 · " + ClinicLocation.SEC_44.address()
                + " · " + ClinicLocation.SEC_44.hours(), font(7, Font.NORMAL, Color.WHITE)));
        cell.addElement(new Paragraph("CLINIC 2 · " + ClinicLocation.SEC_82.address()
                + " · " + ClinicLocation.SEC_82.hours(), font(7, Font.NORMAL, Color.WHITE)));
        Anchor clinicOneMap = new Anchor("Clinic 1 map directions", font(7, Font.UNDERLINE, Color.WHITE));
        clinicOneMap.setReference(ClinicLocation.SEC_44.directions());
        Anchor clinicTwoMap = new Anchor("Clinic 2 map directions", font(7, Font.UNDERLINE, Color.WHITE));
        clinicTwoMap.setReference(ClinicLocation.SEC_82.directions());
        Paragraph mapLine = new Paragraph();
        mapLine.add(clinicOneMap);
        mapLine.add(" · ");
        mapLine.add(clinicTwoMap);
        mapLine.setSpacingBefore(2);
        cell.addElement(mapLine);
        Paragraph tagline = new Paragraph("\"Your smile, our commitment\"",
                font(8, Font.BOLD, Color.WHITE));
        tagline.setAlignment(Element.ALIGN_CENTER);
        tagline.setSpacingBefore(4);
        cell.addElement(tagline);
        footer.addCell(cell);
        document.add(footer);
    }

    private Paragraph section(String title, Font font) {
        Paragraph paragraph = new Paragraph(title, font);
        paragraph.setSpacingBefore(6);
        paragraph.setSpacingAfter(3);
        return paragraph;
    }

    private void addCell(PdfPTable table, String text, Font font, Color background) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(background);
        cell.setPadding(5);
        cell.setBorderColor(new Color(220, 226, 226));
        table.addCell(cell);
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(7, Font.BOLD, Color.WHITE)));
        cell.setBackgroundColor(BRAND);
        cell.setPadding(4);
        table.addCell(cell);
    }

    private Font font(float size, int style, Color color) {
        return new Font(baseFont, size, style, color);
    }

    private String money(BigDecimal amount) {
        return "₹" + amount(amount).toPlainString();
    }

    private String sexCode(String gender) {
        if (gender == null || gender.isBlank()) return "—";
        return switch (gender.toLowerCase(java.util.Locale.ROOT)) {
            case "male" -> "M";
            case "female" -> "F";
            default -> "Other";
        };
    }

    private BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
