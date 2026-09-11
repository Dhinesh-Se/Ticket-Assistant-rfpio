package com.example.ticketassistant.llm;

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

/** Generic configurable HTTP adapter. The endpoint receives {"prompt":"..."} and returns analysis JSON directly. */
@Component
@Profile("real-llm")
public class RealTicketAnalysisProvider implements TicketAnalysisProvider {
    private final HttpClient client; private final String endpoint; private final String apiKey; private final Duration timeout; private final Resource promptTemplate;
    public RealTicketAnalysisProvider(@Value("${llm.real.endpoint}") String endpoint, @Value("${llm.real.api-key}") String apiKey, @Value("${llm.timeout-seconds:10}") long timeoutSeconds, @Value("classpath:prompts/ticket-analysis-prompt.md") Resource promptTemplate) { this.client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(timeoutSeconds)).build(); this.endpoint=endpoint; this.apiKey=apiKey; this.timeout=Duration.ofSeconds(timeoutSeconds); this.promptTemplate=promptTemplate; }
    public String analyze(Ticket ticket) {
        try {
            String prompt=new String(promptTemplate.getInputStream().readAllBytes(),StandardCharsets.UTF_8); prompt=prompt.replace("{{customerId}}",ticket.getCustomerId()).replace("{{subject}}",ticket.getSubject()).replace("{{description}}",ticket.getDescription()).replace("{{priority}}",ticket.getPriority().name()).replace("{{product}}",ticket.getProduct()==null?"":ticket.getProduct());
            HttpRequest request=HttpRequest.newBuilder(URI.create(endpoint)).timeout(timeout).header("Content-Type","application/json").header("Authorization","Bearer "+apiKey).POST(HttpRequest.BodyPublishers.ofString("{\"prompt\":"+json(prompt)+"}")).build(); HttpResponse<String> response=client.send(request,HttpResponse.BodyHandlers.ofString()); if(response.statusCode()<200||response.statusCode()>=300) throw new IllegalStateException("LLM provider returned a non-success status"); return response.body();
        } catch (java.net.http.HttpTimeoutException e) { throw new LlmTimeoutException("LLM request timed out",e); }
        catch (Exception e) { if(e instanceof LlmTimeoutException timeoutException) throw timeoutException; throw new IllegalStateException("LLM provider request failed",e); }
    }
    private String json(String value) { return "\""+value.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r")+"\""; }
}
