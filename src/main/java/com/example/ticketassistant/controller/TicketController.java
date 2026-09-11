package com.example.ticketassistant.controller;

import com.example.ticketassistant.dto.CreateTicketRequest;
import com.example.ticketassistant.dto.TicketResponse;
import com.example.ticketassistant.service.TicketService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {
    private final TicketService service;

    public TicketController(TicketService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody CreateTicketRequest request) {
        TicketResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/tickets/" + response.ticketId())).body(response);
    }

    @GetMapping("/{ticketId}")
    public TicketResponse get(@PathVariable String ticketId) {
        return service.get(ticketId);
    }
}
