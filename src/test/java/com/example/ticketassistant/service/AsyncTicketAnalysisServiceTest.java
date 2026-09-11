package com.example.ticketassistant.service;

import com.example.ticketassistant.entity.*;
import com.example.ticketassistant.llm.LlmTimeoutException;
import com.example.ticketassistant.llm.TicketAnalysisProvider;
import com.example.ticketassistant.repository.TicketAnalysisRepository;
import com.example.ticketassistant.repository.TicketRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class AsyncTicketAnalysisServiceTest {
    @Mock
    TicketRepository tickets;
    @Mock
    TicketAnalysisRepository analyses;
    @Mock
    TicketAnalysisProvider provider;

    @Test
    void savesValidatedAnalysisAndCompletesTicket() {
        Ticket ticket = ticket();
        when(tickets.findById("id")).thenReturn(Optional.of(ticket));
        when(provider.analyze(ticket)).thenReturn(
                "{\"category\":\"IMPORT_FAILURE\",\"summary\":\"Import fails\",\"suggestedResponse\":\"We are reviewing it.\",\"recommendedTeam\":\"IMPORT_ENGINEERING\",\"confidence\":0.87}");
        service().analyze("id");
        assertEquals(ProcessingStatus.COMPLETED, ticket.getProcessingStatus());
        verify(analyses).save(any(TicketAnalysis.class));
    }

    @Test
    void failsTicketAndDoesNotSaveAnalysisForInvalidOutput() {
        Ticket ticket = ticket();
        when(tickets.findById("id")).thenReturn(Optional.of(ticket));
        when(provider.analyze(ticket)).thenReturn("not json");
        service().analyze("id");
        assertEquals(ProcessingStatus.FAILED, ticket.getProcessingStatus());
        verify(analyses, never()).save(any());
    }

    @Test
    void failsTicketWhenProviderThrowsIncludingTimeouts() {
        Ticket ticket = ticket();
        when(tickets.findById("id")).thenReturn(Optional.of(ticket));
        when(provider.analyze(ticket)).thenThrow(new IllegalStateException("timeout"));
        service().analyze("id");
        assertEquals(ProcessingStatus.FAILED, ticket.getProcessingStatus());
        verify(analyses, never()).save(any());
    }

    @Test
    void failsTicketWhenProviderTimesOut() {
        Ticket ticket = ticket();
        when(tickets.findById("id")).thenReturn(Optional.of(ticket));
        when(provider.analyze(ticket)).thenThrow(new LlmTimeoutException("timed out", new RuntimeException()));

        service().analyze("id");

        assertEquals(ProcessingStatus.FAILED, ticket.getProcessingStatus());
        verify(analyses, never()).save(any());
    }

    @Test
    void rejectsIncompleteStructuredOutput() {
        AnalysisResponseValidator validator = new AnalysisResponseValidator(new ObjectMapper());

        assertThrows(RuntimeException.class,
                () -> validator.validate("{\"category\":\"IMPORT_FAILURE\",\"confidence\":0.8}"));
    }

    @Test
    void rejectsUnexpectedFieldsNullValuesAndInvalidConfidence() {
        AnalysisResponseValidator validator = new AnalysisResponseValidator(new ObjectMapper());
        String valid = "\"category\":\"IMPORT_FAILURE\",\"summary\":\"Import fails\","
                + "\"suggestedResponse\":\"We are reviewing it.\",\"recommendedTeam\":\"SUPPORT\"";

        assertThrows(RuntimeException.class,
                () -> validator.validate("{" + valid + ",\"extra\":true,\"confidence\":0.8}"));
        assertThrows(RuntimeException.class, () -> validator.validate("{\"category\":null,\"summary\":\"Import fails\","
                + "\"suggestedResponse\":\"We are reviewing it.\",\"recommendedTeam\":\"SUPPORT\",\"confidence\":0.8}"));
        assertThrows(RuntimeException.class, () -> validator.validate("{" + valid + ",\"confidence\":1.1}"));
        assertThrows(RuntimeException.class, () -> validator.validate("{" + valid + ",\"confidence\":-0.1}"));
        assertThrows(RuntimeException.class,
                () -> validator.validate("{" + valid.replace("IMPORT_FAILURE", "ImportFailure")
                        + ",\"confidence\":0.8}"));
    }

    @Test
    void skipsDuplicateWorkerWhenTicketWasAlreadyClaimed() {
        AsyncTicketAnalysisService analysisService = service();
        when(tickets.claimPending("id", ProcessingStatus.PENDING, ProcessingStatus.PROCESSING)).thenReturn(0);
        analysisService.analyze("id");

        verify(provider, never()).analyze(any());
        verify(tickets, never()).findById("id");
    }

    private AsyncTicketAnalysisService service() {
        lenient().when(tickets.claimPending("id", ProcessingStatus.PENDING, ProcessingStatus.PROCESSING))
                .thenReturn(1);
        return new AsyncTicketAnalysisService(new TicketAnalysisPersistenceService(tickets, analyses), provider,
                new AnalysisResponseValidator(new ObjectMapper()));
    }

    private Ticket ticket() {
        return new Ticket("id", "CUST-1", "Import", "Description", Priority.HIGH, null);
    }
}
