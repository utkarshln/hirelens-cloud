package com.hirelens.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Component
public class PdfExtractor {

    public String extractText(MultipartFile file) throws Exception {
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        byte[] bytes = file.getBytes();
        if (name.endsWith(".pdf")) {
            try (PDDocument doc = Loader.loadPDF(bytes)) {
                String text = new PDFTextStripper().getText(doc);
                if (text != null && !text.isBlank()) return text;
                // if pdf has no text (scanned), fallback to raw
            } catch (Exception e) {
                // not a valid PDF (e.g. text file with .pdf ext from demo) -> fallback to text
            }
            // fallback: try as plain text
            String asText = new String(bytes);
            if (!asText.isBlank()) return asText;
            return "";
        } else {
            return new String(bytes);
        }
    }

    public String extractTextFallback(byte[] bytes, String filename) {
        try {
            if (filename != null && filename.toLowerCase().endsWith(".pdf")) {
                try (PDDocument doc = Loader.loadPDF(bytes)) {
                    return new PDFTextStripper().getText(doc);
                }
            }
            return new String(bytes);
        } catch (Exception e) { return new String(bytes); }
    }
}
