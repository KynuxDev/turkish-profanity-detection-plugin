package kynux.cloud.turkishProfanityDetection.model;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public class ProfanityRecord {
    private final UUID playerId;
    private final String playerName;
    private final String word;
    private final String category;
    private final int severityLevel;
    private final List<String> detectedWords;
    private final String originalMessage;
    private final LocalDateTime timestamp;
    private final boolean aiDetected;
    private final double confidence;
    private final String model;
    private final String actionRecommendation;
    private final boolean isSafeForMinecraft;
    private final String analysisDetails;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public ProfanityRecord(
            @NotNull UUID playerId,
            @NotNull String playerName,
            @NotNull String word,
            @NotNull String category,
            int severityLevel,
            @NotNull List<String> detectedWords,
            @NotNull String originalMessage,
            boolean aiDetected,
            double confidence,
            String model,
            String actionRecommendation,
            boolean isSafeForMinecraft,
            String analysisDetails) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.word = word;
        this.category = category;
        this.severityLevel = severityLevel;
        this.detectedWords = detectedWords;
        this.originalMessage = originalMessage;
        this.timestamp = LocalDateTime.now();
        this.aiDetected = aiDetected;
        this.confidence = confidence;
        this.model = model;
        this.actionRecommendation = actionRecommendation;
        this.isSafeForMinecraft = isSafeForMinecraft;
        this.analysisDetails = analysisDetails;
    }
    
    public ProfanityRecord(
            @NotNull UUID playerId,
            @NotNull String playerName,
            @NotNull String word,
            @NotNull String category,
            int severityLevel,
            @NotNull List<String> detectedWords,
            @NotNull String originalMessage,
            boolean aiDetected) {
        this(playerId, playerName, word, category, severityLevel, detectedWords,
            originalMessage, aiDetected, 0.0, "", "", false, "");
    }
    
    public ProfanityRecord(
            @NotNull UUID playerId,
            @NotNull String playerName,
            @NotNull String word,
            @NotNull String category,
            int severityLevel,
            @NotNull List<String> detectedWords,
            @NotNull String originalMessage,
            boolean aiDetected,
            @NotNull LocalDateTime timestamp,
            double confidence,
            String model,
            String actionRecommendation,
            boolean isSafeForMinecraft,
            String analysisDetails) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.word = word;
        this.category = category;
        this.severityLevel = severityLevel;
        this.detectedWords = detectedWords;
        this.originalMessage = originalMessage;
        this.timestamp = timestamp;
        this.aiDetected = aiDetected;
        this.confidence = confidence;
        this.model = model;
        this.actionRecommendation = actionRecommendation;
        this.isSafeForMinecraft = isSafeForMinecraft;
        this.analysisDetails = analysisDetails;
    }
    
    public ProfanityRecord(
            @NotNull UUID playerId,
            @NotNull String playerName,
            @NotNull String word,
            @NotNull String category,
            int severityLevel,
            @NotNull List<String> detectedWords,
            @NotNull String originalMessage,
            boolean aiDetected,
            @NotNull LocalDateTime timestamp) {
        this(playerId, playerName, word, category, severityLevel, detectedWords,
            originalMessage, aiDetected, timestamp, 0.0, "", "", false, "");
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public String getWord() {
        return word;
    }
    
    public String getCategory() {
        return category;
    }
    
    public int getSeverityLevel() {
        return severityLevel;
    }
    
    public List<String> getDetectedWords() {
        return detectedWords;
    }
    
    public String getOriginalMessage() {
        return originalMessage;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public String getFormattedTimestamp() {
        return timestamp.format(DATE_FORMATTER);
    }
    
    public boolean isAiDetected() {
        return aiDetected;
    }
    
    public double getConfidence() {
        return confidence;
    }
    
    public String getModel() {
        return model != null ? model : "";
    }
    
    public String getActionRecommendation() {
        return actionRecommendation != null ? actionRecommendation : "";
    }
    
    public boolean isSafeForMinecraft() {
        return isSafeForMinecraft;
    }

    public String getAnalysisDetails() {
        return analysisDetails != null ? analysisDetails : "";
    }
    
    @Override
    public String toString() {
        return String.format(
                "[%s] %s: '%s' (Kategori: %s, Şiddet: %d, AI: %b, Model: %s, Güven: %.2f, Öneri: %s, Detay: %s)",
                getFormattedTimestamp(),
                playerName,
                word,
                category,
                severityLevel,
                aiDetected,
                getModel(),
                confidence,
                getActionRecommendation(),
                getAnalysisDetails()
        );
    }
}
