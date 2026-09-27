package com.liveinterviewer.sessionservice.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Service
public class FileTextExtractor {

    public String extractText(MultipartFile file) {
        String filename = file.getOriginalFilename();
        log.info("Extracting text from file: {} (size: {} bytes)", filename, file.getSize());

        if (filename == null) {
            throw new IllegalArgumentException("Uploaded file has no name");
        }

        String lower = filename.toLowerCase();

        try {
            String text;
            if (lower.endsWith(".pdf")) {
                text = extractPdf(file);
            } else if (lower.endsWith(".docx")) {
                text = extractDocx(file);
            } else {
                throw new IllegalArgumentException("Only .pdf and .docx files are supported");
            }

            if (text == null || text.isBlank()) {
                log.warn("Extracted text is EMPTY from file: {}. " +
                        "This usually means the PDF is image-based (scanned). " +
                        "Please upload a text-selectable PDF or DOCX.", filename);
                throw new IllegalArgumentException(
                        "Could not extract text from '" + filename + "'. " +
                        "Please make sure it's a text-based PDF (not a scanned image) or a .docx file.");
            }

            log.info("Successfully extracted {} characters from {}", text.length(), filename);
            return text;

        } catch (IOException ex) {
            log.error("IOException reading file {}: {}", filename, ex.getMessage());
            throw new IllegalStateException("Failed to read uploaded file: " + ex.getMessage());
        }
    }

    private String extractPdf(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractDocx(MultipartFile file) throws IOException {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream());
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }
}
