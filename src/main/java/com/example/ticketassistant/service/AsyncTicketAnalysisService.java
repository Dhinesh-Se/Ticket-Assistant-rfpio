package com.example.ticketassistant.service;

import com.example.ticketassistant.entity.Ticket;
import com.example.ticketassistant.exception.InvalidAnalysisException;
import com.example.ticketassistant.llm.LlmTimeoutException;
import com.example.ticketassistant.llm.TicketAnalysisProvider;
import com.example.ticketassistant.llm.ValidatedAnalysis;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncTicketAnalysisService {
    private final TicketAnalysisPersistenceService persistence;
    private final TicketAnalysisProvider provider;
    private final AnalysisResponseValidator validator;

    public AsyncTicketAnalysisService(TicketAnalysisPersistenceService persistence, TicketAnalysisProvider provider,
            AnalysisResponseValidator validator) {
        this.persistence = persistence;
        this.provider = provider;
        this.validator = validator;
    }

    @Async("ticketAnalysisExecutor")
    public void analyze(String ticketId) {
        if (!persistence.claimPending(ticketId))
            return;

        Ticket ticket = persistence.find(ticketId).orElse(null);
        if (ticket == null)
            return;

        try {
            ValidatedAnalysis output = validator.validate(provider.analyze(ticket));
            persistence.complete(ticketId, output);
        } catch (LlmTimeoutException | InvalidAnalysisException | IllegalStateException e) {
            persistence.fail(ticketId);
        }
    }
}
