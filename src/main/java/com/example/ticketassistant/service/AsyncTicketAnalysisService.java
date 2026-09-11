package com.example.ticketassistant.service;

import com.example.ticketassistant.entity.Ticket;
import com.example.ticketassistant.entity.TicketAnalysis;
import com.example.ticketassistant.llm.TicketAnalysisProvider;
import com.example.ticketassistant.llm.ValidatedAnalysis;
import com.example.ticketassistant.repository.TicketAnalysisRepository;
import com.example.ticketassistant.repository.TicketRepository;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncTicketAnalysisService {
    private final TicketRepository tickets;
    private final TicketAnalysisRepository analyses;
    private final TicketAnalysisProvider provider;
    private final AnalysisResponseValidator validator;

    public AsyncTicketAnalysisService(TicketRepository tickets, TicketAnalysisRepository analyses,
            TicketAnalysisProvider provider, AnalysisResponseValidator validator) {
        this.tickets = tickets;
        this.analyses = analyses;
        this.provider = provider;
        this.validator = validator;
    }

    @Async("ticketAnalysisExecutor")
    @Transactional
    public void analyze(String ticketId) {
        Ticket ticket = tickets.findById(ticketId).orElse(null);
        if (ticket == null
                || ticket.getProcessingStatus() != com.example.ticketassistant.entity.ProcessingStatus.PENDING)
            return;
        ticket.markProcessing();
        tickets.save(ticket);
        try {
            ValidatedAnalysis output = validator.validate(provider.analyze(ticket));
            analyses.save(new TicketAnalysis(ticket, output.category(), output.summary(), output.suggestedResponse(),
                    output.recommendedTeam(), output.confidence()));
            ticket.markCompleted();
            tickets.save(ticket);
        } catch (Exception ignored) {
            ticket.markFailed("Analysis could not be completed. Please try again later.");
            tickets.save(ticket);
        }
    }
}
