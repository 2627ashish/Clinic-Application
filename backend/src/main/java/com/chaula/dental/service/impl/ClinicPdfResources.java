package com.chaula.dental.service.impl;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.IOException;

@Component
public class ClinicPdfResources {
    private final BaseFont baseFont;

    public ClinicPdfResources() {
        try (var input = new ClassPathResource("fonts/NotoSansDevanagari.ttf").getInputStream()) {
            byte[] fontBytes = input.readAllBytes();
            baseFont = BaseFont.createFont(
                    "NotoSansDevanagari.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fontBytes, null);
        } catch (DocumentException | IOException exception) {
            throw new IllegalStateException("Unable to load the embedded Devanagari font", exception);
        }
    }

    public Font font(float size, int style, Color color) {
        return new Font(baseFont, size, style, color);
    }
}
