package kynux.cloud.turkishProfanityDetection.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Küfür tespit API'si ile iletişim kuran servis sınıfı.
 */
public class ProfanityApiService {
    private final OkHttpClient client;
    private final Gson gson;
    private final String apiUrl;
    private final boolean useAI;
    private final String model;
    private final double confidence;
    private final List<String> ipWhitelist;
    private final boolean ipWhitelistEnabled;
    private final Logger logger;
    private final int timeout;

    /**
     * API servisini başlatır.
     *
     * @param config Ayarları içeren yapılandırma dosyası
     * @param logger Log işlemleri için logger
     */
    public ProfanityApiService(@NotNull FileConfiguration config, @NotNull Logger logger) {
        this.logger = logger;
        
        // API ayarlarını yapılandırma dosyasından al - yeni minecraft-check endpoint'i ile
        this.apiUrl = config.getString("api.url", "http://api.kynux.cloud/api/swear/minecraft-check");
        this.timeout = config.getInt("api.timeout", 30000); // Varsayılan 30 saniye
        this.useAI = config.getBoolean("api.ai.use", true);
        this.model = config.getString("api.ai.model", "gpt-4.5");
        this.confidence = config.getDouble("api.ai.confidence", 0.1);
        
        // IP güvenlik ayarları
        this.ipWhitelistEnabled = config.getBoolean("security.ip-whitelist.enabled", false);
        this.ipWhitelist = config.getStringList("security.ip-whitelist.ips");
        
        // OkHttpClient yapılandırması ile daha sağlam bir HTTP istemcisi oluştur
        this.client = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                .readTimeout(timeout, TimeUnit.MILLISECONDS)
                .writeTimeout(timeout, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true) // Bağlantı hatalarında otomatik yeniden deneme
                .connectionPool(new okhttp3.ConnectionPool(5, 60, TimeUnit.SECONDS)) // Bağlantı havuzu optimizasyonu
                .protocols(java.util.Arrays.asList(okhttp3.Protocol.HTTP_1_1, okhttp3.Protocol.HTTP_2)) // HTTP protokolleri
                .dns(new okhttp3.Dns() {
                    @Override
                    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
                        try {
                            return java.util.Arrays.asList(InetAddress.getAllByName(hostname));
                        } catch (UnknownHostException e) {
                            logger.warning("DNS çözümleme hatası: " + hostname + " - " + e.getMessage());
                            throw e;
                        }
                    }
                })
                .build();
                
        // JSON çözücü/kodlayıcı
        this.gson = new GsonBuilder().create();
        
        logger.info("Küfür tespit API servisi başlatıldı: " + apiUrl);
        logger.info("API timeout süresi: " + timeout + "ms");
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
                // URL builder değişkenini daha geniş kapsamda tanımla
                HttpUrl.Builder urlBuilder = null;
                
                try {
                    // IP güvenlik kontrolü
                    if (ipWhitelistEnabled && !isApiHostAllowed()) {
                        logger.severe("API host IP whitelistte değil! İstek reddedildi.");
                        return ProfanityResponse.createApiError("API host IP whitelist kontrolünü geçemedi");
                    }

                    // API URL ve parametreleri oluştur
                    try {
                        urlBuilder = HttpUrl.parse(apiUrl).newBuilder();
                    } catch (NullPointerException e) {
                        logger.severe("Geçersiz API URL: " + apiUrl);
                        return ProfanityResponse.createApiError("Geçersiz API URL: " + apiUrl);
                    }
                    
                    // URL parametrelerini ekle - yeni endpoint sadece text parametresi kullanıyor
                    urlBuilder.addQueryParameter("text", text);
                    
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
                            return ProfanityResponse.createServerError(statusCode);
                        }

                        // Yanıtı JSON olarak çözümle
                        String responseBody = response.body().string();
                        ProfanityResponse profanityResponse = gson.fromJson(responseBody, ProfanityResponse.class);
                        return profanityResponse;
                    }
                } catch (SocketTimeoutException e) {
                    // Zaman aşımı hatası - yeniden deneme ile devam et
                    logger.log(Level.WARNING, "API isteği sırasında zaman aşımı: " + e.getMessage());
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
                    
                    // Retry mekanizması
                    if (urlBuilder != null) {
                        return retryRequest(urlBuilder, text, errorType);
                    } else {
                        logger.log(Level.SEVERE, "URL oluşturulamadığı için yeniden deneme yapılamıyor", e);
                        return ProfanityResponse.createConnectionError(errorMessage);
                    }
                } catch (IllegalArgumentException e) {
                    logger.log(Level.SEVERE, "API istek hatası: " + e.getMessage(), e);
                    return ProfanityResponse.createApiError(e.getMessage());
                }
            }
        });
    }
    
    /**
     * API isteğini yeniden deneme mantığı.
     * 
     * @param urlBuilder Orijinal URL builder
     * @param text İstek metni
     * @param errorType İlk hatanın türü
     * @return API yanıtı
     */
    private ProfanityResponse retryRequest(HttpUrl.Builder urlBuilder, String text, String errorType) {
        // Yeniden deneme sayısı ve gecikmesi
        final int MAX_RETRIES = 3;
        int currentRetry = 0;
        long retryDelay = 1000; // Başlangıç 1 saniye
        
        while (currentRetry < MAX_RETRIES) {
            currentRetry++;
            
            try {
                // Kademeli olarak artan gecikme (exponential backoff)
                Thread.sleep(retryDelay);
                retryDelay *= 2; // Her denemede süreyi iki katına çıkar
                
                logger.info("API isteği yeniden deneniyor (" + currentRetry + "/" + MAX_RETRIES + 
                           ") - Hata: " + errorType);
                
                // Sadece text parametresini ekle - yeni endpoint'de sadece bu gerekiyor
                urlBuilder.addQueryParameter("text", text);
                
                // Yeni bir istek oluştur
                Request retryRequest = new Request.Builder()
                        .url(urlBuilder.build())
                        .get()
                        .header("Connection", "close") // Keep-alive sorunlarını önlemek için bağlantıyı kapat
                        .header("User-Agent", "TurkishProfanityDetection/1.0")
                        .header("Accept", "application/json")
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
                    logger.info("API isteği başarıyla yeniden denendi");
                    return profanityResponse;
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                logger.log(Level.SEVERE, "Yeniden deneme işlemi kesintiye uğradı", ie);
                return ProfanityResponse.createConnectionError("Yeniden deneme sırasında kesinti: " + ie.getMessage());
            } catch (IOException retryEx) {
                logger.log(Level.WARNING, "Yeniden deneme sırasında bağlantı hatası (" + currentRetry + "/" + MAX_RETRIES + 
                          "): " + retryEx.getMessage());
                // Devam et ve bir sonraki denemeye geç
            }
        }
        
        // Tüm denemeler başarısız olduğunda
        logger.log(Level.SEVERE, "API bağlantısı " + MAX_RETRIES + " deneme sonrasında kurulamadı");
        return ProfanityResponse.createConnectionError("API'ye " + MAX_RETRIES + " deneme sonrasında bağlanılamadı");
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
}
