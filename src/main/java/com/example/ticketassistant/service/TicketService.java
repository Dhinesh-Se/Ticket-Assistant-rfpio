package com.example.ticketassistant.service;

import com.example.ticketassistant.dto.CreateTicketRequest;
import com.example.ticketassistant.dto.TicketAnalysisResponse;
import com.example.ticketassistant.dto.TicketResponse;
import com.example.ticketassistant.entity.Ticket;
import com.example.ticketassistant.entity.TicketAnalysis;
import com.example.ticketassistant.exception.TicketNotFoundException;
import com.example.ticketassistant.repository.TicketAnalysisRepository;
import com.example.ticketassistant.repository.TicketRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class TicketService {
    private final TicketRepository tickets;
    private final TicketAnalysisRepository analyses;
    private final AsyncTicketAnalysisService asyncService;

    public TicketService(TicketRepository tickets, TicketAnalysisRepository analyses,
            AsyncTicketAnalysisService asyncService) {
        this.tickets = tickets;
        this.analyses = analyses;
        this.asyncService = asyncService;
    }

    @Transactional
    public TicketResponse create(CreateTicketRequest request) {
        Ticket ticket = tickets.saveAndFlush(
                new Ticket(UUID.randomUUID().toString(), request.customerId().trim(), request.subject().trim(),
                        request.description().trim(), request.priority(), blankToNull(request.product())));
        String ticketId = ticket.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                asyncService.analyze(ticketId);
            }
        });
        return response(ticket, null);
    }

    @Transactional(readOnly = true)
    public TicketResponse get(String id) {
        Ticket ticket = tickets.findById(id).orElseThrow(() -> new TicketNotFoundException(id));
        return response(ticket, analyses.findByTicketId(id).orElse(null));
    }

    private TicketResponse response(Ticket t, TicketAnalysis a) {
        TicketAnalysisResponse analysis = a == null ? null
                : new TicketAnalysisResponse(a.getCategory(), a.getSummary(), a.getSuggestedResponse(),
                        a.getRecommendedTeam(), a.getConfidence());
        return new TicketResponse(t.getId(), t.getCustomerId(), t.getSubject(), t.getDescription(), t.getPriority(),
                t.getProduct(), t.getProcessingStatus(), analysis,
                t.getProcessingStatus() == com.example.ticketassistant.entity.ProcessingStatus.FAILED
                        ? t.getProcessingError()
                        : null);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
