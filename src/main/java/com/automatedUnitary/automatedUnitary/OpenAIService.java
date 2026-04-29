package com.automatedUnitary.automatedUnitary;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;

@Service
public class OpenAIService {

    @Value("${openai.api.key}")
    private String apiKey;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    public String generateTests(String javaCode, String testType) throws IOException {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", "gpt-3.5-turbo");
        body.put("max_tokens", 2000);

        ArrayNode messages = mapper.createArrayNode();

        ObjectNode systemMessage = mapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", buildSystemPrompt(testType));
        messages.add(systemMessage);

        ObjectNode userMessage = mapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", "Analiza este código Java y genera los tests:\n\n" + javaCode);
        messages.add(userMessage);

        body.set("messages", messages);

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .post(RequestBody.create(mapper.writeValueAsString(body), MediaType.get("application/json")))
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            JsonNode json = mapper.readTree(responseBody);
            if (json.has("error")) {
                return "Error de API: " + json.get("error").get("message").asText();
            }
            return json.get("choices").get(0).get("message").get("content").asText();
        }
    }

    private String buildSystemPrompt(String testType) {
        return switch (testType) {
            case "unit" -> """
                Eres un experto en QA y testing de Java. 
                Genera tests unitarios completos usando JUnit 5 y Mockito.
                Incluye casos de éxito, casos de error y casos borde.
                El código debe ser compilable y seguir buenas prácticas.
                Responde SOLO con el código Java, sin explicaciones adicionales.
                """;
            case "integration" -> """
                Eres un experto en QA y testing de Java con Spring Boot.
                Genera tests de integración usando @SpringBootTest y MockMvc.
                Incluye tests de endpoints REST si los hay.
                El código debe ser compilable y seguir buenas prácticas.
                Responde SOLO con el código Java, sin explicaciones adicionales.
                """;
            case "plain" -> """
                Eres un experto en QA y documentación de software.
                Genera casos de prueba en texto plano en formato estructurado.
                Para cada caso incluye: ID, Descripción, Precondiciones, Pasos, Resultado esperado.
                Cubre casos positivos, negativos y casos borde.
                Responde en el idioma que predomine en los comentarios del codigo y si no hay comentarios responde en inglés.
                """;
            default -> "Genera tests completos para el código Java proporcionado.";
        };
    }
}