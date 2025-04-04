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
        
        // API ayarlarını yapılandırma dosyasından al
        this.apiUrl = config.getString("api.url", "http://api.kynux.cloud/api/swear/detect");
        this.timeout = config.getInt("api.timeout", 30000); // Varsayılan 30 saniye
        this.useAI = config.getBoolean("api.ai.use", true);
        this.model = config.getString("api.ai.model", "gpt-4.5");
        this.confidence = config.getDouble("api.ai.confidence", 0.1);
        
        // IP güvenlik ayarları
        this.ipWhitelistEnabled = config.getBoolean("security.ip-whitelist.enabled", false);
        this.ipWhitelist = config.getStringList("security.ip-whitelist.ips");
        this.client = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                .readTimeout(timeout, TimeUnit.MILLISECONDS)
                .writeTimeout(timeout, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(false) // Yeniden denemeler kapalı
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
                try {
                    // IP güvenlik kontrolü
                    if (ipWhitelistEnabled && !isApiHostAllowed()) {
                        logger.severe("API host IP whitelistte değil! İstek reddedildi.");
                        return ProfanityResponse.createApiError("API host IP whitelist kontrolünü geçemedi");
                    }

                    // API URL ve parametreleri oluştur
                    HttpUrl.Builder urlBuilder;
                    try {
                        urlBuilder = HttpUrl.parse(apiUrl).newBuilder();
                    } catch (NullPointerException e) {
                        logger.severe("Geçersiz API URL: " + apiUrl);
                        return ProfanityResponse.createApiError("Geçersiz API URL: " + apiUrl);
                    }
                    
                    // URL parametrelerini ekle
                    urlBuilder.addQueryParameter("text", text)
                            .addQueryParameter("useAI", String.valueOf(useAI))
                            .addQueryParameter("model", model)
                            .addQueryParameter("confidence", String.valueOf(confidence));
                    
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
                    // Zaman aşımı hatası - basitçe log tutup devam et
                    logger.log(Level.WARNING, "API isteği sırasında zaman aşımı: " + e.getMessage());
                    return ProfanityResponse.createTimeoutError(e.getMessage());
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "API bağlantı hatası: " + e.getMessage(), e);
                    return ProfanityResponse.createConnectionError(e.getMessage());
                } catch (IllegalArgumentException e) {
                    logger.log(Level.SEVERE, "API istek hatası: " + e.getMessage(), e);
                    return ProfanityResponse.createApiError(e.getMessage());
                }
            }
        });
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
