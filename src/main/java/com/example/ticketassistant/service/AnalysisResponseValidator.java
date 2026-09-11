package com.example.ticketassistant.service;

import com.example.ticketassistant.exception.InvalidAnalysisException;
import com.example.ticketassistant.llm.ValidatedAnalysis;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Iterator;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AnalysisResponseValidator {
    private static final Set<String> REQUIRED = Set.of("category", "summary", "suggestedResponse", "recommendedTeam", "confidence");
    private final ObjectMapper objectMapper;
    public AnalysisResponseValidator(ObjectMapper objectMapper) { this.objectMapper = objectMapper; }
    public ValidatedAnalysis validate(String rawResponse) {
        try {
            JsonNode root=objectMapper.readTree(rawResponse);
            if (!root.isObject() || root.size()!=REQUIRED.size()) throw new InvalidAnalysisException("Analysis must contain exactly the required fields");
            Iterator<String> fields=root.fieldNames(); while(fields.hasNext()) if(!REQUIRED.contains(fields.next())) throw new InvalidAnalysisException("Analysis contains unexpected fields");
            String category=requiredText(root,"category",100); String summary=requiredText(root,"summary",2000); String suggestedResponse=requiredText(root,"suggestedResponse",4000); String team=requiredText(root,"recommendedTeam",100);
            JsonNode confidenceNode=root.get("confidence"); if(confidenceNode==null || !confidenceNode.isNumber()) throw new InvalidAnalysisException("Confidence must be numeric");
            double confidence=confidenceNode.doubleValue(); if(!Double.isFinite(confidence)||confidence<0||confidence>1) throw new InvalidAnalysisException("Confidence must be between 0 and 1");
            return new ValidatedAnalysis(category,summary,suggestedResponse,team,confidence);
        } catch (InvalidAnalysisException e) { throw e; }
        catch (Exception e) { throw new InvalidAnalysisException("Analysis is not valid JSON", e); }
    }
    private String requiredText(JsonNode root, String field, int maxLength) { JsonNode node=root.get(field); if(node==null||!node.isTextual()||node.asText().isBlank()||node.asText().length()>maxLength) throw new InvalidAnalysisException("Invalid " + field); return node.asText().trim(); }
}
