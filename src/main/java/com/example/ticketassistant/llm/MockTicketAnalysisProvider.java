package com.example.ticketassistant.llm;

import com.example.ticketassistant.entity.Ticket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!real-llm")
public class MockTicketAnalysisProvider implements TicketAnalysisProvider {
    private final String mode;
    private final long delayMs;

    public MockTicketAnalysisProvider(@Value("${llm.mock.mode:success}") String mode,
            @Value("${llm.mock.delay-ms:100}") long delayMs) {
        this.mode = mode;
        this.delayMs = delayMs;
    }

    public String analyze(Ticket ticket) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Analysis interrupted", e);
        }
        if ("failure".equalsIgnoreCase(mode))
            throw new IllegalStateException("Mock provider failure");
        if ("invalid".equalsIgnoreCase(mode))
            return "{\"category\":\"IMPORT_FAILURE\",\"confidence\":1.4}";
        String category = ticket.getSubject().toLowerCase().contains("import") ? "IMPORT_FAILURE" : "GENERAL_SUPPORT";
        return "{\"category\":\"%s\",\"summary\":\"The customer needs assistance with %s.\",\"suggestedResponse\":\"Thank you for reporting this. Our support team is reviewing the issue.\",\"recommendedTeam\":\"SUPPORT\",\"confidence\":0.85}"
                .formatted(category, escape(ticket.getSubject()));
    }

    private String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
