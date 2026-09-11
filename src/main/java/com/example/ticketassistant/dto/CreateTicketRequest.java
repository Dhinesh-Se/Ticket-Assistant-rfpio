package com.example.ticketassistant.dto;

import com.example.ticketassistant.entity.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTicketRequest(@NotBlank String customerId, @NotBlank String subject, @NotBlank String description,
        @NotNull Priority priority, String product) {
}
