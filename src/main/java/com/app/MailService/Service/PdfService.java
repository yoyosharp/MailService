package com.app.MailService.Service;


import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Service
@Slf4j
public class PdfService {

    public byte[] createPdfFromHtml(String htmlContent, String password) {
        if (htmlContent.isEmpty()) {
            throw new IllegalArgumentException("Trying to create PDF from empty HTML content");
        }

//        org.jsoup.nodes.Document jsoupDocument = Jsoup.parse(htmlContent);
//        jsoupDocument.outputSettings(new org.jsoup.nodes.Document.OutputSettings().syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml));
//        jsoupDocument.outputSettings().escapeMode(Entities.EscapeMode.xhtml);
//        htmlContent = jsoupDocument.html();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Document document = new Document();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, byteArrayOutputStream);
            writer.setEncryption(
                    password.getBytes(), password.getBytes(),
                    PdfWriter.ALLOW_PRINTING | PdfWriter.ALLOW_COPY,
                    PdfWriter.ENCRYPTION_AES_128
            );

            document.open();
            XMLWorkerHelper.getInstance().parseXHtml(writer, document, new ByteArrayInputStream(htmlContent.getBytes()));
        } catch (Exception e) {
            log.error("Error while creating PDF: {}", e.getMessage());
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        return byteArrayOutputStream.toByteArray();
    }
}
