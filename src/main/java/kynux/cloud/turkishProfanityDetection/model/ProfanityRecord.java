package kynux.cloud.turkishProfanityDetection.model;

import java.time.LocalDateTime;
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
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public ProfanityRecord(
            @NotNull UUID playerId,
            @NotNull String playerName,
            @NotNull String word,
            @NotNull String category,
            int severityLevel,
            @NotNull List<String> detectedWords,
            @NotNull String originalMessage,
            boolean aiDetected) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.word = word;
        this.category = category;
        this.severityLevel = severityLevel;
        this.detectedWords = detectedWords;
        this.originalMessage = originalMessage;
        this.timestamp = LocalDateTime.now();
        this.aiDetected = aiDetected;
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
        this.playerId = playerId;
        this.playerName = playerName;
        this.word = word;
        this.category = category;
        this.severityLevel = severityLevel;
        this.detectedWords = detectedWords;
        this.originalMessage = originalMessage;
        this.timestamp = timestamp;
        this.aiDetected = aiDetected;
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
    
    @Override
    public String toString() {
        return String.format(
                "[%s] %s: '%s' (%s) - Şiddet: %d, AI: %b",
                getFormattedTimestamp(),
                playerName,
                word,
                category,
                severityLevel,
                aiDetected
        );
    }
}
