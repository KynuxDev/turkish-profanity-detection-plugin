package kynux.cloud.turkishProfanityDetection.api;

import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProfanityResponse {
    private boolean success;
    private Result result;
    private String message;
    private String error;
    private ErrorType errorType;
 
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
    

    public ProfanityResponse() {
        this.success = false;
        this.errorType = ErrorType.UNKNOWN;
    }
    

    public static ProfanityResponse createTimeoutError(String message) {
        ProfanityResponse response = new ProfanityResponse();
        response.success = false;
        response.error = "API zaman aşımı: " + message;
        response.message = "API yanıt vermedi (zaman aşımı)";
        response.errorType = ErrorType.TIMEOUT;
        return response;
    }
    
    public static ProfanityResponse createConnectionError(String message) {
        ProfanityResponse response = new ProfanityResponse();
        response.success = false;
        response.error = "API bağlantı hatası: " + message;
        response.message = "API'ye bağlanılamadı";
        response.errorType = ErrorType.CONNECTION_ERROR;
        return response;
    }
    
    public static ProfanityResponse createApiError(String message) {
        ProfanityResponse response = new ProfanityResponse();
        response.success = false;
        response.error = "API hatası: " + message;
        response.message = "API bir hata döndürdü";
        response.errorType = ErrorType.API_ERROR;
        return response;
    }
    
    public static ProfanityResponse createServerError(int statusCode) {
        ProfanityResponse response = new ProfanityResponse();
        response.success = false;
        response.error = "Sunucu hatası: HTTP " + statusCode;
        response.message = "API sunucusu hata döndürdü: " + statusCode;
        response.errorType = ErrorType.SERVER_ERROR;
        return response;
    }
  
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
    
    public void setSuccess(boolean success) {
        this.success = success;
    }

    @Nullable
    public Result getResult() {
        return result;
    }
    
    public void setResult(Result result) {
        this.result = result;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    @Nullable
    public String getError() {
        return error;
    }
    
    @NotNull
    public ErrorType getErrorType() {
        return errorType != null ? errorType : ErrorType.NONE;
    }
    
    public boolean isTimeoutError() {
        return errorType == ErrorType.TIMEOUT;
    }
    
    public boolean isConnectionError() {
        return errorType == ErrorType.CONNECTION_ERROR;
    }
    
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
        
        public void setSwear(boolean isSwear) {
            this.isSwear = isSwear;
        }

        @Nullable
        public Details getDetails() {
            return details;
        }
        
        public void setDetails(Details details) {
            this.details = details;
        }

        public boolean isAiDetected() {
            return aiDetected;
        }
        
        public void setAiDetected(boolean aiDetected) {
            this.aiDetected = aiDetected;
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
        
        public void setWord(String word) {
            this.word = word;
        }

        @NotNull
        public String getCategory() {
            return category != null ? category : "bilinmeyen";
        }
        
        public void setCategory(String category) {
            this.category = category;
        }

        public int getSeverityLevel() {
            return severityLevel;
        }
        
        public void setSeverityLevel(int severityLevel) {
            this.severityLevel = severityLevel;
        }

        @NotNull
        public List<String> getDetectedWords() {
            return detectedWords != null ? detectedWords : Collections.emptyList();
        }
        
        public void setDetectedWords(List<String> detectedWords) {
            this.detectedWords = detectedWords;
        }
    }
}
