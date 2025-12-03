package in.bored.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import in.bored.api.dto.ContentItemResponse;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class GeminiService {

    private static final String GEMINI_API_KEY = "AIzaSyCRj5v0Y0-trJAneMqWCjG-2mTC1jxLf3g";
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    // Using the specific preview model available to this key
    private static final String MODEL = "gemini-2.5-flash-lite-preview-09-2025";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GeminiService() {
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newHttpClient();
    }

    public List<ContentItemResponse> generateContent(String topicName, String categoryName, int count) {
        try {
            String prompt = buildPrompt(topicName, categoryName, count);
            String requestBody = buildRequestBody(prompt);

            String url = String.format("%s/models/%s:generateContent?key=%s", GEMINI_BASE_URL, MODEL, GEMINI_API_KEY);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseGeminiResponse(response.body(), topicName);
            } else {
                System.err.println("Gemini API Error: " + response.statusCode() + " " + response.body());
                return Collections.emptyList();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private String buildPrompt(String topic, String category, int count) {
        return String.format("""
                Generate %d interesting facts about "%s" (Category: %s).

                Return the response as a strictly valid JSON array of objects.
                Do NOT include markdown formatting (like ```json). Just the raw JSON array.

                Each object must have this structure:
                {
                    "content": "The fact content here...",
                    "topicName": "%s"
                }

                Make the content engaging, concise (1-3 sentences), and include an emoji at the start.
                """, count, topic, category, topic);
    }

    private String buildRequestBody(String prompt) {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode contents = root.putArray("contents");
        ObjectNode part = contents.addObject();
        ArrayNode parts = part.putArray("parts");
        parts.addObject().put("text", prompt);
        return root.toString();
    }

    private List<ContentItemResponse> parseGeminiResponse(String jsonResponse, String topicName) {
        List<ContentItemResponse> results = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode content = candidates.get(0).path("content");
                JsonNode parts = content.path("parts");
                if (parts.isArray() && parts.size() > 0) {
                    String text = parts.get(0).path("text").asText();

                    // Clean up potential markdown code blocks if Gemini ignores the instruction
                    text = text.trim();
                    if (text.startsWith("```json")) {
                        text = text.substring(7);
                    } else if (text.startsWith("```")) {
                        text = text.substring(3);
                    }
                    if (text.endsWith("```")) {
                        text = text.substring(0, text.length() - 3);
                    }
                    text = text.trim();

                    JsonNode factsArray = objectMapper.readTree(text);
                    if (factsArray.isArray()) {
                        int index = 0;
                        for (JsonNode factNode : factsArray) {
                            ContentItemResponse item = new ContentItemResponse();
                            item.setId(System.currentTimeMillis() + index); // Fake ID
                            item.setTopicName(topicName);
                            // item.setTopicId(null); // Optional, or set a dummy ID
                            item.setTopicEmoji(null);
                            item.setContent(factNode.path("content").asText());
                            item.setContentIndex(index++);
                            item.setCreatedAt(java.time.OffsetDateTime.now());

                            results.add(item);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing Gemini response: " + e.getMessage());
        }
        return results;
    }
}
