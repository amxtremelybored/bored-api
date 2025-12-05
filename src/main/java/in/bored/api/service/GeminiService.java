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
                    "Provide exactly %d diverse, fun, and interesting facts about %s. " +
                            "IMPORTANT: Start each fact with '### Fact X:' followed directly by an emoji and then the content. "
                            +
                            "Each fact should be concise (1-3 sentences), engaging, and include at least one emoji. " +
                            "Use markdown formatting with bullets and short paragraphs. " +
                            "DO NOT include any introductory text or header. " +
                            "DO NOT say 'Here are 10 facts about...' - just start with the first fact immediately. " +
                            "Format example: '### Fact 1: 🎭 Shakespeare wrote 37 plays...' " +
                            "Make all facts unique, interesting, and visually appealing with emojis.",
                    count, topicName);

            String responseText = sendMessage(topicName, prompt);
            if (responseText == null)
                return List.of();

            return parseContent(responseText, topicName, categoryName);

        } catch (Exception e) {
            logger.error("Error generating content", e);
            return List.of();
        }
    }

    public String generateContent(String topicName, int count) {
        try {
            String prompt = String.format(
                    "Generate %d interesting, unique, and fun facts about %s. " +
                            "Each fact should be concise (1-2 sentences) and include an emoji at the start. " +
                            "Format the output as a JSON array of strings.",
                    count, topicName);

            return sendMessage(topicName, prompt);
        } catch (Exception e) {
            logger.error("Error generating content", e);
            return null;
        }
    }

    public List<ContentItemResponse> parseContent(String jsonText, String topicName) {
        return parseContent(jsonText, topicName, "General");
    }

    public List<ContentItemResponse> parseContent(String jsonText, String topicName, String categoryName) {
        if (jsonText == null || jsonText.trim().isEmpty()) {
            return List.of();
        }

        List<ContentItemResponse> results = new ArrayList<>();

        // 1. Try to parse as JSON first (backward compatibility or if Gemini decides to
        // wrap in JSON)
        try {
            String cleanText = cleanJson(jsonText);
            if (!cleanText.isEmpty()) {
                JsonNode root = objectMapper.readTree(cleanText);

                if (root.isTextual()) {
                    // Handle double-encoded JSON if happens
                    String inner = root.asText();
                    try {
                        root = objectMapper.readTree(cleanJson(inner));
                    } catch (Exception ignored) {
                    }
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
                    return results;
                }
            }
        } catch (Exception e) {
            // Not JSON, ignore and fall through to text parsing
            logger.debug("Content is not JSON, trying text parsing. Error: {}", e.getMessage());
        }

        // 2. Parse as custom Markdown Text (### Fact X:)
        // Regex to split by "### Fact <number>:" or just "Fact <number>:"
        String[] parts = jsonText.split("###\\s*Fact\\s*\\d+:|Fact\\s*\\d+:");

        for (String part : parts) {
            String cleanPart = part.trim();
            // Filter out empty parts or intro text
            if (!cleanPart.isEmpty() && cleanPart.length() > 10) {
                // Double check it's not just "Here are facts"
                if (cleanPart.toLowerCase().startsWith("here are") || cleanPart.toLowerCase().startsWith("sure,")) {
                    continue;
                }

                ContentItemResponse item = new ContentItemResponse();
                item.setContent(cleanPart);
                item.setTopicName(topicName);
                item.setCategoryName(categoryName);
                item.setSource("Gemini");
                results.add(item);
            }
        }

        return results;
    }

    private String cleanJson(String text) {
        if (text == null)
            return "";
        text = text.trim();

        // Find start and end of JSON content
        int arrayStart = text.indexOf('[');
        int objectStart = text.indexOf('{');
        int start = -1;

        if (arrayStart != -1 && objectStart != -1) {
            start = Math.min(arrayStart, objectStart);
        } else if (arrayStart != -1) {
            start = arrayStart;
        } else if (objectStart != -1) {
            start = objectStart;
        }

        if (start != -1) {
            int arrayEnd = text.lastIndexOf(']');
            int objectEnd = text.lastIndexOf('}');
            int end = Math.max(arrayEnd, objectEnd);

            if (end > start) {
                return text.substring(start, end + 1);
            }
        }

        // Fallback cleanup if proper start/end not found
        if (text.startsWith("```json"))
            text = text.substring(7);
        if (text.startsWith("```"))
            text = text.substring(3);
        if (text.endsWith("```"))
            text = text.substring(0, text.length() - 3);
        return text.trim();
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

    public List<in.bored.api.model.JokeContent> generateJoke(int count) {
        String prompt = """
                You are a witty comedian. Provide a list of simple, clean, and funny jokes suitable for all ages.
                Each joke must be easy to understand and make people smile.
                Generate exactly %d new and unique general jokes.
                Each joke must have a short setup and a clear punchline.
                Avoid offensive or adult content.
                The response MUST be a JSON array formatted exactly according to the schema:
                [{"setup": "string", "punchline": "string"}]
                DO NOT include any text outside the JSON block.
                """.formatted(count);

        try {
            String responseText = sendMessage("Generic Jokes", prompt);
            if (responseText == null)
                return List.of();
            return parseGeminiJokeResponse(responseText);
        } catch (Exception e) {
            logger.error("Error generating jokes", e);
            return List.of();
        }
    }

    private List<in.bored.api.model.JokeContent> parseGeminiJokeResponse(String text) {
        List<in.bored.api.model.JokeContent> jokes = new ArrayList<>();
        try {
            String cleanText = cleanJson(text);
            JsonNode root = objectMapper.readTree(cleanText);
            if (root.isArray()) {
                for (JsonNode node : root) {
                    in.bored.api.model.JokeContent joke = new in.bored.api.model.JokeContent();
                    joke.setSetup(node.path("setup").asText());
                    joke.setPunchline(node.path("punchline").asText());
                    joke.setSource("Gemini");
                    jokes.add(joke);
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing Gemini joke response: {}", e.getMessage());
        }
        return jokes;
    }

    public List<String> generateFun(int count) {
        String prompt = """
                You are a witty comedian. Provide a list of clean, witty, hilarious anecdotes and gag-type jokes.
                Generate exactly %d new and unique items.
                Each item should be concise (1-4 sentences), engaging, and include at least one emoji.
                Avoid "Why did..." style jokes.
                The response MUST be a JSON array of strings.
                Example: ["🎭 When the mouse went to the bar...", "Another anecdote..."]
                DO NOT include any text outside the JSON block.
                """.formatted(count);

        try {
            String responseText = sendMessage("Anecdotes", prompt);
            if (responseText == null)
                return List.of();
            return parseGeminiStringListResponse(responseText);
        } catch (Exception e) {
            logger.error("Error generating fun content", e);
            return List.of();
        }
    }

    public List<String> generateDoYouKnow(int count) {
        String prompt = """
                Generate exactly %d unique "Do You Know" facts about general knowledge (Space, History, Science, Nature, etc.).
                Each fact must be concise, interesting, and formatted as a single, engaging sentence or short paragraph (max 3 sentences).
                The response MUST be a JSON array of strings.
                Example: ["Did you know that honey never spoils?", "Another fact..."]
                DO NOT include any text outside the JSON block.
                """
                .formatted(count);

        try {
            String responseText = sendMessage("General Facts", prompt);
            if (responseText == null)
                return List.of();
            return parseGeminiStringListResponse(responseText);
        } catch (Exception e) {
            logger.error("Error generating do you know content", e);
            return List.of();
        }
    }

    public List<String> generateHealthWellnessTip(String topic, int count) {
        String prompt = """
                Provide exactly %d simple, practical and safe everyday tips about %s (general lifestyle only – no medical diagnosis or treatment).
                Each tip should be concise (1–3 sentences), encouraging, and include at least one emoji.
                The response MUST be a JSON array of strings.
                Example: ["💧 Drink water regularly...", "Another tip..."]
                DO NOT include any text outside the JSON block.
                """
                .formatted(count, topic);

        try {
            String responseText = sendMessage(topic, prompt);
            if (responseText == null)
                return List.of();
            return parseGeminiStringListResponse(responseText);
        } catch (Exception e) {
            logger.error("Error generating health wellness content", e);
            return List.of();
        }
    }

    private List<String> parseGeminiStringListResponse(String text) {
        List<String> items = new ArrayList<>();
        try {
            String cleanText = cleanJson(text);
            JsonNode root = objectMapper.readTree(cleanText);
            if (root.isArray()) {
                for (JsonNode node : root) {
                    items.add(node.asText());
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing Gemini string list response: {}", e.getMessage());
        }
        return items;
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
            String cleanJson = cleanJson(text);
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
        } catch (Exception e) {
            logger.error("Error parsing Gemini quiz/puzzle response: {}", e.getMessage());
        }
        return responses;
    }
}
