package com.example.ticketassistant.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ticket_analyses")
public class TicketAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false, unique = true)
    private Ticket ticket;
    @Column(nullable = false)
    private String category;
    @Column(nullable = false, length = 2000)
    private String summary;
    @Column(nullable = false, length = 4000)
    private String suggestedResponse;
    @Column(nullable = false)
    private String recommendedTeam;
    @Column(nullable = false)
    private double confidence;
    @Column(nullable = false)
    private Instant createdAt;

    protected TicketAnalysis() {
    }

    public TicketAnalysis(Ticket ticket, String category, String summary, String suggestedResponse,
            String recommendedTeam, double confidence) {
        if (!isIdentifier(category) || !isIdentifier(recommendedTeam))
            throw new IllegalArgumentException("Category and recommended team must be uppercase identifiers");
        if (!Double.isFinite(confidence) || confidence < 0 || confidence > 1)
            throw new IllegalArgumentException("Confidence must be between 0 and 1");
        this.ticket = ticket;
        this.category = category;
        this.summary = summary;
        this.suggestedResponse = suggestedResponse;
        this.recommendedTeam = recommendedTeam;
        this.confidence = confidence;
    }

    private boolean isIdentifier(String value) {
        return value != null && value.matches("[A-Z][A-Z0-9_]*");
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public String getCategory() {
        return category;
    }

    public String getSummary() {
        return summary;
    }

    public String getSuggestedResponse() {
        return suggestedResponse;
    }

    public String getRecommendedTeam() {
        return recommendedTeam;
    }

    public double getConfidence() {
        return confidence;
    }
}
