package com.example.ticketassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class TicketAssistantApplication {
    public static void main(String[] args) { SpringApplication.run(TicketAssistantApplication.class, args); }
}
