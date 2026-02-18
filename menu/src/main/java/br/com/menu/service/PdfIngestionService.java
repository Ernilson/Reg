package br.com.menu.service;

import br.com.menu.repository.VectorRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PdfIngestionService {

    private final EmbeddingService embeddingService;
    private final VectorRepository vectorRepository;

    private static final int CHUNK_SIZE = 1000;
    private static final int OVERLAP = 200;

    public void ingest(MultipartFile file) throws Exception {

        try (InputStream is = file.getInputStream();
             PDDocument document = PDDocument.load(is)) {

            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = document.getNumberOfPages();

            for (int page = 1; page <= totalPages; page++) {

                stripper.setStartPage(page);
                stripper.setEndPage(page);

                String text = stripper.getText(document);

                List<String> chunks = chunkText(text);

                for (int i = 0; i < chunks.size(); i++) {

                    String chunk = chunks.get(i);

                    List<Double> embedding =
                            embeddingService.generateEmbedding(chunk);

                    vectorRepository.insertDocument(
                            UUID.randomUUID(),
                            file.getOriginalFilename(),
                            page,
                            i,
                            chunk,
                            embedding
                    );
                }
            }
        }
    }

    private List<String> chunkText(String text) {

        new StringBuilder();

        var chunks = new java.util.ArrayList<String>();

        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            chunks.add(text.substring(start, end));
            start += CHUNK_SIZE - OVERLAP;
        }

        return chunks;
    }
}

