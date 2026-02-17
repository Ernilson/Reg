package br.com.menu.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RagService {

    private final OllamaService ollamaService;

    public String ask(String question, String context) {

        String prompt = """
                Responda baseado somente no contexto abaixo.

                Contexto:
                %s

                Pergunta:
                %s
                """.formatted(context, question);

        return ollamaService.generate(prompt);
    }
}

