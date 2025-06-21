package kynux.cloud.turkishProfanityDetection.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import okhttp3.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProfanityApiService {
    private final OkHttpClient client;
    private final Gson gson;
    private final String apiUrl;
    private final String apiKey;
    private final boolean useAI;
    private final String model;
    private final double temperature;
    private final int maxTokens;
    private final Logger logger;
    private final int timeout;
    
    private static final String SYSTEM_PROMPT = 
        "Sen bir Minecraft sunucusu için küfür, hakaret ve uygunsuz içerik tespit sistemisin.\n\n" +
        "GÖREVİN:\n" +
        "- Türkçe metinlerdeki küfür, hakaret, cinsel içerik, tehdit ve uygunsuz ifadeleri tespit et\n" +
        "- Minecraft sunucusu ortamına uygun olmayan her türlü içeriği yakala\n" +
        "- Gizli/dolaylı küfürleri, sembol/rakam ile yazılan küfürleri de tespit et\n\n" +
        "TESPIT KRİTERLERİ:\n" +
        "1. Açık küfür ve hakaret\n" +
        "2. Cinsel içerikli ifadeler\n" +
        "3. Tehdit ve şiddet içeren mesajlar\n" +
        "4. Ayrımcılık ve nefret söylemi\n" +
        "5. Spam ve rahatsız edici içerik\n" +
        "6. Sembol/rakam ile gizlenmiş küfürler (örn: s1k, @mq, vb.)\n\n" +
        "SEVİYE TANIMLAMALARI:\n" +
        "1 = Hafif uygunsuz (argo, hafif hakaret)\n" +
        "2 = Orta düzeyde uygunsuz (küfür, hakaret)\n" +
        "3 = Ağır uygunsuz (ağır küfür, tehdit)\n" +
        "4 = Çok ağır (cinsel içerik, ağır tehdit)\n" +
        "5 = Maksimum ağır (aşırı ağır küfür, nefret söylemi)\n\n" +
        "YANIT FORMATI (sadece JSON döndür):\n" +
        "{\n" +
        "  \"isSwear\": true/false,\n" +
        "  \"severityLevel\": 1-5,\n" +
        "  \"category\": \"küfür/hakaret/tehdit/cinsel/ayrımcılık/spam\",\n" +
        "  \"detectedWords\": [\"tespit edilen kelimeler\"],\n" +
        "  \"reason\": \"Tespit sebebi kısa açıklama\"\n" +
        "}";

    public ProfanityApiService(@NotNull FileConfiguration config, @NotNull Logger logger) {
        this.logger = logger;
        
        this.apiUrl = config.getString("api.url", "https://ai.kynux.cloud/v1/chat/completions");
        this.apiKey = config.getString("api.api-key", "");
        this.timeout = config.getInt("api.timeout", 30000); // Varsayılan 30 saniye
        this.useAI = config.getBoolean("api.ai.use", true);
        this.model = config.getString("api.ai.model", "grok-3-mini");
        this.temperature = config.getDouble("api.ai.temperature", 0.1);
        this.maxTokens = config.getInt("api.ai.max-tokens", 200);
        
        this.client = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                .readTimeout(timeout, TimeUnit.MILLISECONDS)
                .writeTimeout(timeout, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(false) 
                .build();
                
        this.gson = new GsonBuilder().create();
        
        logger.info("Küfür tespit AI API servisi başlatıldı: " + apiUrl);
        logger.info("AI Model: " + model);
        logger.info("API timeout süresi: " + timeout + "ms");
        
        if (apiKey.isEmpty()) {
            logger.warning("API anahtarı yapılandırılmamış! API istekleri başarısız olabilir.");
        }
    }

    public CompletableFuture<ProfanityResponse> checkText(@NotNull String text) {
        return CompletableFuture.supplyAsync(new Supplier<ProfanityResponse>() {
            @Override
            public ProfanityResponse get() {
                try {
                    JsonObject requestBody = createOpenAIRequest(text);
                    
                    RequestBody body = RequestBody.create(
                        requestBody.toString(),
                        MediaType.parse("application/json; charset=utf-8")
                    );
                    
                    Request.Builder requestBuilder = new Request.Builder()
                            .url(apiUrl)
                            .post(body)
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Accept", "application/json");
                    
                    if (!apiKey.isEmpty()) {
                        requestBuilder.addHeader("Authorization", "Bearer " + apiKey);
                    }
                    
                    Request request = requestBuilder.build();

                    try (Response response = client.newCall(request).execute()) {
                        if (!response.isSuccessful()) {
                            int statusCode = response.code();
                            logger.warning("AI API yanıt vermedi! Durum kodu: " + statusCode);
                            return ProfanityResponse.createServerError(statusCode);
                        }

                        String responseBody = response.body().string();
                        return parseOpenAIResponse(responseBody);
                    }
                } catch (SocketTimeoutException e) {
                    logger.log(Level.WARNING, "AI API isteği sırasında zaman aşımı: " + e.getMessage());
                    return ProfanityResponse.createTimeoutError(e.getMessage());
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "AI API bağlantı hatası: " + e.getMessage(), e);
                    return ProfanityResponse.createConnectionError(e.getMessage());
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "AI API istek hatası: " + e.getMessage(), e);
                    return ProfanityResponse.createApiError(e.getMessage());
                }
            }
        });
    }

    private JsonObject createOpenAIRequest(String text) {
        JsonObject request = new JsonObject();
        request.addProperty("model", model);
        request.addProperty("temperature", temperature);
        request.addProperty("max_tokens", maxTokens);
        
        JsonArray messages = new JsonArray();
        
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", SYSTEM_PROMPT);
        messages.add(systemMessage);
        
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", text);
        messages.add(userMessage);
        
        request.add("messages", messages);
        
        return request;
    }

    private ProfanityResponse parseOpenAIResponse(String responseBody) {
        try {
            JsonObject response = JsonParser.parseString(responseBody).getAsJsonObject();
            
            if (!response.has("choices") || response.getAsJsonArray("choices").size() == 0) {
                logger.warning("AI API geçersiz yanıt formatı döndürdü");
                return ProfanityResponse.createApiError("Geçersiz API yanıt formatı");
            }
            
            JsonObject choice = response.getAsJsonArray("choices").get(0).getAsJsonObject();
            JsonObject message = choice.getAsJsonObject("message");
            String content = message.get("content").getAsString();
            
            JsonObject aiResult = JsonParser.parseString(content).getAsJsonObject();
            
            ProfanityResponse profanityResponse = new ProfanityResponse();
            profanityResponse.setSuccess(true);
            
            ProfanityResponse.Result result = new ProfanityResponse.Result();
            result.setSwear(aiResult.get("isSwear").getAsBoolean());
            result.setAiDetected(true);
            
            if (result.isSwear()) {
                ProfanityResponse.Details details = new ProfanityResponse.Details();
                details.setSeverityLevel(aiResult.get("severityLevel").getAsInt());
                details.setCategory(aiResult.get("category").getAsString());
                
                JsonArray detectedWordsArray = aiResult.getAsJsonArray("detectedWords");
                String[] detectedWords = new String[detectedWordsArray.size()];
                for (int i = 0; i < detectedWordsArray.size(); i++) {
                    detectedWords[i] = detectedWordsArray.get(i).getAsString();
                }
                details.setDetectedWords(java.util.Arrays.asList(detectedWords));
                
                if (detectedWords.length > 0) {
                    details.setWord(detectedWords[0]);
                }
                
                result.setDetails(details);
            }
            
            profanityResponse.setResult(result);
            return profanityResponse;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "AI API yanıtı parse edilirken hata: " + e.getMessage(), e);
            logger.warning("AI API yanıt içeriği: " + responseBody);
            return ProfanityResponse.createApiError("AI yanıtı parse edilemedi: " + e.getMessage());
        }
    }

}
