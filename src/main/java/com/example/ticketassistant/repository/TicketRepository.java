package com.example.ticketassistant.repository;

import com.example.ticketassistant.entity.Ticket;
import com.example.ticketassistant.entity.ProcessingStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, String> {
    @Modifying
    @Query("update Ticket t set t.processingStatus = :processing, t.processingError = null "
            + "where t.id = :ticketId and t.processingStatus = :pending")
    int claimPending(@Param("ticketId") String ticketId, @Param("pending") ProcessingStatus pending,
            @Param("processing") ProcessingStatus processing);
}
