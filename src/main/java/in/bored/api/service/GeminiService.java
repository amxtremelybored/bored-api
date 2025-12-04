package in.bored.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import in.bored.api.dto.ContentItemResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);

    private static final String GEMINI_API_KEY = "AIzaSyCvoqBMLpY2WZBfgkfwHHuob41DjLFJork";
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

            logger.info("🤖 Calling Gemini API for topic: '{}', category: '{}', count: {}", topicName, categoryName,
                    count);

            String url = String.format("%s/models/%s:generateContent?key=%s", GEMINI_BASE_URL, MODEL, GEMINI_API_KEY);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                logger.info("✅ Gemini API response received successfully for topic: '{}'", topicName);
                return parseGeminiResponse(response.body(), topicName);
            } else {
                logger.error("❌ Gemini API Error: {} {}", response.statusCode(), response.body());
                return Collections.emptyList();
            }

        } catch (Exception e) {
            logger.error("❌ Exception while calling Gemini API", e);
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
            logger.error("❌ Error parsing Gemini response", e);
        }
        return results;
    }

    public List<in.bored.api.dto.QuizResponse> generateQuiz(String topicName, int count, int setNumber) {
        try {
            String prompt = buildQuizPrompt(topicName, count, setNumber);
            String requestBody = buildRequestBody(prompt);

            logger.info("🤖 Calling Gemini API for Quiz topic: '{}', count: {}", topicName, count);

            String url = String.format("%s/models/%s:generateContent?key=%s", GEMINI_BASE_URL, MODEL, GEMINI_API_KEY);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                logger.info("✅ Gemini API response received successfully for Quiz topic: '{}'", topicName);
                return parseGeminiQuizResponse(response.body(), topicName);
            } else {
                logger.error("❌ Gemini API Error: {} {}", response.statusCode(), response.body());
                return Collections.emptyList();
            }

        } catch (Exception e) {
            logger.error("❌ Exception while calling Gemini API for Quiz", e);
            return Collections.emptyList();
        }
    }

    private String buildQuizPrompt(String randomTopic, int count, int setNumber) {
        return String.format(
                """
                        Generate exactly %d multiple-choice quiz questions about **%s** for **Set %d**.
                        The questions must be unique, non-trivial, and completely distinct from any previous questions you may have generated. This set number ensures we get fresh content.

                        MOBILE CONSTRAINTS (VERY IMPORTANT):
                        - Each question MUST be short and fit on a phone screen easily.
                        - Max ~120 characters per question.
                        - No line breaks or paragraphs inside the question.
                        - Each option MUST be a short phrase (max ~35 characters).
                        - No line breaks or extra formatting in options.
                        - Do NOT prefix options with letters or numbers (no "A)", "1.", etc.).

                        The response MUST be a single JSON object in this exact shape:
                        {
                          "questions": [
                            {
                              "question": "The question text 1",
                              "options": ["Option A", "Option B", "Option C", "Option D"],
                              "correct_answer": "The text of the correct option"
                            }
                          ]
                        }

                        Rules:
                        - Questions must be simple, fun and suitable for a casual quiz app.
                        - Avoid offensive or adult content.
                        - DO NOT include any introductory or explanatory text outside the JSON object.
                        """,
                count, randomTopic, setNumber);
    }

    private List<in.bored.api.dto.QuizResponse> parseGeminiQuizResponse(String jsonResponse, String topicName) {
        List<in.bored.api.dto.QuizResponse> results = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode content = candidates.get(0).path("content");
                JsonNode parts = content.path("parts");
                if (parts.isArray() && parts.size() > 0) {
                    String text = parts.get(0).path("text").asText();

                    // Clean up potential markdown code blocks
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

                    JsonNode jsonRoot = objectMapper.readTree(text);
                    JsonNode questionsArray = jsonRoot.path("questions");

                    if (questionsArray.isArray()) {
                        for (JsonNode qNode : questionsArray) {
                            in.bored.api.dto.QuizResponse item = new in.bored.api.dto.QuizResponse();
                            // item.setId(...) - ID will be generated by DB
                            item.setCategoryName(topicName); // Using topic as category name for now
                            item.setQuestion(qNode.path("question").asText());
                            item.setAnswer(qNode.path("correct_answer").asText());

                            ArrayNode optionsNode = (ArrayNode) qNode.path("options");
                            item.setOptions(optionsNode.toString()); // Store as JSON string

                            item.setDifficultyLevel(1);
                            results.add(item);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("❌ Error parsing Gemini Quiz response", e);
        }
        return results;
    }
}
