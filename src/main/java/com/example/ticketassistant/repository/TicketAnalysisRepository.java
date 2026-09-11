package com.example.ticketassistant.repository;
import com.example.ticketassistant.entity.TicketAnalysis;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TicketAnalysisRepository extends JpaRepository<TicketAnalysis, String> { Optional<TicketAnalysis> findByTicketId(String ticketId); }
