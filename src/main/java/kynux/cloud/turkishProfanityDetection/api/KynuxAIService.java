package kynux.cloud.turkishProfanityDetection.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import kynux.cloud.turkishProfanityDetection.TurkishProfanityDetection;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class KynuxAIService {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client;
    private final Gson gson;
    private final TurkishProfanityDetection plugin;
    private String apiUrl;
    private String apiKey;

    public KynuxAIService(TurkishProfanityDetection plugin) {
        this.plugin = plugin;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
        loadConfig();
    }

    private void loadConfig() {
        this.apiUrl = plugin.getConfig().getString("kynux_api.url", "https://api.kynux.cloud/api/v1/chat/completion");
        this.apiKey = plugin.getConfig().getString("kynux_api.key", "");

        if (apiKey == null || apiKey.isEmpty()) {
            plugin.getLogger().warning("Kynux API anahtarı config.yml dosyasında bulunamadı veya boş.");
        }
        if (apiUrl == null || apiUrl.isEmpty()) {
            plugin.getLogger().warning("Kynux API URL'si config.yml dosyasında bulunamadı veya boş.");
            this.apiUrl = "https://api.kynux.cloud/api/v1/chat/completion";
        }
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        loadConfig();
    }

    @Nullable
    public KynuxAIResponse getChatCompletion(String userMessage) {
        if (apiKey == null || apiKey.isEmpty()) {
            plugin.getLogger().severe("Kynux API anahtarı yapılandırılmamış. İstek gönderilemiyor.");
            return null;
        }
        if (apiUrl == null || apiUrl.isEmpty()) {
            plugin.getLogger().severe("Kynux API URL'si yapılandırılmamış. İstek gönderilemiyor.");
            return null;
        }

        String systemPrompt = "You are a highly advanced profanity and content moderation AI for a Minecraft server chat. " +
                "Your primary goal is to analyze user messages and identify any content that violates server rules, " +
                "including profanity, hate speech, sexual content, severe insults, spam, threats, or other inappropriate language. " +
                "Consider common ways users try to bypass filters, such as using special characters, numbers, or misspellings. " +
                "When analyzing a message, you MUST provide your response strictly in the following JSON format. " +
                "Do NOT include any other text, explanations, or conversational elements outside of this JSON structure. " +
                "Ensure all string values in the JSON are properly escaped if they contain special characters like quotes. " +
                "The 'detected_word' should be the actual problematic segment from the user's message. " +
                "The 'analysis_details' should be a concise, professional explanation (max 150 characters). " +
                "If the message is clean, all relevant fields should reflect that (e.g., is_profane: false, severity: 0, category: 'none').\n\n" +
                "JSON Structure:\n" +
                "{\n" +
                "  \"is_profane\": boolean,\n" +
                "  \"is_safe_for_minecraft\": boolean,\n" +
                "  \"severity\": int,\n" +
                "  \"category\": \"string\",\n" +
                "  \"detected_word\": \"string\",\n" +
                "  \"action_recommendation\": \"string\",\n" +
                "  \"analysis_details\": \"string\"\n" +
                "}";

        JsonObject messageObject = new JsonObject();
        messageObject.addProperty("role", "user");
        messageObject.addProperty("content", userMessage);

        JsonObject systemMessageObject = new JsonObject();
        systemMessageObject.addProperty("role", "system");
        systemMessageObject.addProperty("content", systemPrompt);

        java.util.List<JsonObject> messages = new java.util.ArrayList<>();
        messages.add(systemMessageObject);
        messages.add(messageObject);
        
        JsonObject payload = new JsonObject();
        payload.addProperty("model", plugin.getConfig().getString("kynux_api.model", "gpt-3.5-turbo"));
        payload.add("messages", gson.toJsonTree(messages));

        RequestBody body = RequestBody.create(payload.toString(), JSON);
        Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                plugin.getLogger().warning("Kynux API'ye yapılan istek başarısız oldu: " + response.code() + " " + response.message());
                if (response.body() != null) {
                    plugin.getLogger().warning("Yanıt: " + response.body().string());
                }
                return null;
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                plugin.getLogger().warning("Kynux API'den boş yanıt alındı.");
                return null;
            }

            String responseString = responseBody.string();
            
            try {
                JsonObject jsonResponseFromChat = gson.fromJson(responseString, JsonObject.class);
                if (jsonResponseFromChat.has("choices")) {
                    JsonObject choice = jsonResponseFromChat.getAsJsonArray("choices").get(0).getAsJsonObject();
                    if (choice.has("message")) {
                        JsonObject messageContentObj = choice.getAsJsonObject("message");
                        if (messageContentObj.has("content")) {
                            String actualJsonResponse = messageContentObj.get("content").getAsString();
                            return gson.fromJson(actualJsonResponse, KynuxAIResponse.class);
                        }
                    }
                }
                plugin.getLogger().warning("Kynux API chat yanıt formatı beklenenden farklı (choices/message/content eksik): " + responseString);
                return null;
            } catch (JsonSyntaxException e) {
                plugin.getLogger().severe("Kynux AI'nın döndürdüğü JSON yanıtı parse edilirken hata oluştu: " + e.getMessage());
                plugin.getLogger().info("Alınan Ham Yanıt: " + responseString);
                return null;
            }

        } catch (IOException e) {
            plugin.getLogger().severe("Kynux API'ye bağlanırken bir hata oluştu: " + e.getMessage());
            return null;
        }
    }
}
