package br.com.menu.ragController;

import br.com.menu.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rag")
public class RagController {

    private final RagService ragService;

    public static class AskRequest {

        private String question;

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }
    }


    @PostMapping("/ask")
    public RagService.RagAnswer ask(@RequestBody AskRequest req) {
        return ragService.ask(req.getQuestion());
    }
}
