package com.example.ticketassistant.service;

import com.example.ticketassistant.entity.ProcessingStatus;
import com.example.ticketassistant.entity.Ticket;
import com.example.ticketassistant.entity.TicketAnalysis;
import com.example.ticketassistant.llm.ValidatedAnalysis;
import com.example.ticketassistant.repository.TicketAnalysisRepository;
import com.example.ticketassistant.repository.TicketRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketAnalysisPersistenceService {
    private final TicketRepository tickets;
    private final TicketAnalysisRepository analyses;

    public TicketAnalysisPersistenceService(TicketRepository tickets, TicketAnalysisRepository analyses) {
        this.tickets = tickets;
        this.analyses = analyses;
    }

    @Transactional
    public boolean claimPending(String ticketId) {
        return tickets.claimPending(ticketId, ProcessingStatus.PENDING, ProcessingStatus.PROCESSING) == 1;
    }

    @Transactional(readOnly = true)
    public Optional<Ticket> find(String ticketId) {
        return tickets.findById(ticketId);
    }

    @Transactional
    public void complete(String ticketId, ValidatedAnalysis output) {
        Ticket ticket = tickets.findById(ticketId).orElseThrow();
        analyses.save(new TicketAnalysis(ticket, output.category(), output.summary(), output.suggestedResponse(),
                output.recommendedTeam(), output.confidence()));
        ticket.markCompleted();
        tickets.save(ticket);
    }

    @Transactional
    public void fail(String ticketId) {
        tickets.findById(ticketId).ifPresent(ticket -> {
            if (ticket.getProcessingStatus() != ProcessingStatus.COMPLETED) {
                ticket.markFailed("Analysis could not be completed. Please try again later.");
                tickets.save(ticket);
            }
        });
    }
}