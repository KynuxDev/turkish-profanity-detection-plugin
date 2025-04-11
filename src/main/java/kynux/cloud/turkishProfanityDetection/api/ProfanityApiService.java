package kynux.cloud.turkishProfanityDetection.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Küfür tespit API'si ile iletişim kuran gelişmiş servis sınıfı.
 * Circuit breaker pattern, çoklu dil desteği ve API anahtarı doğrulama içerir.
 */
public class ProfanityApiService {
    private final OkHttpClient client;
    private final Gson gson;
    private final String apiUrl;
    private final String apiKey;
    private final boolean useAI;
    private final String model;
    private final double confidence;
    private final List<String> languages;
    private final List<String> ipWhitelist;
    private final boolean ipWhitelistEnabled;
    private final Logger logger;
    private final int timeout;
    
    // Retry ayarları
    private final int maxRetryAttempts;
    private final double backoffMultiplier;
    
    // Circuit breaker ayarları
    private final boolean circuitBreakerEnabled;
    private final int failureThreshold;
    private final long resetTimeout;
    private final AtomicInteger failureCount;
    private volatile boolean circuitOpen;
    private volatile long circuitOpenTime;
    
    // Önbellek sistemi
    private final Map<String, CacheEntry> responseCache;
    private final int cacheTtlSeconds;
    private final int cacheMaxSize;
    
    // Metrik takibi
    private final AtomicInteger requestCount;
    private final AtomicInteger successCount;
    private final AtomicInteger errorCount;
    private final AtomicInteger cacheHitCount;

    /**
     * API servisini başlatır.
     *
     * @param config Ayarları içeren yapılandırma dosyası
     * @param logger Log işlemleri için logger
     */
    public ProfanityApiService(@NotNull FileConfiguration config, @NotNull Logger logger) {
        this.logger = logger;
        
        // API ayarlarını yapılandırma dosyasından al
        this.apiUrl = config.getString("api.url", "http://api.kynux.cloud/api/swear/minecraft-check");
        this.timeout = config.getInt("api.timeout", 30000); // Varsayılan 30 saniye
        this.apiKey = config.getString("api.api-key", "");
        this.useAI = config.getBoolean("api.ai.use", true);
        this.model = config.getString("api.ai.model", "claude-3.7-sonnet");
        this.confidence = config.getDouble("api.ai.confidence", 0.1);
        
        // Dil desteği
        List<String> configLanguages = config.getStringList("api.languages");
        if (configLanguages.isEmpty()) {
            configLanguages = Collections.singletonList("tr"); // Varsayılan olarak Türkçe
        }
        this.languages = Collections.unmodifiableList(new ArrayList<>(configLanguages));
        
        // Retry ayarları
        this.maxRetryAttempts = config.getInt("api.retry.max-attempts", 3);
        this.backoffMultiplier = config.getDouble("api.retry.backoff-multiplier", 2.0);
        
        // Circuit breaker ayarları
        this.circuitBreakerEnabled = config.getBoolean("api.circuit-breaker.enabled", true);
        this.failureThreshold = config.getInt("api.circuit-breaker.failure-threshold", 5);
        this.resetTimeout = config.getLong("api.circuit-breaker.reset-timeout", 60000); // 1 dakika
        this.failureCount = new AtomicInteger(0);
        this.circuitOpen = false;
        this.circuitOpenTime = 0;
        
        // Önbellek ayarları
        this.responseCache = new ConcurrentHashMap<>();
        this.cacheTtlSeconds = config.getInt("cache.ttl-seconds", 3600); // 1 saat
        this.cacheMaxSize = config.getInt("cache.max-size", 1000);
        
        // Metrik takibi
        this.requestCount = new AtomicInteger(0);
        this.successCount = new AtomicInteger(0);
        this.errorCount = new AtomicInteger(0);
        this.cacheHitCount = new AtomicInteger(0);
        
        // IP güvenlik ayarları
        this.ipWhitelistEnabled = config.getBoolean("security.ip-whitelist.enabled", false);
        this.ipWhitelist = config.getStringList("security.ip-whitelist.ips");
        
        // OkHttpClient yapılandırması ile daha sağlam bir HTTP istemcisi oluştur
        this.client = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                .readTimeout(timeout, TimeUnit.MILLISECONDS)
                .writeTimeout(timeout, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true) // Bağlantı hatalarında otomatik yeniden deneme
                .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES)) // Bağlantı havuzu optimizasyonu
                .protocols(Arrays.asList(Protocol.HTTP_1_1, Protocol.HTTP_2)) // HTTP protokolleri
                .dns(new Dns() {
                    @Override
                    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
                        try {
                            return Arrays.asList(InetAddress.getAllByName(hostname));
                        } catch (UnknownHostException e) {
                            logger.warning("DNS çözümleme hatası: " + hostname + " - " + e.getMessage());
                            throw e;
                        }
                    }
                })
                // Interceptor ekleyerek tüm isteklere ortak header'lar ekle
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request.Builder requestBuilder = original.newBuilder()
                            .header("User-Agent", "TurkishProfanityDetection/1.1.0")
                            .header("Accept", "application/json")
                            .header("Connection", "keep-alive");
                    
                    // API anahtarı varsa ekle
                    if (apiKey != null && !apiKey.isEmpty()) {
                        requestBuilder.header("X-API-Key", apiKey);
                    }
                    
                    return chain.proceed(requestBuilder.build());
                })
                .build();
                
        // JSON çözücü/kodlayıcı
        this.gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                .create();
        
        logger.info("Küfür tespit API servisi başlatıldı: " + apiUrl);
        logger.info("API timeout süresi: " + timeout + "ms");
        logger.info("Desteklenen diller: " + String.join(", ", languages));
        
        if (circuitBreakerEnabled) {
            logger.info("Circuit breaker aktif: Eşik=" + failureThreshold + ", Sıfırlama=" + resetTimeout + "ms");
        }
        
        // Önbellek temizleme görevini başlat
        startCacheCleanupTask();
    }

    /**
     * Verilen metinde küfür olup olmadığını API'ye sorarak kontrol eder.
     * Asenkron çalışır ve sonucu gelecekte döner.
     *
     * @param text Kontrol edilecek metin
     * @return API yanıtını içeren CompletableFuture
     */
    public CompletableFuture<ProfanityResponse> checkText(@NotNull String text) {
        return CompletableFuture.supplyAsync(new Supplier<ProfanityResponse>() {
            @Override
            public ProfanityResponse get() {
                // Önbellek kontrolü
                String cacheKey = generateCacheKey(text);
                CacheEntry cachedResponse = responseCache.get(cacheKey);
                
                if (cachedResponse != null && !cachedResponse.isExpired()) {
                    // Önbellekte bulunan yanıtı kullan
                    cacheHitCount.incrementAndGet();
                    logger.fine("Önbellek kullanıldı: " + cacheKey);
                    return cachedResponse.getResponse();
                }
                
                // İstek sayacını artır
                requestCount.incrementAndGet();
                
                // Circuit breaker kontrolü
                if (circuitBreakerEnabled && isCircuitOpen()) {
                    errorCount.incrementAndGet();
                    return ProfanityResponse.createCircuitOpenError();
                }
                
                // URL builder değişkenini daha geniş kapsamda tanımla
                HttpUrl.Builder urlBuilder = null;
                
                try {
                    // IP güvenlik kontrolü
                    if (ipWhitelistEnabled && !isApiHostAllowed()) {
                        logger.severe("API host IP whitelistte değil! İstek reddedildi.");
                        errorCount.incrementAndGet();
                        return ProfanityResponse.createApiError("API host IP whitelist kontrolünü geçemedi");
                    }

                    // API URL ve parametreleri oluştur
                    try {
                        urlBuilder = HttpUrl.parse(apiUrl).newBuilder();
                    } catch (NullPointerException e) {
                        logger.severe("Geçersiz API URL: " + apiUrl);
                        errorCount.incrementAndGet();
                        recordFailure();
                        return ProfanityResponse.createApiError("Geçersiz API URL: " + apiUrl);
                    }
                    
                    // URL parametrelerini ekle
                    urlBuilder.addQueryParameter("text", text);
                    
                    // Dil parametrelerini ekle
                    for (String language : languages) {
                        urlBuilder.addQueryParameter("languages", language);
                    }
                    
                    // İsteği oluştur
                    Request request = new Request.Builder()
                            .url(urlBuilder.build())
                            .get()
                            .build();

                    // İsteği gönder ve yanıtı al
                    try (Response response = client.newCall(request).execute()) {
                        if (!response.isSuccessful()) {
                            int statusCode = response.code();
                            logger.warning("API yanıt vermedi! Durum kodu: " + statusCode);
                            errorCount.incrementAndGet();
                            recordFailure();
                            return ProfanityResponse.createServerError(statusCode);
                        }

                        // Yanıtı JSON olarak çözümle
                        String responseBody = response.body().string();
                        ProfanityResponse profanityResponse = gson.fromJson(responseBody, ProfanityResponse.class);
                        
                        // Başarılı yanıtı önbelleğe ekle
                        if (profanityResponse.isSuccess()) {
                            successCount.incrementAndGet();
                            resetCircuitBreaker();
                            cacheResponse(cacheKey, profanityResponse);
                        } else {
                            errorCount.incrementAndGet();
                            recordFailure();
                        }
                        
                        return profanityResponse;
                    }
                } catch (SocketTimeoutException e) {
                    // Zaman aşımı hatası - yeniden deneme ile devam et
                    logger.log(Level.WARNING, "API isteği sırasında zaman aşımı: " + e.getMessage());
                    errorCount.incrementAndGet();
                    recordFailure();
                    return retryRequest(urlBuilder, text, "Zaman aşımı");
                } catch (IOException e) {
                    // Bağlantı hatalarını işle ve yeniden dene
                    String errorMessage = e.getMessage();
                    String errorType = "Genel IO Hatası";
                    
                    if (errorMessage != null) {
                        if (errorMessage.contains("unexpected end of stream") || 
                            errorMessage.contains("Connection reset") || 
                            errorMessage.contains("aborted by the software") ||
                            errorMessage.contains("EOF") ||
                            errorMessage.contains("not found: limit=0 content")) {
                            errorType = "Bağlantı Kesilmesi";
                        } else if (errorMessage.contains("Failed to connect") || 
                                   errorMessage.contains("Unable to resolve host")) {
                            errorType = "Sunucuya Ulaşılamıyor";
                        }
                    }
                    
                    logger.log(Level.WARNING, "API bağlantı hatası (" + errorType + "): " + errorMessage);
                    errorCount.incrementAndGet();
                    recordFailure();
                    
                    // Retry mekanizması
                    if (urlBuilder != null) {
                        return retryRequest(urlBuilder, text, errorType);
                    } else {
                        logger.log(Level.SEVERE, "URL oluşturulamadığı için yeniden deneme yapılamıyor", e);
                        return ProfanityResponse.createConnectionError(errorMessage);
                    }
                } catch (IllegalArgumentException e) {
                    logger.log(Level.SEVERE, "API istek hatası: " + e.getMessage(), e);
                    errorCount.incrementAndGet();
                    recordFailure();
                    return ProfanityResponse.createApiError(e.getMessage());
                }
            }
        });
    }
    
    /**
     * API isteğini yeniden deneme mantığı.
     * Gelişmiş exponential backoff ve jitter algoritması kullanır.
     * 
     * @param urlBuilder Orijinal URL builder
     * @param text İstek metni
     * @param errorType İlk hatanın türü
     * @return API yanıtı
     */
    private ProfanityResponse retryRequest(HttpUrl.Builder urlBuilder, String text, String errorType) {
        int currentRetry = 0;
        long retryDelay = 1000; // Başlangıç 1 saniye
        
        while (currentRetry < maxRetryAttempts) {
            currentRetry++;
            
            try {
                // Kademeli olarak artan gecikme (exponential backoff) + jitter
                long jitter = (long) (Math.random() * 200); // 0-200ms arası rastgele jitter
                Thread.sleep(retryDelay + jitter);
                retryDelay = (long) (retryDelay * backoffMultiplier); // Her denemede süreyi çarpanla artır
                
                logger.info("API isteği yeniden deneniyor (" + currentRetry + "/" + maxRetryAttempts + 
                           ") - Hata: " + errorType + " - Gecikme: " + (retryDelay + jitter) + "ms");
                
                // URL parametrelerini temizle ve yeniden ekle
                HttpUrl.Builder newUrlBuilder = HttpUrl.parse(apiUrl).newBuilder();
                newUrlBuilder.addQueryParameter("text", text);
                
                // Dil parametrelerini ekle
                for (String language : languages) {
                    newUrlBuilder.addQueryParameter("languages", language);
                }
                
                // Yeni bir istek oluştur
                Request retryRequest = new Request.Builder()
                        .url(newUrlBuilder.build())
                        .get()
                        .build();
                
                // Yeni bir HTTP istemcisi oluştur (bağlantı havuzu sorunlarını önlemek için)
                OkHttpClient retryClient = client.newBuilder()
                        .connectTimeout(timeout + (retryDelay / 2), TimeUnit.MILLISECONDS) // Her denemede biraz daha uzun timeout
                        .readTimeout(timeout + (retryDelay / 2), TimeUnit.MILLISECONDS)
                        .build();
                
                // İsteği gönder
                try (Response retryResponse = retryClient.newCall(retryRequest).execute()) {
                    if (!retryResponse.isSuccessful()) {
                        int statusCode = retryResponse.code();
                        logger.warning("Yeniden deneme başarısız! Durum kodu: " + statusCode);
                        // Devam et ve bir sonraki denemeye geç
                        continue;
                    }

                    // Yanıtı JSON olarak çözümle
                    String responseBody = retryResponse.body().string();
                    ProfanityResponse profanityResponse = gson.fromJson(responseBody, ProfanityResponse.class);
                    
                    // Başarılı yanıtı önbelleğe ekle
                    if (profanityResponse.isSuccess()) {
                        successCount.incrementAndGet();
                        resetCircuitBreaker();
                        String cacheKey = generateCacheKey(text);
                        cacheResponse(cacheKey, profanityResponse);
                        logger.info("API isteği başarıyla yeniden denendi");
                    }
                    
                    return profanityResponse;
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                logger.log(Level.SEVERE, "Yeniden deneme işlemi kesintiye uğradı", ie);
                return ProfanityResponse.createConnectionError("Yeniden deneme sırasında kesinti: " + ie.getMessage());
            } catch (IOException retryEx) {
                logger.log(Level.WARNING, "Yeniden deneme sırasında bağlantı hatası (" + currentRetry + "/" + maxRetryAttempts + 
                          "): " + retryEx.getMessage());
                // Devam et ve bir sonraki denemeye geç
            }
        }
        
        // Tüm denemeler başarısız olduğunda
        logger.log(Level.SEVERE, "API bağlantısı " + maxRetryAttempts + " deneme sonrasında kurulamadı");
        return ProfanityResponse.createConnectionError("API'ye " + maxRetryAttempts + " deneme sonrasında bağlanılamadı");
    }

    /**
     * API sunucusunun IP'sinin beyaz listede olup olmadığını kontrol eder.
     *
     * @return IP'nin izin verilip verilmediği
     */
    private boolean isApiHostAllowed() {
        if (!ipWhitelistEnabled || ipWhitelist.isEmpty()) {
            return true; // Whitelist devre dışı veya boşsa her zaman izin ver
        }

        try {
            String apiHost = HttpUrl.parse(apiUrl).host();
            
            if (ipWhitelist.contains(apiHost)) {
                return true; // Doğrudan host ismi eşleşirse izin ver
            }
            
            // Host adını IP'ye çözümle
            InetAddress address = InetAddress.getByName(apiHost);
            String hostAddress = address.getHostAddress();
            
            return ipWhitelist.contains(hostAddress);
        } catch (UnknownHostException | NullPointerException e) {
            logger.log(Level.SEVERE, "API host IP doğrulama hatası: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Circuit breaker'ın açık olup olmadığını kontrol eder.
     * Eğer açıksa ve yeterli süre geçmişse, yarı-açık duruma geçer.
     *
     * @return Circuit breaker açıksa true
     */
    private boolean isCircuitOpen() {
        if (!circuitOpen) {
            return false;
        }
        
        // Sıfırlama süresi geçti mi kontrol et
        long now = System.currentTimeMillis();
        if (now - circuitOpenTime > resetTimeout) {
            // Yarı-açık duruma geç
            logger.info("Circuit breaker yarı-açık duruma geçiyor (sıfırlama süresi doldu)");
            circuitOpen = false;
            failureCount.set(0);
            return false;
        }
        
        return true;
    }
    
    /**
     * Bir hata durumunda circuit breaker sayacını artırır ve
     * eşik değeri aşılırsa circuit breaker'ı açar.
     */
    private void recordFailure() {
        if (!circuitBreakerEnabled) {
            return;
        }
        
        int failures = failureCount.incrementAndGet();
        if (failures >= failureThreshold && !circuitOpen) {
            // Circuit breaker'ı aç
            circuitOpen = true;
            circuitOpenTime = System.currentTimeMillis();
            logger.warning("Circuit breaker açıldı! " + failures + " hata sonrası API istekleri " + 
                          (resetTimeout / 1000) + " saniye boyunca engelleniyor.");
        }
    }
    
    /**
     * Başarılı bir istek sonrası circuit breaker'ı sıfırlar.
     */
    private void resetCircuitBreaker() {
        if (!circuitBreakerEnabled) {
            return;
        }
        
        if (circuitOpen || failureCount.get() > 0) {
            circuitOpen = false;
            failureCount.set(0);
            logger.info("Circuit breaker sıfırlandı (başarılı istek)");
        }
    }
    
    /**
     * Önbellek için bir anahtar oluşturur.
     *
     * @param text Metin
     * @return Önbellek anahtarı
     */
    private String generateCacheKey(String text) {
        // Basit bir hash fonksiyonu
        return "text:" + text.hashCode() + ":langs:" + String.join("-", languages);
    }
    
    /**
     * Bir yanıtı önbelleğe ekler.
     *
     * @param key Önbellek anahtarı
     * @param response API yanıtı
     */
    private void cacheResponse(String key, ProfanityResponse response) {
        // Önbellek boyutu sınırını kontrol et
        if (responseCache.size() >= cacheMaxSize) {
            // En eski girişi bul ve sil (basit LRU implementasyonu)
            String oldestKey = null;
            long oldestTime = Long.MAX_VALUE;
            
            for (Map.Entry<String, CacheEntry> entry : responseCache.entrySet()) {
                if (entry.getValue().getCreationTime() < oldestTime) {
                    oldestTime = entry.getValue().getCreationTime();
                    oldestKey = entry.getKey();
                }
            }
            
            if (oldestKey != null) {
                responseCache.remove(oldestKey);
            }
        }
        
        // Yeni yanıtı önbelleğe ekle
        responseCache.put(key, new CacheEntry(response, cacheTtlSeconds));
    }
    
    /**
     * Önbellek temizleme görevini başlatır.
     * Düzenli aralıklarla süresi dolmuş önbellek girişlerini temizler.
     */
    private void startCacheCleanupTask() {
        Timer timer = new Timer("ProfanityAPI-CacheCleanup", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    int removedCount = 0;
                    Iterator<Map.Entry<String, CacheEntry>> iterator = responseCache.entrySet().iterator();
                    
                    while (iterator.hasNext()) {
                        Map.Entry<String, CacheEntry> entry = iterator.next();
                        if (entry.getValue().isExpired()) {
                            iterator.remove();
                            removedCount++;
                        }
                    }
                    
                    if (removedCount > 0) {
                        logger.fine("Önbellek temizlendi: " + removedCount + " süresi dolmuş giriş silindi");
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Önbellek temizleme sırasında hata: " + e.getMessage(), e);
                }
            }
        }, TimeUnit.MINUTES.toMillis(5), TimeUnit.MINUTES.toMillis(5)); // 5 dakikada bir çalıştır
    }
    
    /**
     * API metriklerini döndürür.
     *
     * @return API metrikleri
     */
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("requestCount", requestCount.get());
        metrics.put("successCount", successCount.get());
        metrics.put("errorCount", errorCount.get());
        metrics.put("cacheHitCount", cacheHitCount.get());
        metrics.put("cacheSize", responseCache.size());
        metrics.put("circuitBreakerOpen", circuitOpen);
        metrics.put("failureCount", failureCount.get());
        
        return metrics;
    }
    
    /**
     * Önbellek girişi sınıfı.
     * Bir API yanıtını ve geçerlilik süresini tutar.
     */
    private static class CacheEntry {
        private final ProfanityResponse response;
        private final long expirationTime;
        private final long creationTime;
        
        public CacheEntry(ProfanityResponse response, int ttlSeconds) {
            this.response = response;
            this.creationTime = System.currentTimeMillis();
            this.expirationTime = creationTime + (ttlSeconds * 1000L);
        }
        
        public ProfanityResponse getResponse() {
            return response;
        }
        
        public long getCreationTime() {
            return creationTime;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
    }
}
