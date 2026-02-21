package br.com.menu.ragController;

import br.com.menu.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rag")
public class RagController {

    private final RagService ragService;

    @PostMapping("/ask")
    public RagService.RagAnswer ask(@RequestBody AskRequest askRequest) {
        return ragService.ask(
                askRequest.getSessionId(),
                askRequest.getQuestion()
        );
    }
    public static class AskRequest {

        private String sessionId;
        private String question;

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }
    }

}
