package com.example.intranet_back_stage.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FileIconService {

    // Optional absolute base URL for cross-origin frontends.
    // Example application.yml: app.publicBaseUrl: "https://api.example.com"
    @Value("${app.publicBaseUrl:}")
    private String publicBaseUrl;

    private static final Map<String, String> EXT_TO_ICON = new HashMap<>();
    static {
        // Office / docs
        EXT_TO_ICON.put("pdf", "/icons/pdf.svg");
        EXT_TO_ICON.put("doc", "/icons/Word.svg");
        EXT_TO_ICON.put("docx", "/icons/Word.svg");
        EXT_TO_ICON.put("xls", "/icons/Excel.svg");
        EXT_TO_ICON.put("xlsx", "/icons/Excel.svg");
        EXT_TO_ICON.put("ppt", "/icons/PowerPoint.svg");
        EXT_TO_ICON.put("pptx", "/icons/PowerPoint.svg");
        EXT_TO_ICON.put("csv", "/icons/Excel.svg");
    }

    public String iconFor(String docType, String filename) {
        String ext = getExt(filename);
        if (ext != null) {
            String mapped = EXT_TO_ICON.get(ext);
            if (mapped != null) return absolutize(mapped);
        }
        // Fallback by docType (your app uses: PDF, WORD, EXCEL, PPT, IMAGE, TEXT, FILE)
        if (docType != null) {
            switch (docType) {
                case "PDF":   return absolutize("/icons/pdf.svg");
                case "WORD":  return absolutize("/icons/Word.svg");
                case "EXCEL": return absolutize("/icons/Excel.svg");
                case "PPT":   return absolutize("/icons/PowerPoint.svg");
            }
        }
        return absolutize("/icons/file-generic.svg");
    }

    private String getExt(String filename) {
        if (filename == null) return null;
        int i = filename.lastIndexOf('.');
        if (i < 0 || i == filename.length() - 1) return null;
        return filename.substring(i + 1).toLowerCase();
    }

    private String absolutize(String path) {
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) return path;
        return (publicBaseUrl.endsWith("/"))
                ? publicBaseUrl.substring(0, publicBaseUrl.length()-1) + path
                : publicBaseUrl + path;
    }
}
