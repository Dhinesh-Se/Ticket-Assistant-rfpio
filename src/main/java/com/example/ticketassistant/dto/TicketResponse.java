package com.example.ticketassistant.dto;

import com.example.ticketassistant.entity.Priority;
import com.example.ticketassistant.entity.ProcessingStatus;

public record TicketResponse(String ticketId, String customerId, String subject, String description, Priority priority,
        String product, ProcessingStatus status, TicketAnalysisResponse analysis, String processingError) {
}
