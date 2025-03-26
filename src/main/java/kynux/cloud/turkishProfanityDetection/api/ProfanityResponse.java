package kynux.cloud.turkishProfanityDetection.api;

import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * API'den dönen küfür tespiti yanıtı için model sınıfı.
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
        private Details details;
        private boolean aiDetected;

        public boolean isSwear() {
            return isSwear;
        }

        @Nullable
        public Details getDetails() {
            return details;
        }

        public boolean isAiDetected() {
            return aiDetected;
        }
    }

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
