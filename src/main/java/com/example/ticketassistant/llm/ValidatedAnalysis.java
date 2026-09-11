package com.example.ticketassistant.llm;

public record ValidatedAnalysis(String category, String summary, String suggestedResponse, String recommendedTeam,
        double confidence) {
}
