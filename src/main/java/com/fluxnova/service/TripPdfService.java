package com.fluxnova.service;

import com.fluxnova.model.Trip;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class TripPdfService {

    private static final float MARGIN = 60f;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float LINE_HEIGHT = 20f;

    public byte[] generate(Trip trip) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = PDRectangle.A4.getHeight() - MARGIN;

                // Header band
                cs.setNonStrokingColor(0.5f, 0.1f, 0.05f); // maroon
                cs.addRect(0, y - 10, PAGE_WIDTH, 50);
                cs.fill();

                // Header text
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 22);
                cs.setNonStrokingColor(1f, 1f, 1f);
                cs.newLineAtOffset(MARGIN, y + 10);
                cs.showText("FluxNova Trip Planner");
                cs.endText();

                y -= 70;

                // Trip title
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
                cs.setNonStrokingColor(0.5f, 0.1f, 0.05f);
                cs.newLineAtOffset(MARGIN, y);
                cs.showText(trip.getTitle());
                cs.endText();

                y -= 10;

                // Divider
                cs.setStrokingColor(0.85f, 0.4f, 0.1f);
                cs.setLineWidth(1.5f);
                cs.moveTo(MARGIN, y);
                cs.lineTo(PAGE_WIDTH - MARGIN, y);
                cs.stroke();

                y -= 25;

                // Details
                y = writeRow(cs, y, "Season",      trip.getSeason() != null ? trip.getSeason().name() : "—");
                y = writeRow(cs, y, "Status",      trip.getStatus() != null ? trip.getStatus().name() : "—");
                y = writeRow(cs, y, "Destination", trip.getDestination() != null ? trip.getDestination().getName() : "—");

                if (trip.getStartDate() != null) {
                    y = writeRow(cs, y, "Start Date", trip.getStartDate().toString());
                }
                if (trip.getEndDate() != null) {
                    y = writeRow(cs, y, "End Date", trip.getEndDate().toString());
                }

                if (trip.getNotes() != null && !trip.getNotes().isBlank()) {
                    y -= 10;
                    // Notes label
                    cs.beginText();
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11);
                    cs.setNonStrokingColor(0.3f, 0.3f, 0.3f);
                    cs.newLineAtOffset(MARGIN, y);
                    cs.showText("Preferences / Notes");
                    cs.endText();
                    y -= LINE_HEIGHT;

                    // Notes value — wrap long text naively by pipe separator
                    cs.setNonStrokingColor(0f, 0f, 0f);
                    for (String line : trip.getNotes().split("\\|")) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty()) continue;
                        cs.beginText();
                        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                        cs.newLineAtOffset(MARGIN + 10, y);
                        cs.showText("• " + trimmed);
                        cs.endText();
                        y -= LINE_HEIGHT;
                    }
                }

                // Footer
                String generated = "Generated " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE), 9);
                cs.setNonStrokingColor(0.6f, 0.6f, 0.6f);
                cs.newLineAtOffset(MARGIN, 30);
                cs.showText(generated);
                cs.endText();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate trip PDF", e);
        }
    }

    private float writeRow(PDPageContentStream cs, float y, String label, String value) throws IOException {
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11);
        cs.setNonStrokingColor(0.3f, 0.3f, 0.3f);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(label + ": ");
        cs.endText();

        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.newLineAtOffset(MARGIN + 100, y);
        cs.showText(value);
        cs.endText();

        return y - LINE_HEIGHT;
    }
}
