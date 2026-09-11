package com.example.ticketassistant.llm;

import com.example.ticketassistant.entity.Ticket;

public interface TicketAnalysisProvider {
    String analyze(Ticket ticket);
}
