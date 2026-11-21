package com.chaula.dental.service.impl;

import com.chaula.dental.domain.Appointment;
import com.chaula.dental.domain.BillItem;
import com.chaula.dental.domain.ClinicLocation;
import com.chaula.dental.domain.Patient;
import com.chaula.dental.domain.TreatmentAndBill;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class OutpatientBillPdfGenerator {
    private static final String HSN_DENTAL_SERVICES = "999312";
    private final ClinicPdfResources resources;

    public OutpatientBillPdfGenerator(ClinicPdfResources resources) {
        this.resources = resources;
    }

    public byte[] generate(Patient patient, TreatmentAndBill treatment, Appointment appointment) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 25, 25, 22, 24);
        try {
            PdfWriter.getInstance(document, output);
            document.open();
            addClinicHeader(document);
            addBillBanner(document);
            addPatientAndPayerDetails(document, patient, treatment, appointment);
            addItemizedBilling(document, treatment);
            addTotals(document, treatment);
            addSignatureFooter(document);
            document.close();
            return output.toByteArray();
        } catch (DocumentException exception) {
            throw new IllegalStateException("Unable to generate outpatient bill PDF", exception);
        } finally {
            if (document.isOpen()) document.close();
        }
    }

    private void addClinicHeader(Document document) throws DocumentException {
        PdfPTable header = new PdfPTable(new float[]{4.2f, 1.3f});
        header.setWidthPercentage(100);
        PdfPCell clinic = new PdfPCell();
        clinic.setPadding(8);
        clinic.setBorderColor(new Color(220, 226, 226));
        clinic.addElement(new Paragraph(
                "CHAUHAN DENTAL CLINIC", resources.font(16, Font.BOLD, ClinicPdfSupport.BRAND)));
        clinic.addElement(new Paragraph(
                ClinicLocation.SEC_44.address(), resources.font(7, Font.NORMAL, Color.DARK_GRAY)));
        clinic.addElement(new Paragraph(
                ClinicLocation.SEC_82.address(), resources.font(7, Font.NORMAL, Color.DARK_GRAY)));
        clinic.addElement(new Paragraph(
                "Tel: +91-98107-64715 / +91-97177-73179",
                resources.font(8, Font.BOLD, Color.DARK_GRAY)));
        header.addCell(clinic);

        PdfPCell qrPlaceholder = new PdfPCell();
        qrPlaceholder.setMinimumHeight(78);
        qrPlaceholder.setPadding(6);
        qrPlaceholder.setHorizontalAlignment(Element.ALIGN_CENTER);
        qrPlaceholder.setVerticalAlignment(Element.ALIGN_MIDDLE);
        qrPlaceholder.setBackgroundColor(new Color(247, 247, 252));
        qrPlaceholder.setBorderColor(new Color(177, 165, 205));
        Paragraph qrLabel = new Paragraph("UPI SCAN & PAY",
                resources.font(8, Font.BOLD, ClinicPdfSupport.PURPLE));
        qrLabel.setAlignment(Element.ALIGN_CENTER);
        qrPlaceholder.addElement(qrLabel);
        Paragraph notConfigured = new Paragraph("QR not configured",
                resources.font(7, Font.NORMAL, Color.DARK_GRAY));
        notConfigured.setAlignment(Element.ALIGN_CENTER);
        qrPlaceholder.addElement(notConfigured);
        header.addCell(qrPlaceholder);
        document.add(header);
    }

    private void addBillBanner(Document document) throws DocumentException {
        PdfPTable banner = new PdfPTable(1);
        banner.setWidthPercentage(100);
        banner.setSpacingBefore(7);
        banner.setSpacingAfter(8);
        PdfPCell cell = ClinicPdfSupport.headerCell(
                "OUT PATIENT BILL", resources.font(12, Font.BOLD, Color.WHITE), ClinicPdfSupport.PURPLE);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(8);
        banner.addCell(cell);
        document.add(banner);
    }

    private void addPatientAndPayerDetails(
            Document document,
            Patient patient,
            TreatmentAndBill treatment,
            Appointment appointment
    ) throws DocumentException {
        PdfPTable columns = new PdfPTable(2);
        columns.setWidthPercentage(100);
        columns.setSpacingAfter(8);
        PdfPCell patientInfo = informationBlock(List.of(
                new Field("Patient Name", ClinicPdfSupport.safe(patient.getFullName())),
                new Field("UHID / Patient ID", ClinicPdfSupport.patientNumber(patient)),
                new Field("Contact No", ClinicPdfSupport.safe(patient.getPhoneNumber())),
                new Field("Age / Sex", ClinicPdfSupport.ageAndSex(patient)),
                new Field("Bill Date", formattedDate(ClinicPdfSupport.billDate(treatment))),
                new Field("Print Date", formattedDate(LocalDate.now())),
                new Field("Patient Address", "Not recorded")
        ));
        PdfPCell payerInfo = informationBlock(List.of(
                new Field("Primary Doctor", "Dr. B.S. Chauhan, B.D.S"),
                new Field("Referring Doctor", "Self"),
                new Field("Payor Name", "CASH / UPI"),
                new Field("Payment Status", paymentStatus(treatment)),
                new Field("Clinic", ClinicLocation.defaultLocation(
                        appointment == null ? null : appointment.getClinicLocation()).label())
        ));
        columns.addCell(patientInfo);
        columns.addCell(payerInfo);
        document.add(columns);
    }

    private PdfPCell informationBlock(List<Field> fields) {
        PdfPCell block = new PdfPCell();
        block.setPadding(6);
        block.setBorderColor(new Color(220, 226, 226));
        for (Field field : fields) {
            Paragraph line = new Paragraph();
            line.setSpacingAfter(3);
            line.add(new Phrase(field.label() + ": ", resources.font(7, Font.BOLD, ClinicPdfSupport.BRAND)));
            line.add(new Phrase(field.value(), resources.font(7, Font.NORMAL, Color.DARK_GRAY)));
            block.addElement(line);
        }
        return block;
    }

    private void addItemizedBilling(Document document, TreatmentAndBill treatment) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{0.45f, 2.4f, 0.65f, 0.55f, 1.0f, 0.75f, 1.15f});
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.setSplitRows(true);
        for (String title : List.of(
                "S.No", "Particulars", "HSN Code", "Qty", "Service Amount", "Discount", "Net Gross Amount")) {
            PdfPCell heading = ClinicPdfSupport.headerCell(
                    title, resources.font(6, Font.BOLD, Color.WHITE), ClinicPdfSupport.BRAND);
            heading.setPadding(4);
            table.addCell(heading);
        }
        List<BillItem> items = ClinicPdfSupport.billItems(treatment);
        for (int index = 0; index < items.size(); index++) {
            BillItem item = items.get(index);
            BigDecimal amount = ClinicPdfSupport.amount(item.amount());
            addBodyCell(table, String.valueOf(index + 1), Element.ALIGN_CENTER);
            addBodyCell(table, ClinicPdfSupport.safe(item.description()), Element.ALIGN_LEFT);
            addBodyCell(table, HSN_DENTAL_SERVICES, Element.ALIGN_CENTER);
            addBodyCell(table, "1.00", Element.ALIGN_RIGHT);
            addBodyCell(table, ClinicPdfSupport.money(amount), Element.ALIGN_RIGHT);
            addBodyCell(table, ClinicPdfSupport.money(BigDecimal.ZERO), Element.ALIGN_RIGHT);
            addBodyCell(table, ClinicPdfSupport.money(amount), Element.ALIGN_RIGHT);
        }
        document.add(table);
    }

    private void addTotals(Document document, TreatmentAndBill treatment) throws DocumentException {
        BigDecimal itemTotal = ClinicPdfSupport.billItems(treatment).stream()
                .map(item -> ClinicPdfSupport.amount(item.amount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = treatment.getTotalAmount() == null
                ? itemTotal : ClinicPdfSupport.amount(treatment.getTotalAmount());
        BigDecimal paid = ClinicPdfSupport.amount(treatment.getPaidAmount()).min(total);
        BigDecimal payable = total.subtract(paid).max(BigDecimal.ZERO);
        BigDecimal roundOff = total.setScale(0, RoundingMode.HALF_UP).subtract(total);

        PdfPTable totals = new PdfPTable(new float[]{3, 1});
        totals.setWidthPercentage(55);
        totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totals.setSpacingBefore(8);
        addTotalRow(totals, "Total Amount", total);
        addTotalRow(totals, "Patient Share", total);
        addTotalRow(totals, "Paid by Patient", paid);
        addTotalRow(totals, "Net Patient Payable", payable);
        addTotalRow(totals, "Bill Round Off Amount", roundOff);
        document.add(totals);
        Paragraph words = new Paragraph(
                "Rupees in words: " + ClinicPdfSupport.rupeesInWords(total),
                resources.font(8, Font.BOLD, ClinicPdfSupport.BRAND));
        words.setSpacingBefore(7);
        document.add(words);
    }

    private void addTotalRow(PdfPTable table, String label, BigDecimal value) {
        table.addCell(ClinicPdfSupport.textCell(
                label, resources.font(8, Font.NORMAL, Color.DARK_GRAY), ClinicPdfSupport.PALE));
        PdfPCell amount = ClinicPdfSupport.textCell(
                ClinicPdfSupport.money(value), resources.font(8, Font.BOLD, Color.DARK_GRAY), Color.WHITE);
        amount.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(amount);
    }

    private void addSignatureFooter(Document document) throws DocumentException {
        PdfPTable footer = new PdfPTable(2);
        footer.setWidthPercentage(100);
        footer.setSpacingBefore(28);
        PdfPCell cashier = ClinicPdfSupport.textCell(
                "Cashier / Manager Signature: ____________________", resources.font(8, Font.NORMAL, Color.DARK_GRAY),
                Color.WHITE);
        PdfPCell doctor = ClinicPdfSupport.textCell(
                "Authorized Signature: ____________________", resources.font(8, Font.NORMAL, Color.DARK_GRAY),
                Color.WHITE);
        cashier.setBorderColor(Color.WHITE);
        doctor.setBorderColor(Color.WHITE);
        doctor.setHorizontalAlignment(Element.ALIGN_RIGHT);
        footer.addCell(cashier);
        footer.addCell(doctor);
        document.add(footer);
        Paragraph note = new Paragraph(
                "This is a computer-generated invoice. Patient address and UPI QR are not configured in the clinic record.",
                resources.font(7, Font.ITALIC, Color.GRAY));
        note.setAlignment(Element.ALIGN_CENTER);
        note.setSpacingBefore(5);
        document.add(note);
    }

    private void addBodyCell(PdfPTable table, String value, int alignment) {
        PdfPCell cell = ClinicPdfSupport.textCell(
                value, resources.font(7, Font.NORMAL, Color.DARK_GRAY), Color.WHITE);
        cell.setHorizontalAlignment(alignment);
        table.addCell(cell);
    }

    private String paymentStatus(TreatmentAndBill treatment) {
        BigDecimal total = treatment.getTotalAmount() == null
                ? ClinicPdfSupport.billItems(treatment).stream()
                    .map(item -> ClinicPdfSupport.amount(item.amount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                : ClinicPdfSupport.amount(treatment.getTotalAmount());
        BigDecimal paid = ClinicPdfSupport.amount(treatment.getPaidAmount());
        if (paid.compareTo(total) >= 0) return "Paid";
        return paid.signum() > 0 ? "Partially Paid" : "Unpaid";
    }

    private String formattedDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
    }

    private record Field(String label, String value) {
    }
}
