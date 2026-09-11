package com.example.ticketassistant.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.ticketassistant.entity.Ticket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/** Azure OpenAI Responses API adapter for the real-llm profile. */
@Component
@Profile("real-llm")
public class RealTicketAnalysisProvider implements TicketAnalysisProvider {
    private final HttpClient client;
    private final String endpoint;
    private final String apiKey;
    private final String authHeader;
    private final String authPrefix;
    private final String model;
    private final Duration timeout;
    private final Resource promptTemplate;
    private final ObjectMapper objectMapper;

    public RealTicketAnalysisProvider(@Value("${llm.real.endpoint}") String endpoint,
            @Value("${llm.real.api-key}") String apiKey, @Value("${llm.real.model:gpt-5.4-1}") String model,
            @Value("${llm.real.auth-header:api-key}") String authHeader,
            @Value("${llm.real.auth-prefix:}") String authPrefix,
            @Value("${llm.timeout-seconds:10}") long timeoutSeconds,
            @Value("classpath:prompts/ticket-analysis-prompt.md") Resource promptTemplate, ObjectMapper objectMapper) {
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(timeoutSeconds)).build();
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.authHeader = authHeader;
        this.authPrefix = authPrefix;
        this.model = model;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.promptTemplate = promptTemplate;
        this.objectMapper = objectMapper;
    }

    public String analyze(Ticket ticket) {
        try {
            String prompt = new String(promptTemplate.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            prompt = prompt.replace("{{customerId}}", ticket.getCustomerId())
                    .replace("{{subject}}", ticket.getSubject()).replace("{{description}}", ticket.getDescription())
                    .replace("{{priority}}", ticket.getPriority().name())
                    .replace("{{product}}", ticket.getProduct() == null ? "" : ticket.getProduct());
            String requestBody = objectMapper.createObjectNode().put("model", model).put("input", prompt).toString();
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint)).timeout(timeout)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Accept", "application/json")
                    .header(authHeader, authPrefix + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody)).build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new IllegalStateException("LLM provider returned a non-success status");
            return extractOutput(response.body());
        } catch (java.net.http.HttpTimeoutException e) {
            throw new LlmTimeoutException("LLM request timed out", e);
        } catch (Exception e) {
            if (e instanceof LlmTimeoutException timeoutException)
                throw timeoutException;
            throw new IllegalStateException("LLM provider request failed", e);
        }
    }

    private String extractOutput(String responseBody) throws Exception {
        JsonNode response = objectMapper.readTree(responseBody);
        if (response.has("category"))
            return responseBody;
        if (response.path("output_text").isTextual())
            return response.path("output_text").textValue();
        for (JsonNode outputItem : response.path("output")) {
            for (JsonNode contentItem : outputItem.path("content")) {
                if (contentItem.path("text").isTextual())
                    return contentItem.path("text").textValue();
            }
        }
        throw new IllegalStateException("LLM provider response did not contain output text");
    }
}
