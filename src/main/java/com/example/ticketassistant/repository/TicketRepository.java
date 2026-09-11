package com.example.ticketassistant.repository;
import com.example.ticketassistant.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TicketRepository extends JpaRepository<Ticket, String> { }
