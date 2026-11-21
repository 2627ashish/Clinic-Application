package com.chaula.dental.service.impl;

import com.chaula.dental.domain.TreatmentAndBill;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.util.List;
import java.util.Locale;

@Component
public class PrescriptionAftercarePage {
    private final ClinicPdfResources resources;

    public PrescriptionAftercarePage(ClinicPdfResources resources) {
        this.resources = resources;
    }

    public void addPage(Document document, TreatmentAndBill treatment) throws DocumentException {
        addTitle(document, "CHAUHAN DENTAL CLINIC", 17, ClinicPdfSupport.BRAND);
        addTitle(document, "POST-TREATMENT CARE & PRECAUTIONS", 12, Color.DARK_GRAY);
        Paragraph subtitle = new Paragraph(
                "उपचार के बाद सावधानियां",
                resources.font(10, Font.NORMAL, ClinicPdfSupport.BRAND));
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(10);
        document.add(subtitle);

        CarePlan carePlan = carePlan(treatment);
        Paragraph guideTitle = new Paragraph(
                carePlan.title(), resources.font(11, Font.BOLD, ClinicPdfSupport.BRAND));
        guideTitle.setSpacingAfter(7);
        document.add(guideTitle);
        if (treatment.getAftercareGuidelines() != null && !treatment.getAftercareGuidelines().isBlank()) {
            Paragraph personalized = new Paragraph(
                    "Your dentist's instructions: " + treatment.getAftercareGuidelines(),
                    resources.font(8, Font.NORMAL, Color.DARK_GRAY));
            personalized.setSpacingAfter(8);
            document.add(personalized);
        }
        addCareTable(document, "DO'S / क्या करें", carePlan.dos(), ClinicPdfSupport.GREEN);
        addCareTable(document, "DON'TS / क्या न करें", carePlan.donts(), ClinicPdfSupport.RED);
        addDietAndWarningTables(document, carePlan);
        Paragraph contact = new Paragraph(
                "For urgent concerns, contact Dr. Chauhan: +91-98107-64715",
                resources.font(10, Font.BOLD, ClinicPdfSupport.BRAND));
        contact.setSpacingBefore(10);
        document.add(contact);
    }

    private void addCareTable(Document document, String title, List<String> items, Color accent)
            throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingAfter(8);
        table.addCell(ClinicPdfSupport.headerCell(
                title, resources.font(9, Font.BOLD, Color.WHITE), accent));
        for (String item : items) {
            table.addCell(checklistCell(item, accent, accent.equals(ClinicPdfSupport.GREEN)));
        }
        document.add(table);
    }

    private PdfPCell checklistCell(String item, Color accent, boolean check) {
        PdfPTable row = new PdfPTable(new float[]{0.15f, 1});
        row.setWidthPercentage(100);
        PdfPCell marker = new PdfPCell(new Phrase(""));
        marker.setBorder(Rectangle.NO_BORDER);
        marker.setFixedHeight(20);
        marker.setCellEvent((cell, position, canvases) -> {
            PdfContentByte canvas = canvases[PdfPTable.LINECANVAS];
            float left = position.getLeft();
            float bottom = position.getBottom();
            float top = position.getTop();
            float centerY = (bottom + top) / 2;
            canvas.saveState();
            canvas.setColorStroke(accent);
            canvas.setLineWidth(1.8f);
            if (check) {
                canvas.moveTo(left + 5, centerY);
                canvas.lineTo(left + 8, centerY - 3);
                canvas.lineTo(left + 14, centerY + 5);
            } else {
                canvas.moveTo(left + 5, centerY - 4);
                canvas.lineTo(left + 13, centerY + 4);
                canvas.moveTo(left + 13, centerY - 4);
                canvas.lineTo(left + 5, centerY + 4);
            }
            canvas.stroke();
            canvas.restoreState();
        });
        row.addCell(marker);
        PdfPCell text = new PdfPCell(new Phrase(item, resources.font(8, Font.NORMAL, Color.DARK_GRAY)));
        text.setBorder(Rectangle.NO_BORDER);
        text.setPadding(4);
        row.addCell(text);
        PdfPCell cell = new PdfPCell(row);
        cell.setBorderColor(new Color(220, 226, 226));
        cell.setBackgroundColor(Color.WHITE);
        return cell;
    }

    private void addDietAndWarningTables(Document document, CarePlan carePlan) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{1, 1});
        table.setWidthPercentage(100);
        PdfPCell diet = new PdfPCell();
        diet.setPadding(8);
        diet.addElement(ClinicPdfSupport.section(
                "RECOMMENDED SOFT DIET", resources.font(9, Font.BOLD, ClinicPdfSupport.GREEN)));
        diet.addElement(new Paragraph(carePlan.diet(), resources.font(8, Font.NORMAL, Color.DARK_GRAY)));
        table.addCell(diet);
        PdfPCell warning = new PdfPCell();
        warning.setPadding(8);
        warning.setBackgroundColor(new Color(255, 246, 246));
        warning.addElement(ClinicPdfSupport.section(
                "CONTACT THE CLINIC IF", resources.font(9, Font.BOLD, ClinicPdfSupport.RED)));
        for (String item : List.of("Bleeding does not stop", "Fever or pus develops", "Sudden severe pain after 3–4 days")) {
            warning.addElement(new Paragraph("• " + item, resources.font(8, Font.NORMAL, Color.DARK_GRAY)));
        }
        table.addCell(warning);
        document.add(table);
    }

    private CarePlan carePlan(TreatmentAndBill treatment) {
        String context = (value(treatment.getProcedurePerformed()) + " "
                + value(treatment.getSymptomsAndDiagnosis())).toLowerCase(Locale.ROOT);
        if (context.contains("extract") || context.contains("impaction") || context.contains("wisdom")) {
            return new CarePlan(
                    "Extraction Care · दाँत निकालने के बाद सावधानियां",
                    List.of("Keep gauze pressed for 60 minutes.", "Use an ice pack externally for 15–20 minutes at a time.",
                            "Choose cool, soft foods during the first day.", "After 24 hours, rinse gently with warm salt water if bleeding has stopped."),
                    List.of("Do not spit or rinse during the first 24 hours.", "Do not use a straw.",
                            "Do not smoke or disturb the socket clot.", "Avoid hot, hard, or crunchy foods at first."),
                    "Soft khichdi, yogurt, idli, porridge, and soup; follow your dentist's advice about when to resume normal food."
            );
        }
        if (context.contains("root canal") || context.matches(".*\\brct\\b.*")) {
            return new CarePlan(
                    "Root Canal Care · रूट कैनाल के बाद देखभाल",
                    List.of("Eat only after numbness has worn off.", "Take prescribed medicines as directed.",
                            "Arrange the recommended permanent crown on time.", "Attend the advised follow-up visit."),
                    List.of("Do not chew hard foods on the treated side before the crown is placed.",
                            "Avoid ice, areca nut (supari), and other very hard foods.", "Do not disturb a temporary filling with your tongue."),
                    "Soft khichdi, yogurt, idli, porridge, and vegetable soup until comfortable to chew."
            );
        }
        return new CarePlan(
                "General Dental Care · सामान्य देखभाल",
                List.of("Follow the treatment-specific instructions from your dentist.",
                        "Take medicines only as prescribed.", "Keep the treated area clean.", "Attend the advised follow-up."),
                List.of("Do not change or stop prescribed medicines without advice.",
                        "Avoid foods that worsen discomfort.", "Do not ignore increasing pain, swelling, or fever."),
                "Choose soft, nourishing foods if chewing is uncomfortable and stay hydrated."
        );
    }

    private void addTitle(Document document, String text, float size, Color color) throws DocumentException {
        Paragraph title = new Paragraph(text, resources.font(size, Font.BOLD, color));
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(3);
        document.add(title);
    }

    private String value(String text) {
        return text == null ? "" : text;
    }

    private record CarePlan(String title, List<String> dos, List<String> donts, String diet) {
    }
}
