package com.example.chicke_booking.service;

import com.example.chicke_booking.model.entity.Booking;
import com.example.chicke_booking.model.entity.BookingItem;
import com.example.chicke_booking.model.entity.Chick;
import com.example.chicke_booking.model.entity.Location;
import com.example.chicke_booking.model.entity.User;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfService {

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 24, Font.BOLD, new Color(34, 139, 34));
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 14, Font.BOLD, Color.WHITE);
    private static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 12, Font.BOLD, Color.DARK_GRAY);
    private static final Font NORMAL_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);
    private static final Font SMALL_FONT = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY);
    private static final Font BOLD_FONT = new Font(Font.HELVETICA, 10, Font.BOLD, Color.BLACK);
    private static final Font TOTAL_FONT = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(34, 139, 34));
    
    private static final Color PRIMARY_COLOR = new Color(34, 139, 34);  // Green
    private static final Color LIGHT_GRAY = new Color(245, 245, 245);
    
    /**
     * Generate a PDF receipt for a booking
     */
    public byte[] generateReceipt(Booking booking) throws DocumentException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        
        try {
            PdfWriter.getInstance(document, baos);
            document.open();
            
            // Header with company info
            addReceiptHeader(document, booking);
            
            // Booking/Customer Info
            addCustomerInfo(document, booking);
            
            // Order Items Table
            addOrderItems(document, booking);
            
            // Total Section
            addTotalSection(document, booking);
            
            // Footer
            addReceiptFooter(document);
            
            document.close();
        } catch (Exception e) {
            throw new DocumentException("Error generating PDF receipt: " + e.getMessage());
        }
        
        return baos.toByteArray();
    }
    
    private void addReceiptHeader(Document document, Booking booking) throws DocumentException {
        // Company Title
        Paragraph title = new Paragraph("CHICK BOOKING", TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        
        Paragraph subtitle = new Paragraph("Order Receipt", SUBTITLE_FONT);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(10);
        document.add(subtitle);
        
        // Receipt Number and Date
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setSpacingBefore(20);
        headerTable.setSpacingAfter(20);
        
        PdfPCell receiptCell = new PdfPCell();
        receiptCell.setBorder(Rectangle.NO_BORDER);
        receiptCell.addElement(new Paragraph("Receipt #: " + booking.getReceiptNumber(), BOLD_FONT));
        receiptCell.addElement(new Paragraph("Booking ID: #" + booking.getId(), SMALL_FONT));
        headerTable.addCell(receiptCell);
        
        PdfPCell dateCell = new PdfPCell();
        dateCell.setBorder(Rectangle.NO_BORDER);
        dateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");
        dateCell.addElement(new Paragraph("Date: " + booking.getCreatedAt().format(formatter), NORMAL_FONT));
        dateCell.addElement(new Paragraph("Status: " + booking.getStatus().name(), SMALL_FONT));
        headerTable.addCell(dateCell);
        
        document.add(headerTable);
        
        // Divider line using a table
        addDividerLine(document, PRIMARY_COLOR);
    }
    
    private void addDividerLine(Document document, Color color) throws DocumentException {
        document.add(new Paragraph(" "));
        PdfPTable lineTable = new PdfPTable(1);
        lineTable.setWidthPercentage(100);
        PdfPCell lineCell = new PdfPCell();
        lineCell.setBorder(Rectangle.BOTTOM);
        lineCell.setBorderColor(color);
        lineCell.setBorderWidth(1f);
        lineCell.setFixedHeight(1);
        lineTable.addCell(lineCell);
        document.add(lineTable);
        document.add(new Paragraph(" "));
    }
    
    private void addCustomerInfo(Document document, Booking booking) throws DocumentException {
        Paragraph customerHeader = new Paragraph("Customer Information", SUBTITLE_FONT);
        customerHeader.setSpacingBefore(10);
        customerHeader.setSpacingAfter(10);
        document.add(customerHeader);
        
        PdfPTable customerTable = new PdfPTable(2);
        customerTable.setWidthPercentage(100);
        customerTable.setWidths(new float[]{1, 2});
        
        addCustomerRow(customerTable, "Customer Name:", booking.getCustomerName());
        addCustomerRow(customerTable, "Phone:", booking.getPhone());
        addCustomerRow(customerTable, "Location:", booking.getLocation());
        addCustomerRow(customerTable, "Pickup Date:", booking.getPickupDate().format(DateTimeFormatter.ofPattern("MMMM d, yyyy")));
        if (booking.getEmail() != null && !booking.getEmail().isEmpty()) {
            addCustomerRow(customerTable, "Email:", booking.getEmail());
        }
        if (booking.getNotes() != null && !booking.getNotes().isEmpty()) {
            addCustomerRow(customerTable, "Notes:", booking.getNotes());
        }
        
        document.add(customerTable);
    }
    
    private void addCustomerRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, BOLD_FONT));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5);
        labelCell.setBackgroundColor(LIGHT_GRAY);
        table.addCell(labelCell);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value, NORMAL_FONT));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }
    
    private void addOrderItems(Document document, Booking booking) throws DocumentException {
        Paragraph itemsHeader = new Paragraph("Order Items", SUBTITLE_FONT);
        itemsHeader.setSpacingBefore(20);
        itemsHeader.setSpacingAfter(10);
        document.add(itemsHeader);
        
        PdfPTable itemsTable = new PdfPTable(4);
        itemsTable.setWidthPercentage(100);
        itemsTable.setWidths(new float[]{3, 1.5f, 1, 1.5f});
        
        // Table Header
        addTableHeader(itemsTable, "Breed");
        addTableHeader(itemsTable, "Unit Price");
        addTableHeader(itemsTable, "Qty");
        addTableHeader(itemsTable, "Subtotal");
        
        // Table Rows
        for (BookingItem item : booking.getItems()) {
            addItemRow(itemsTable, 
                item.getChick().getBreed(), 
                "UGX " + formatNumber(item.getUnitPrice()),
                String.valueOf(item.getQuantity()),
                "UGX " + formatNumber(item.getSubtotal()));
        }
        
        document.add(itemsTable);
    }
    
    private void addTableHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setBackgroundColor(PRIMARY_COLOR);
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }
    
    private void addItemRow(PdfPTable table, String breed, String unitPrice, String quantity, String subtotal) {
        PdfPCell breedCell = new PdfPCell(new Phrase(breed, NORMAL_FONT));
        breedCell.setPadding(8);
        breedCell.setBackgroundColor(LIGHT_GRAY);
        table.addCell(breedCell);
        
        PdfPCell priceCell = new PdfPCell(new Phrase(unitPrice, NORMAL_FONT));
        priceCell.setPadding(8);
        priceCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(priceCell);
        
        PdfPCell qtyCell = new PdfPCell(new Phrase(quantity, NORMAL_FONT));
        qtyCell.setPadding(8);
        qtyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        qtyCell.setBackgroundColor(LIGHT_GRAY);
        table.addCell(qtyCell);
        
        PdfPCell subtotalCell = new PdfPCell(new Phrase(subtotal, BOLD_FONT));
        subtotalCell.setPadding(8);
        subtotalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(subtotalCell);
    }
    
    private void addTotalSection(Document document, Booking booking) throws DocumentException {
        document.add(new Paragraph(" "));
        
        PdfPTable totalTable = new PdfPTable(2);
        totalTable.setWidthPercentage(50);
        totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        
        PdfPCell labelCell = new PdfPCell(new Phrase("TOTAL AMOUNT:", BOLD_FONT));
        labelCell.setBorder(Rectangle.TOP);
        labelCell.setBorderColor(PRIMARY_COLOR);
        labelCell.setBorderWidth(2);
        labelCell.setPadding(10);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalTable.addCell(labelCell);
        
        PdfPCell amountCell = new PdfPCell(new Phrase("UGX " + formatNumber(booking.getTotalAmount()), TOTAL_FONT));
        amountCell.setBorder(Rectangle.TOP);
        amountCell.setBorderColor(PRIMARY_COLOR);
        amountCell.setBorderWidth(2);
        amountCell.setPadding(10);
        amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalTable.addCell(amountCell);
        
        document.add(totalTable);
    }
    
    private void addReceiptFooter(Document document) throws DocumentException {
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));
        
        addDividerLine(document, Color.LIGHT_GRAY);
        
        Paragraph thankYou = new Paragraph("Thank you for your order!", SUBTITLE_FONT);
        thankYou.setAlignment(Element.ALIGN_CENTER);
        thankYou.setSpacingBefore(20);
        document.add(thankYou);
        
        Paragraph footer = new Paragraph("This is a computer-generated receipt.", SMALL_FONT);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(10);
        document.add(footer);
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        Paragraph generatedAt = new Paragraph("Generated at: " + LocalDateTime.now().format(formatter), SMALL_FONT);
        generatedAt.setAlignment(Element.ALIGN_CENTER);
        document.add(generatedAt);
    }
    
    /**
     * Generate a PDF report for bookings list
     */
    public byte[] generateBookingsReport(List<Booking> bookings, String reportTitle) throws DocumentException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 30, 30, 30, 30);
        
        try {
            PdfWriter.getInstance(document, baos);
            document.open();
            
            // Title
            Paragraph title = new Paragraph(reportTitle, TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);
            
            // Generated date
            Paragraph date = new Paragraph("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy HH:mm")), SMALL_FONT);
            date.setAlignment(Element.ALIGN_CENTER);
            date.setSpacingAfter(20);
            document.add(date);
            
            // Summary stats
            addReportSummary(document, bookings);
            
            // Bookings table
            if (!bookings.isEmpty()) {
                addBookingsTable(document, bookings);
            } else {
                Paragraph noData = new Paragraph("No bookings found.", NORMAL_FONT);
                noData.setAlignment(Element.ALIGN_CENTER);
                document.add(noData);
            }
            
            document.close();
        } catch (Exception e) {
            throw new DocumentException("Error generating PDF report: " + e.getMessage());
        }
        
        return baos.toByteArray();
    }
    
    private void addReportSummary(Document document, List<Booking> bookings) throws DocumentException {
        PdfPTable summaryTable = new PdfPTable(4);
        summaryTable.setWidthPercentage(100);
        summaryTable.setSpacingAfter(20);
        
        long pending = bookings.stream().filter(b -> b.getStatus().name().equals("PENDING")).count();
        long confirmed = bookings.stream().filter(b -> b.getStatus().name().equals("CONFIRMED")).count();
        long completedCount = bookings.stream().filter(b -> b.getStatus().name().equals("COMPLETED")).count();
        BigDecimal total = bookings.stream()
            .filter(b -> b.getStatus().name().equals("COMPLETED"))
            .map(Booking::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        addSummaryCell(summaryTable, "Total Bookings", String.valueOf(bookings.size()), new Color(59, 130, 246));
        addSummaryCell(summaryTable, "Pending", String.valueOf(pending), new Color(234, 179, 8));
        addSummaryCell(summaryTable, "Confirmed/Completed", String.valueOf(confirmed + completedCount), new Color(34, 197, 94));
        addSummaryCell(summaryTable, "Completed Revenue", "UGX " + formatNumber(total), new Color(139, 92, 246));
        
        document.add(summaryTable);
    }
    
    private void addSummaryCell(PdfPTable table, String label, String value, Color bgColor) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bgColor);
        cell.setPadding(15);
        cell.setBorder(Rectangle.NO_BORDER);
        
        Font labelFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.WHITE);
        Font valueFont = new Font(Font.HELVETICA, 18, Font.BOLD, Color.WHITE);
        
        Paragraph labelP = new Paragraph(label, labelFont);
        labelP.setAlignment(Element.ALIGN_CENTER);
        Paragraph valueP = new Paragraph(value, valueFont);
        valueP.setAlignment(Element.ALIGN_CENTER);
        
        cell.addElement(labelP);
        cell.addElement(valueP);
        
        table.addCell(cell);
    }
    
    private void addBookingsTable(Document document, List<Booking> bookings) throws DocumentException {
        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.5f, 1.5f, 2, 2, 1.5f, 1.5f, 1, 1.5f});
        
        // Headers
        String[] headers = {"Receipt #", "ID", "Customer", "Location", "Phone", "Pickup Date", "Status", "Amount"};
        for (String header : headers) {
            addTableHeader(table, header);
        }
        
        // Data rows
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
        for (Booking booking : bookings) {
            addBookingRow(table, 
                booking.getReceiptNumber() != null ? booking.getReceiptNumber() : "-",
                "#" + booking.getId(),
                booking.getCustomerName(),
                booking.getLocation(),
                booking.getPhone(),
                booking.getPickupDate().format(dateFormatter),
                booking.getStatus().name(),
                "UGX " + formatNumber(booking.getTotalAmount()));
        }
        
        document.add(table);
    }
    
    private void addBookingRow(PdfPTable table, String... values) {
        boolean alternate = table.getRows().size() % 2 == 0;
        for (String value : values) {
            PdfPCell cell = new PdfPCell(new Phrase(value, NORMAL_FONT));
            cell.setPadding(6);
            if (alternate) {
                cell.setBackgroundColor(LIGHT_GRAY);
            }
            table.addCell(cell);
        }
    }
    
    /**
     * Generate a PDF report for chicks inventory
     */
    public byte[] generateChicksReport(List<Chick> chicks) throws DocumentException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        
        try {
            PdfWriter.getInstance(document, baos);
            document.open();
            
            // Title
            Paragraph title = new Paragraph("CHICKS INVENTORY REPORT", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);
            
            // Generated date
            Paragraph date = new Paragraph("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy HH:mm")), SMALL_FONT);
            date.setAlignment(Element.ALIGN_CENTER);
            date.setSpacingAfter(20);
            document.add(date);
            
            // Chicks table
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{0.5f, 2, 1.5f, 1});
            
            addTableHeader(table, "ID");
            addTableHeader(table, "Breed");
            addTableHeader(table, "Price");
            addTableHeader(table, "Status");
            
            for (Chick chick : chicks) {
                addChickRow(table, chick);
            }
            
            document.add(table);
            document.close();
        } catch (Exception e) {
            throw new DocumentException("Error generating chicks report: " + e.getMessage());
        }
        
        return baos.toByteArray();
    }
    
    private void addChickRow(PdfPTable table, Chick chick) {
        boolean alternate = table.getRows().size() % 2 == 0;
        Color bgColor = alternate ? LIGHT_GRAY : Color.WHITE;
        
        PdfPCell idCell = new PdfPCell(new Phrase("#" + chick.getId(), NORMAL_FONT));
        idCell.setPadding(8);
        idCell.setBackgroundColor(bgColor);
        table.addCell(idCell);
        
        PdfPCell breedCell = new PdfPCell(new Phrase(chick.getBreed(), BOLD_FONT));
        breedCell.setPadding(8);
        breedCell.setBackgroundColor(bgColor);
        table.addCell(breedCell);
        
        PdfPCell priceCell = new PdfPCell(new Phrase("UGX " + formatNumber(chick.getPrice()), NORMAL_FONT));
        priceCell.setPadding(8);
        priceCell.setBackgroundColor(bgColor);
        table.addCell(priceCell);
        
        PdfPCell statusCell = new PdfPCell(new Phrase(chick.isActive() ? "Active" : "Inactive", NORMAL_FONT));
        statusCell.setPadding(8);
        statusCell.setBackgroundColor(chick.isActive() ? new Color(220, 252, 231) : new Color(254, 226, 226));
        table.addCell(statusCell);
    }
    
    /**
     * Generate a PDF report for locations
     */
    public byte[] generateLocationsReport(List<Location> locations) throws DocumentException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 30, 30, 30, 30);
        
        try {
            PdfWriter.getInstance(document, baos);
            document.open();
            
            // Title
            Paragraph title = new Paragraph("LOCATIONS REPORT", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);
            
            // Generated date
            Paragraph date = new Paragraph("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy HH:mm")), SMALL_FONT);
            date.setAlignment(Element.ALIGN_CENTER);
            date.setSpacingAfter(20);
            document.add(date);
            
            // Locations table
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{0.5f, 2, 1.5f, 1.5f, 1.5f, 1});
            
            addTableHeader(table, "ID");
            addTableHeader(table, "Name");
            addTableHeader(table, "District");
            addTableHeader(table, "Region");
            addTableHeader(table, "Contact");
            addTableHeader(table, "Status");
            
            for (Location location : locations) {
                addLocationRow(table, location);
            }
            
            document.add(table);
            document.close();
        } catch (Exception e) {
            throw new DocumentException("Error generating locations report: " + e.getMessage());
        }
        
        return baos.toByteArray();
    }
    
    private void addLocationRow(PdfPTable table, Location location) {
        boolean alternate = table.getRows().size() % 2 == 0;
        Color bgColor = alternate ? LIGHT_GRAY : Color.WHITE;
        
        addSimpleCell(table, "#" + location.getId(), bgColor);
        addSimpleCell(table, location.getName(), bgColor);
        addSimpleCell(table, location.getDistrict() != null ? location.getDistrict() : "-", bgColor);
        addSimpleCell(table, location.getRegion() != null ? location.getRegion() : "-", bgColor);
        addSimpleCell(table, location.getContact() != null ? location.getContact() : "-", bgColor);
        
        boolean isActive = location.getActive() != null && location.getActive();
        PdfPCell statusCell = new PdfPCell(new Phrase(isActive ? "Active" : "Inactive", NORMAL_FONT));
        statusCell.setPadding(6);
        statusCell.setBackgroundColor(isActive ? new Color(220, 252, 231) : new Color(254, 226, 226));
        table.addCell(statusCell);
    }
    
    /**
     * Generate a PDF report for operators/users
     */
    public byte[] generateOperatorsReport(List<User> operators) throws DocumentException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        
        try {
            PdfWriter.getInstance(document, baos);
            document.open();
            
            // Title
            Paragraph title = new Paragraph("OPERATORS REPORT", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);
            
            // Generated date
            Paragraph date = new Paragraph("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy HH:mm")), SMALL_FONT);
            date.setAlignment(Element.ALIGN_CENTER);
            date.setSpacingAfter(20);
            document.add(date);
            
            // Operators table
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{0.5f, 2, 2, 1.5f, 1});
            
            addTableHeader(table, "ID");
            addTableHeader(table, "Username");
            addTableHeader(table, "Full Name");
            addTableHeader(table, "Created At");
            addTableHeader(table, "Status");
            
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
            for (User operator : operators) {
                addOperatorRow(table, operator, dateFormatter);
            }
            
            document.add(table);
            document.close();
        } catch (Exception e) {
            throw new DocumentException("Error generating operators report: " + e.getMessage());
        }
        
        return baos.toByteArray();
    }
    
    private void addOperatorRow(PdfPTable table, User operator, DateTimeFormatter formatter) {
        boolean alternate = table.getRows().size() % 2 == 0;
        Color bgColor = alternate ? LIGHT_GRAY : Color.WHITE;
        
        addSimpleCell(table, "#" + operator.getId(), bgColor);
        addSimpleCell(table, operator.getUsername(), bgColor);
        addSimpleCell(table, operator.getFullName(), bgColor);
        addSimpleCell(table, operator.getCreatedAt() != null ? operator.getCreatedAt().format(formatter) : "-", bgColor);
        
        PdfPCell statusCell = new PdfPCell(new Phrase(operator.isEnabled() ? "Active" : "Disabled", NORMAL_FONT));
        statusCell.setPadding(6);
        statusCell.setBackgroundColor(operator.isEnabled() ? new Color(220, 252, 231) : new Color(254, 226, 226));
        table.addCell(statusCell);
    }
    
    private void addSimpleCell(PdfPTable table, String text, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, NORMAL_FONT));
        cell.setPadding(6);
        cell.setBackgroundColor(bgColor);
        table.addCell(cell);
    }
    
    private String formatNumber(BigDecimal number) {
        if (number == null) return "0";
        return String.format("%,.0f", number);
    }
}
