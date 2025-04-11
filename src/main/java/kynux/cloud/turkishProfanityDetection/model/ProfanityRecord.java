package kynux.cloud.turkishProfanityDetection.model;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Bir küfür tespiti için kayıt modeli.
 * Yeni minecraft-check endpoint yapısına uygun olarak güncellenmiştir.
 */
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
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Yeni bir küfür kaydı oluşturur.
     *
     * @param playerId Oyuncu UUID
     * @param playerName Oyuncu adı
     * @param word Tespit edilen küfür kelimesi
     * @param category Küfür kategorisi
     * @param severityLevel Küfür şiddet seviyesi (1-5)
     * @param detectedWords Tespit edilen tüm kelimeler listesi
     * @param originalMessage Orijinal mesaj
     * @param aiDetected Yapay zeka tarafından tespit edildi mi
     * @param confidence Tespit güvenilirliği (0.0-1.0)
     * @param model Kullanılan AI modeli
     * @param actionRecommendation Önerilen aksiyon (warn, mute, kick, ban)
     * @param isSafeForMinecraft Minecraft için güvenli mi
     */
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
            boolean isSafeForMinecraft) {
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
    }
    
    /**
     * Geriye uyumluluk için yardımcı constructor.
     * Yeni alanlar için varsayılan değerler kullanır.
     *
     * @param playerId Oyuncu UUID
     * @param playerName Oyuncu adı
     * @param word Tespit edilen küfür kelimesi
     * @param category Küfür kategorisi
     * @param severityLevel Küfür şiddet seviyesi (1-5)
     * @param detectedWords Tespit edilen tüm kelimeler listesi
     * @param originalMessage Orijinal mesaj
     * @param aiDetected Yapay zeka tarafından tespit edildi mi
     */
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
            originalMessage, aiDetected, 0.0, "", "", false);
    }
    
    /**
     * Veritabanından yüklenen bir küfür kaydı oluşturur.
     *
     * @param playerId Oyuncu UUID
     * @param playerName Oyuncu adı
     * @param word Tespit edilen küfür kelimesi
     * @param category Küfür kategorisi
     * @param severityLevel Küfür şiddet seviyesi (1-5)
     * @param detectedWords Tespit edilen tüm kelimeler listesi
     * @param originalMessage Orijinal mesaj
     * @param aiDetected Yapay zeka tarafından tespit edildi mi
     * @param timestamp Kaydın oluşturulma zamanı
     * @param confidence Tespit güvenilirliği (0.0-1.0)
     * @param model Kullanılan AI modeli
     * @param actionRecommendation Önerilen aksiyon (warn, mute, kick, ban)
     * @param isSafeForMinecraft Minecraft için güvenli mi
     */
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
            boolean isSafeForMinecraft) {
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
    }
    
    /**
     * Geriye uyumluluk için veritabanından yüklenen bir küfür kaydı oluşturur.
     *
     * @param playerId Oyuncu UUID
     * @param playerName Oyuncu adı
     * @param word Tespit edilen küfür kelimesi
     * @param category Küfür kategorisi
     * @param severityLevel Küfür şiddet seviyesi (1-5)
     * @param detectedWords Tespit edilen tüm kelimeler listesi
     * @param originalMessage Orijinal mesaj
     * @param aiDetected Yapay zeka tarafından tespit edildi mi
     * @param timestamp Kaydın oluşturulma zamanı
     */
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
            originalMessage, aiDetected, timestamp, 0.0, "", "", false);
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
    
    @Override
    public String toString() {
        return String.format(
                "[%s] %s: '%s' (%s) - Şiddet: %d, AI: %b, Model: %s, Güven: %.2f, Öneri: %s",
                getFormattedTimestamp(),
                playerName,
                word,
                category,
                severityLevel,
                aiDetected,
                getModel(),
                confidence,
                getActionRecommendation()
        );
    }
}
