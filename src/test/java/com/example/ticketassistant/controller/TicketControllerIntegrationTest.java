package com.example.ticketassistant.controller;

import com.example.ticketassistant.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = { "llm.mock.delay-ms=300" })
@AutoConfigureMockMvc
class TicketControllerIntegrationTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    TicketRepository tickets;

    @Test
    void createsTicketWithGeneratedIdAndPendingStatus() throws Exception {
        long ticketCountBefore = tickets.count();
        mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(
                "{\"customerId\":\"CUST-101\",\"subject\":\"Import failure\",\"description\":\"Stuck at 80%\",\"priority\":\"HIGH\",\"product\":\"Import\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/tickets/")))
                .andExpect(jsonPath("$.ticketId").isNotEmpty()).andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.analysis").doesNotExist());
        org.junit.jupiter.api.Assertions.assertEquals(ticketCountBefore + 1, tickets.count());
    }

    @Test
    void rejectsInvalidRequestAndReturnsConsistentError() throws Exception {
        mvc.perform(post("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\":\"\",\"subject\":\"\",\"description\":\"\",\"priority\":\"URGENT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void returns404ForMissingTicket() throws Exception {
        mvc.perform(get("/api/tickets/missing")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("TICKET_NOT_FOUND"));
    }

    @Test
    void eventuallyReturnsCompletedStructuredAnalysis() throws Exception {
        String response = mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(
                "{\"customerId\":\"CUST-102\",\"subject\":\"Import failure\",\"description\":\"Stuck at 80%\",\"priority\":\"HIGH\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String ticketId = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).get("ticketId").asText();

        awaitCompletion(ticketId);
        mvc.perform(get("/api/tickets/" + ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.analysis.category").isNotEmpty())
                .andExpect(jsonPath("$.analysis.summary").isNotEmpty())
                .andExpect(jsonPath("$.analysis.suggestedResponse").isNotEmpty())
                .andExpect(jsonPath("$.analysis.recommendedTeam").isNotEmpty())
                .andExpect(jsonPath("$.analysis.confidence").value(0.85));
    }

    private void awaitCompletion(String ticketId) {
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline) {
            try {
                String response = mvc.perform(get("/api/tickets/" + ticketId)).andReturn().getResponse()
                        .getContentAsString();
                if (response.contains("COMPLETED"))
                    return;
                Thread.sleep(50);
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }
        throw new AssertionError("Ticket did not complete within the test timeout");
    }
}
