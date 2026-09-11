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

@SpringBootTest(properties={"llm.mock.delay-ms=300"})
@AutoConfigureMockMvc
class TicketControllerIntegrationTest {
    @Autowired MockMvc mvc; @Autowired TicketRepository tickets;
    @Test void createsTicketWithGeneratedIdAndPendingStatus() throws Exception {
        mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content("{\"customerId\":\"CUST-101\",\"subject\":\"Import failure\",\"description\":\"Stuck at 80%\",\"priority\":\"HIGH\",\"product\":\"Import\"}"))
            .andExpect(status().isCreated()).andExpect(header().string("Location",org.hamcrest.Matchers.containsString("/api/tickets/"))).andExpect(jsonPath("$.ticketId").isNotEmpty()).andExpect(jsonPath("$.status").value("PENDING")).andExpect(jsonPath("$.analysis").doesNotExist());
        org.junit.jupiter.api.Assertions.assertEquals(1,tickets.count());
    }
    @Test void rejectsInvalidRequestAndReturnsConsistentError() throws Exception { mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content("{\"customerId\":\"\",\"subject\":\"\",\"description\":\"\",\"priority\":\"URGENT\"}"))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION_ERROR")); }
    @Test void returns404ForMissingTicket() throws Exception { mvc.perform(get("/api/tickets/missing")).andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("TICKET_NOT_FOUND")); }
}
