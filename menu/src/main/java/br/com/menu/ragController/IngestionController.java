package br.com.menu.ragController;

import br.com.menu.service.PdfIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ingest")
public class IngestionController {

    private final PdfIngestionService ingestionService;

    @PostMapping("/pdf")
    public String upload(@RequestParam("file") MultipartFile file) throws Exception {

        ingestionService.ingest(file);

        return "PDF processado com sucesso!";
    }
}

