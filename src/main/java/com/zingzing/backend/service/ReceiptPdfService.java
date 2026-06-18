package com.zingzing.backend.service;

import com.zingzing.backend.entity.Order;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReceiptPdfService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneId.of("Asia/Karachi"));
    private static final Color BRAND_GREEN = new Color(0x2d, 0x6b, 0x4e);
    private static final Color LIGHT_GRAY = new Color(0xf4, 0xf7, 0xf5);

    public byte[] generate(Order order) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 50, 50, 60, 50);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titleFont = new Font(Font.HELVETICA, 22, Font.BOLD, BRAND_GREEN);
            Font headFont  = new Font(Font.HELVETICA, 10, Font.BOLD, BRAND_GREEN);
            Font bodyFont  = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
            Font smallFont = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY);

            // Platform name
            Paragraph title = new Paragraph("ChamCham", titleFont);
            title.setAlignment(Element.ALIGN_LEFT);
            doc.add(title);

            Paragraph subtitle = new Paragraph("Deal Receipt", new Font(Font.HELVETICA, 12, Font.NORMAL, Color.GRAY));
            subtitle.setSpacingAfter(20);
            doc.add(subtitle);

            // Separator line using table
            PdfPTable line = new PdfPTable(1);
            line.setWidthPercentage(100);
            PdfPCell lineCell = new PdfPCell();
            lineCell.setBackgroundColor(BRAND_GREEN);
            lineCell.setFixedHeight(2);
            lineCell.setBorder(Rectangle.NO_BORDER);
            line.addCell(lineCell);
            line.setSpacingAfter(20);
            doc.add(line);

            // Receipt metadata
            PdfPTable meta = new PdfPTable(2);
            meta.setWidthPercentage(100);
            meta.setWidths(new float[]{1, 1});
            addMetaRow(meta, "Order Number", order.getOrderNumber() != null ? order.getOrderNumber() : "—", headFont, bodyFont);
            addMetaRow(meta, "Date", order.getCreatedAt() != null ? DATE_FMT.format(order.getCreatedAt()) : "—", headFont, bodyFont);
            addMetaRow(meta, "Status", order.getStatus().name(), headFont, bodyFont);
            addMetaRow(meta, "Deal Type", order.getDealType().name(), headFont, bodyFont);
            meta.setSpacingAfter(20);
            doc.add(meta);

            // Parties
            PdfPTable parties = new PdfPTable(2);
            parties.setWidthPercentage(100);
            parties.setWidths(new float[]{1, 1});

            PdfPCell brandCell = new PdfPCell();
            brandCell.setBorder(Rectangle.NO_BORDER);
            brandCell.setBackgroundColor(LIGHT_GRAY);
            brandCell.setPadding(12);
            brandCell.addElement(new Paragraph("BRAND", headFont));
            brandCell.addElement(new Paragraph(order.getBrand().getDisplayName() != null ? order.getBrand().getDisplayName() : "—", bodyFont));
            parties.addCell(brandCell);

            PdfPCell creatorCell = new PdfPCell();
            creatorCell.setBorder(Rectangle.NO_BORDER);
            creatorCell.setBackgroundColor(LIGHT_GRAY);
            creatorCell.setPadding(12);
            creatorCell.addElement(new Paragraph("CREATOR", headFont));
            creatorCell.addElement(new Paragraph(order.getCreator().getName() != null ? order.getCreator().getName() : "—", bodyFont));
            parties.addCell(creatorCell);
            parties.setSpacingAfter(20);
            doc.add(parties);

            // Package
            Paragraph pkg = new Paragraph("Package: " + order.getServicePackage().getTitle(), headFont);
            pkg.setSpacingAfter(5);
            doc.add(pkg);

            List<String> deliverableNames = order.getDeliverables().stream()
                    .map(d -> d.getName() != null ? d.getName() : "Deliverable")
                    .collect(Collectors.toList());
            if (!deliverableNames.isEmpty()) {
                Paragraph deliverables = new Paragraph("Deliverables: " + String.join(", ", deliverableNames), bodyFont);
                deliverables.setSpacingAfter(20);
                doc.add(deliverables);
            } else {
                doc.add(new Paragraph(" "));
            }

            // Financials
            PdfPTable financials = new PdfPTable(2);
            financials.setWidthPercentage(60);
            financials.setHorizontalAlignment(Element.ALIGN_LEFT);
            financials.setWidths(new float[]{2, 1});

            if (order.getAmount() != null && order.getAmount() > 0) {
                addFinancialRow(financials, "Order Amount", "PKR " + order.getAmount(), headFont, bodyFont);
            }
            if (order.getDeadlineDate() != null) {
                addFinancialRow(financials, "Deadline", DATE_FMT.format(order.getDeadlineDate()), headFont, bodyFont);
            }
            financials.setSpacingAfter(30);
            doc.add(financials);

            // Footer
            Paragraph footer = new Paragraph(
                "This receipt was generated by ChamCham — Pakistan's Creator Marketplace.\n" +
                "For support: support@chamcham.pk", smallFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            doc.add(footer);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate receipt PDF", e);
        }
    }

    private void addMetaRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell lCell = new PdfPCell(new Phrase(label, labelFont));
        lCell.setBorder(Rectangle.NO_BORDER);
        lCell.setPaddingBottom(6);
        PdfPCell vCell = new PdfPCell(new Phrase(value, valueFont));
        vCell.setBorder(Rectangle.NO_BORDER);
        vCell.setPaddingBottom(6);
        table.addCell(lCell);
        table.addCell(vCell);
    }

    private void addFinancialRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell lCell = new PdfPCell(new Phrase(label, labelFont));
        lCell.setBorder(Rectangle.BOTTOM);
        lCell.setBorderColor(new Color(0xd1, 0xdd, 0xd6));
        lCell.setPadding(6);
        PdfPCell vCell = new PdfPCell(new Phrase(value, valueFont));
        vCell.setBorder(Rectangle.BOTTOM);
        vCell.setBorderColor(new Color(0xd1, 0xdd, 0xd6));
        vCell.setPadding(6);
        vCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(lCell);
        table.addCell(vCell);
    }
}
