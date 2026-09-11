package com.example.ticketassistant.dto;
public record TicketAnalysisResponse(String category, String summary, String suggestedResponse, String recommendedTeam, double confidence) { }
