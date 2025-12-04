package in.bored.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import in.bored.api.dto.ContentItemResponse;
import in.bored.api.dto.QuizResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Service
public class GeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    private static final String MODEL = "gemini-2.5-flash-lite-preview-09-2025";

    @Value("${gemini.api.key}")
    private String GEMINI_API_KEY;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public List<ContentItemResponse> generateContent(String topicName, String categoryName, int count) {
        try {
            String prompt = String.format(
                    "Generate %d interesting, unique, and fun facts about %s (Category: %s). " +
                            "Each fact should be concise (1-2 sentences) and include an emoji at the start. " +
                            "Format the output as a JSON array of strings.",
                    count, topicName, categoryName);

            String responseText = sendMessage(topicName, prompt);
            if (responseText == null)
                return List.of();

            // Parse JSON array of strings
            JsonNode root = objectMapper.readTree(responseText);
            List<ContentItemResponse> results = new ArrayList<>();

            // Handle if response is wrapped in markdown code block
            if (root.isTextual()) {
                // Try to parse the text if it's a string containing JSON
                String text = root.asText();
                // clean markdown
                if (text.startsWith("```json"))
                    text = text.substring(7);
                if (text.startsWith("```"))
                    text = text.substring(3);
                if (text.endsWith("```"))
                    text = text.substring(0, text.length() - 3);
                root = objectMapper.readTree(text.trim());
            }

            if (root.isArray()) {
                for (JsonNode node : root) {
                    ContentItemResponse item = new ContentItemResponse();
                    item.setContent(node.asText());
                    item.setTopicName(topicName);
                    item.setCategoryName(categoryName);
                    item.setSource("Gemini");
                    results.add(item);
                }
            }
            return results;

        } catch (Exception e) {
            logger.error("Error generating content", e);
            return List.of();
        }
    }

    public List<QuizResponse> generateQuiz(String topicName, int count, int setNumber) {
        String prompt = String.format(
                "Generate exactly %d multiple-choice quiz questions about **%s** for **Set %d**.\n" +
                        "The questions must be unique, non-trivial, and completely distinct from any previous questions.\n"
                        +
                        "MOBILE CONSTRAINTS:\n" +
                        "- Short questions (max ~120 chars).\n" +
                        "- 4 options (max ~35 chars).\n" +
                        "- JSON output ONLY: {\"questions\": [{\"question\": \"...\", \"options\": [\"...\"], \"correct_answer\": \"...\"}]}",
                count, topicName, setNumber);

        try {
            String responseText = sendMessage(topicName, prompt);
            if (responseText == null)
                return List.of();
            return parseGeminiQuizResponse(responseText, topicName);
        } catch (Exception e) {
            logger.error("Error generating quiz", e);
            return List.of();
        }
    }

    public List<QuizResponse> generatePuzzle(int count) {
        String prompt = """
                Generate exactly %d new and unique generic puzzles.
                Each puzzle must:
                - Have a SHORT question (max ~100 characters, 1 line, no line breaks).
                - Have EXACTLY 4 distinct options, each max ~30 characters.
                - Have EXACTLY one correct answer.
                The response MUST be a single JSON object in this exact shape:
                {
                  "questions": [
                    {
                      "question": "short question here",
                      "options": ["Option A", "Option B", "Option C", "Option D"],
                      "answer": "exact text of the correct option"
                    }
                  ]
                }
                Rules:
                - Puzzles must be simple, fun and suitable for a casual mobile app.
                - Avoid offensive or adult content.
                - DO NOT include any introductory or explanatory text outside the JSON object.
                """.formatted(count);

        try {
            String responseText = sendMessage("Generic Puzzles", prompt);
            if (responseText == null)
                return List.of();
            return parseGeminiQuizResponse(responseText, "Generic Puzzles");
        } catch (Exception e) {
            logger.error("Error generating puzzles", e);
            return List.of();
        }
    }

    private String sendMessage(String topic, String prompt) throws IOException, InterruptedException {
        String safePrompt = prompt.replace("\"", "\\\"").replace("\n", "\\n");
        String requestBody = """
                {
                  "contents": [{
                    "parts": [{"text": "%s"}]
                  }]
                }
                """.formatted(safePrompt);

        String url = String.format("%s/models/%s:generateContent?key=%s", GEMINI_BASE_URL, MODEL, GEMINI_API_KEY);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            try {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && candidates.size() > 0) {
                    JsonNode content = candidates.get(0).path("content");
                    JsonNode parts = content.path("parts");
                    if (parts.isArray() && parts.size() > 0) {
                        return parts.get(0).path("text").asText();
                    }
                }
            } catch (Exception e) {
                logger.error("Error parsing Gemini response body", e);
            }
            return null;
        } else {
            logger.error("❌ Gemini API Error for topic '{}': {} {}", topic, response.statusCode(), response.body());
            return null;
        }
    }

    private List<QuizResponse> parseGeminiQuizResponse(String text, String categoryName) {
        List<QuizResponse> responses = new ArrayList<>();
        try {
            text = text.trim();
            if (text.startsWith("```json"))
                text = text.substring(7);
            if (text.startsWith("```"))
                text = text.substring(3);
            if (text.endsWith("```"))
                text = text.substring(0, text.length() - 3);
            text = text.trim();

            int startIndex = text.indexOf('{');
            int endIndex = text.lastIndexOf('}') + 1;

            if (startIndex != -1 && endIndex != -1) {
                String cleanJson = text.substring(startIndex, endIndex);
                JsonNode root = objectMapper.readTree(cleanJson);
                JsonNode questionsNode = root.get("questions");

                if (questionsNode != null && questionsNode.isArray()) {
                    for (JsonNode node : questionsNode) {
                        QuizResponse dto = new QuizResponse();
                        dto.setCategoryName(categoryName);
                        dto.setQuestion(node.path("question").asText());

                        String answer = node.path("answer").asText();
                        if (answer.isEmpty()) {
                            answer = node.path("correct_answer").asText();
                        }
                        dto.setAnswer(answer);

                        JsonNode optionsNode = node.path("options");
                        if (optionsNode.isArray()) {
                            List<String> optionsList = new ArrayList<>();
                            for (JsonNode opt : optionsNode) {
                                optionsList.add(opt.asText());
                            }
                            dto.setOptions(objectMapper.writeValueAsString(optionsList));
                        }

                        dto.setDifficultyLevel(1);
                        responses.add(dto);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing Gemini quiz/puzzle response: {}", e.getMessage());
        }
        return responses;
    }
}
