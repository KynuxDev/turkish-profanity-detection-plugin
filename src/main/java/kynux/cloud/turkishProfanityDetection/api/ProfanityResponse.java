package kynux.cloud.turkishProfanityDetection.api;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * API'den dönen küfür tespiti yanıtı için model sınıfı.
 * Yeni minecraft-check endpoint yanıtları için güncellenmiştir.
 */
public class ProfanityResponse {
    private boolean success;
    private Result result;
    private String message;
    private String error;
    private ErrorType errorType;
    
    /**
     * Hata türlerini tanımlayan enum.
     */
    public enum ErrorType {
        NONE,
        TIMEOUT,
        CONNECTION_ERROR,
        API_ERROR,
        INVALID_RESPONSE,
        SERVER_ERROR,
        CIRCUIT_OPEN,
        UNKNOWN
    }
    
    /**
     * Varsayılan yapıcı - başarısız bir yanıt oluşturur.
     */
    public ProfanityResponse() {
        this.success = false;
        this.errorType = ErrorType.UNKNOWN;
    }
    
    /**
     * Zaman aşımı hatası için statik yardımcı metot.
     * 
     * @param message Hata mesajı
     * @return Yapılandırılmış hata yanıtı
     */
    public static ProfanityResponse createTimeoutError(String message) {
        ProfanityResponse response = new ProfanityResponse();
        response.success = false;
        response.error = "API zaman aşımı: " + message;
        response.message = "API yanıt vermedi (zaman aşımı)";
        response.errorType = ErrorType.TIMEOUT;
        return response;
    }
    
    /**
     * Bağlantı hatası için statik yardımcı metot.
     * 
     * @param message Hata mesajı
     * @return Yapılandırılmış hata yanıtı
     */
    public static ProfanityResponse createConnectionError(String message) {
        ProfanityResponse response = new ProfanityResponse();
        response.success = false;
        response.error = "API bağlantı hatası: " + message;
        response.message = "API'ye bağlanılamadı";
        response.errorType = ErrorType.CONNECTION_ERROR;
        return response;
    }
    
    /**
     * Genel API hatası için statik yardımcı metot.
     * 
     * @param message Hata mesajı
     * @return Yapılandırılmış hata yanıtı
     */
    public static ProfanityResponse createApiError(String message) {
        ProfanityResponse response = new ProfanityResponse();
        response.success = false;
        response.error = "API hatası: " + message;
        response.message = "API bir hata döndürdü";
        response.errorType = ErrorType.API_ERROR;
        return response;
    }
    
    /**
     * Sunucu hatası için statik yardımcı metot.
     * 
     * @param statusCode HTTP durum kodu
     * @return Yapılandırılmış hata yanıtı
     */
    public static ProfanityResponse createServerError(int statusCode) {
        ProfanityResponse response = new ProfanityResponse();
        response.success = false;
        response.error = "Sunucu hatası: HTTP " + statusCode;
        response.message = "API sunucusu hata döndürdü: " + statusCode;
        response.errorType = ErrorType.SERVER_ERROR;
        return response;
    }
    
    /**
     * Devre kesici açıkken oluşturulan hata yanıtı
     * 
     * @return Yapılandırılmış hata yanıtı
     */
    public static ProfanityResponse createCircuitOpenError() {
        ProfanityResponse response = new ProfanityResponse();
        response.success = false;
        response.error = "API devre kesici açık";
        response.message = "API istekleri şu anda engelleniyor, önceki hatalar nedeniyle";
        response.errorType = ErrorType.CIRCUIT_OPEN;
        return response;
    }

    public boolean isSuccess() {
        return success;
    }

    @Nullable
    public Result getResult() {
        return result;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    @Nullable
    public String getError() {
        return error;
    }
    
    /**
     * @return Hatanın türünü döndürür
     */
    @NotNull
    public ErrorType getErrorType() {
        return errorType != null ? errorType : ErrorType.NONE;
    }
    
    /**
     * @return Zaman aşımı hatası olup olmadığını kontrol eder
     */
    public boolean isTimeoutError() {
        return errorType == ErrorType.TIMEOUT;
    }
    
    /**
     * @return Bağlantı hatası olup olmadığını kontrol eder
     */
    public boolean isConnectionError() {
        return errorType == ErrorType.CONNECTION_ERROR;
    }
    
    /**
     * @return Devre kesici hatası olup olmadığını kontrol eder
     */
    public boolean isCircuitOpenError() {
        return errorType == ErrorType.CIRCUIT_OPEN;
    }

    public static class Result {
        private boolean isSwear;
        private String word;
        private String category;
        private int severityLevel;
        private double confidence;
        private String actionRecommendation;
        private String model;
        private String detectedAt;
        private boolean isSafeForMinecraft;
        private List<String> detectedWords; // Eski format için tutuyoruz

        public boolean isSwear() {
            return isSwear;
        }
        
        public boolean isSafeForMinecraft() {
            return isSafeForMinecraft;
        }
        
        public double getConfidence() {
            return confidence;
        }
        
        public String getModel() {
            return model != null ? model : "";
        }
        
        public String getDetectedAt() {
            return detectedAt != null ? detectedAt : "";
        }
        
        public String getActionRecommendation() {
            return actionRecommendation != null ? actionRecommendation : "";
        }

        /**
         * Tespit edilen küfür detaylarını döndürür (geriye uyumluluk için).
         * NOT: Bu metod artık bir Details nesnesi döndürmez çünkü yeni API
         * formatında detaylar ana Result objesine taşınmıştır.
         *
         * @return Küfür detayları
         */
        @Nullable
        public Details getDetails() {
            if (!isSwear || word == null) {
                return null;
            }
            
            // Eski format uyumluluğu için Details oluştur
            Details details = new Details();
            details.word = word;
            details.category = category;
            details.severityLevel = severityLevel;
            details.detectedWords = detectedWords != null ? detectedWords : Collections.singletonList(word);
            
            return details;
        }

        public boolean isAiDetected() {
            return model != null && !model.isEmpty();
        }
        
        /**
         * Küfür kelimesini doğrudan ana Result objesinden döndürür.
         *
         * @return Tespit edilen küfür kelimesi
         */
        @NotNull
        public String getWord() {
            return word != null ? word : "";
        }

        /**
         * Kategoriyi doğrudan ana Result objesinden döndürür.
         *
         * @return Küfür kategorisi
         */
        @NotNull
        public String getCategory() {
            return category != null ? category : "bilinmeyen";
        }

        /**
         * Şiddet seviyesini doğrudan ana Result objesinden döndürür.
         *
         * @return Küfür şiddet seviyesi (1-5 arası)
         */
        public int getSeverityLevel() {
            return severityLevel;
        }
        
        /**
         * Tespit edilen kelimelerin listesini döndürür.
         * NOT: Yeni API formatında bu alan mevcut değildir,
         * geriye uyumluluk için tutulmaktadır.
         *
         * @return Tespit edilen kelimelerin listesi
         */
        @NotNull
        public List<String> getDetectedWords() {
            if (detectedWords != null) {
                return detectedWords;
            } else if (word != null) {
                return Collections.singletonList(word);
            }
            return Collections.emptyList();
        }
    }

    /**
     * Geriye uyumluluk için Details sınıfı.
     * NOT: Yeni API formatında details ayrı bir nesne değildir,
     * tüm bilgiler ana Result objesindedir.
     */
    public static class Details {
        private String word;
        private String category;
        private int severityLevel;
        private List<String> detectedWords;

        @NotNull
        public String getWord() {
            return word != null ? word : "";
        }

        @NotNull
        public String getCategory() {
            return category != null ? category : "bilinmeyen";
        }

        public int getSeverityLevel() {
            return severityLevel;
        }

        @NotNull
        public List<String> getDetectedWords() {
            return detectedWords != null ? detectedWords : Collections.emptyList();
        }
    }
}
