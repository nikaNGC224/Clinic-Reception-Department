package com.poly;

import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class ReportGenerator {
    public ReportGenerator(DefaultTableModel defaultTableModel, String fileName, String title)
            throws DocumentException, IOException {
        String filePath = "report.pdf";
        if (!Objects.equals(fileName, "")) {
            filePath = fileName + ".pdf";
        }
        BaseFont baseFont = BaseFont.createFont("DejaVuSans.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        Font font = new Font(baseFont);
        try (FileOutputStream fs = new FileOutputStream(filePath)) {
            Document document = new Document();
            PdfWriter.getInstance(document, fs);
            document.open();

            Paragraph diagnosisParagraph = new Paragraph(title, font);
            diagnosisParagraph.setAlignment(Element.ALIGN_LEFT);
            document.add(diagnosisParagraph);
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(defaultTableModel.getColumnCount());
            table.setWidthPercentage(100);

            for (int i = 0; i < defaultTableModel.getColumnCount(); i++) {
                PdfPCell headerCell = new PdfPCell(new Phrase(defaultTableModel.getColumnName(i), font));
                headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(headerCell);
            }

            for (int i = 0; i < defaultTableModel.getRowCount(); i++) {
                for (int j = 0; j < defaultTableModel.getColumnCount(); j++) {
                    PdfPCell cell = new PdfPCell(new Phrase(defaultTableModel.getValueAt(i, j).toString(), font));
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    table.addCell(cell);
                }
            }

            document.add(table);

            Date currentDate = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            String formattedDate = dateFormat.format(currentDate);
            Paragraph dateParagraph = new Paragraph(formattedDate, font);
            dateParagraph.setAlignment(Element.ALIGN_LEFT);
            document.add(dateParagraph);

            document.close();
            System.out.println("Report generated successfully!");
            int choice = JOptionPane.showOptionDialog(null, "Отчет успешно создан! Открыть?",
                    "Уведомление", JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, null, null);


            if (choice == JOptionPane.YES_OPTION) {
                File file = new File(filePath);
                Desktop.getDesktop().open(file);
            }

        } catch (DocumentException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
