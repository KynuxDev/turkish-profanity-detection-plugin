package kynux.cloud.turkishProfanityDetection;

import kynux.cloud.turkishProfanityDetection.api.ProfanityApiService;
import kynux.cloud.turkishProfanityDetection.commands.PluginCommand;
import kynux.cloud.turkishProfanityDetection.discord.DiscordWebhook;
import kynux.cloud.turkishProfanityDetection.gui.AdminGui;
import kynux.cloud.turkishProfanityDetection.hooks.PlaceholderAPIHook;
import kynux.cloud.turkishProfanityDetection.listeners.ChatListener;
import kynux.cloud.turkishProfanityDetection.listeners.GuiListener;
import kynux.cloud.turkishProfanityDetection.storage.ProfanityStorage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Turkish Profanity Detection Plugin Ana Sınıfı.
 * Bu plugin, oyuncuların mesajlarını küfür veya hakaret içerip içermediğini tespit etmek için
 * API ile iletişim kurar.
 */
public final class TurkishProfanityDetection extends JavaPlugin {
    private ProfanityApiService profanityApiService;
    private ChatListener chatListener;
    private ProfanityStorage profanityStorage;
    private DiscordWebhook discordWebhook;
    private AdminGui adminGui;
    private PlaceholderAPIHook placeholderAPIHook;
    
    // Performans optimizasyonu için önbellek ve thread yönetimi
    private final Map<String, Boolean> messageCache = new ConcurrentHashMap<>();
    private final int CACHE_SIZE_LIMIT = 1000;
    private ExecutorService threadPool;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        getLogger().info("Turkish Profanity Detection başlatılıyor...");
        
        try {
            // Config dosyasını oluştur/yükle
            saveDefaultConfig();
            
            // Thread pool oluştur - CPU çekirdek sayısına göre
            int threadCount = Runtime.getRuntime().availableProcessors();
            threadPool = Executors.newFixedThreadPool(threadCount);
            getLogger().info("Thread havuzu başlatıldı: " + threadCount + " thread");
            
            // Servisleri başlat
            initServices();
            
            // Listener'ları kaydet
            registerListeners();
            
            // Komutları kaydet
            registerCommands();
            
            // Başlangıç bilgilerini göster
            long elapsedTime = System.currentTimeMillis() - startTime;
            getLogger().info("Turkish Profanity Detection eklentisi başarıyla etkinleştirildi! (" + elapsedTime + "ms)");
            getLogger().info("Küfür tespit API'si: " + getConfig().getString("api.url", "http://localhost:3000/api/swear"));
            
            if (getConfig().getBoolean("actions.discord.enabled", false)) {
                getLogger().info("Discord webhook entegrasyonu aktif!");
            }
            
            String storageType = getConfig().getString("statistics.storage-type", "mysql");
            if (getConfig().getBoolean("statistics.enabled", true)) {
                getLogger().info("İstatistik depolama aktif: " + storageType);
                if (storageType.equals("mysql")) {
                    getLogger().info("MySQL veritabanı kullanılıyor. Veriler sunucu yeniden başlatıldığında kaybolmayacak.");
                } else {
                    getLogger().warning("Dikkat: File tabanlı depolama kullanılıyor. Veriler sunucu yeniden başlatıldığında kaybolabilir.");
                    getLogger().warning("Verilerin kalıcı olması için config.yml dosyasında statistics.storage-type: \"mysql\" ayarını kullanmanızı öneririz.");
                }
            }
            
            // Önbellek temizleme görevi başlat
            startCacheCleanupTask();
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Eklenti başlatılırken ciddi bir hata oluştu: " + e.getMessage(), e);
        }
    }
    
    /**
     * Servisleri başlatır.
     */
    private void initServices() {
        try {
            this.profanityApiService = new ProfanityApiService(getConfig(), getLogger());
            getLogger().info("API servisi başlatıldı");
            
            this.profanityStorage = new ProfanityStorage(this);
            getLogger().info("Depolama servisi başlatıldı");
            
            this.discordWebhook = new DiscordWebhook(this);
            if (getConfig().getBoolean("actions.discord.enabled", false)) {
                getLogger().info("Discord webhook servisi başlatıldı");
            }
            
            this.adminGui = new AdminGui(this, profanityStorage);
            getLogger().info("Admin arayüzü başlatıldı");
            
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                this.placeholderAPIHook = new PlaceholderAPIHook(this);
                getLogger().info("PlaceholderAPI entegrasyonu başlatıldı");
            } else {
                getLogger().info("PlaceholderAPI bulunamadı, ilgili özellikler devre dışı");
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Servisler başlatılırken hata: " + e.getMessage(), e);
        }
    }
    
    /**
     * Dinleyicileri kaydeder.
     */
    private void registerListeners() {
        try {
            this.chatListener = new ChatListener(this);
            getServer().getPluginManager().registerEvents(chatListener, this);
            getServer().getPluginManager().registerEvents(new GuiListener(this), this);
            getLogger().info("Event dinleyicileri kaydedildi");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Listener'lar kaydedilirken hata: " + e.getMessage(), e);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Turkish Profanity Detection kapatılıyor...");
        
        try {
            // Önbelleği temizle
            messageCache.clear();
            
            // Thread havuzunu düzgün bir şekilde kapat
            if (threadPool != null && !threadPool.isShutdown()) {
                threadPool.shutdown();
                try {
                    // Tüm görevlerin tamamlanması için maksimum 5 saniye bekle
                    if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                        // Süre doldu, kalan görevleri zorla sonlandır
                        threadPool.shutdownNow();
                    }
                    getLogger().info("Thread havuzu başarıyla kapatıldı");
                } catch (InterruptedException e) {
                    // Beklerken kesinti olursa, kalan görevleri zorla sonlandır
                    threadPool.shutdownNow();
                    Thread.currentThread().interrupt();
                    getLogger().warning("Thread havuzu kapatılırken kesinti oldu");
                }
            }
            
            // Veritabanı bağlantılarını ve açık kaynakları kapat
            if (profanityStorage != null) {
                profanityStorage.shutdown();
                getLogger().info("Depolama servisi kapatıldı");
            }
            
            // API servisini kapat
            if (profanityApiService != null) {
                // profanityApiService'de kapatma işlemi varsa burada çağrılabilir
                getLogger().info("API servisi kapatıldı");
            }
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Eklenti kapatılırken hata: " + e.getMessage(), e);
        } finally {
            getLogger().info("Turkish Profanity Detection eklentisi devre dışı bırakıldı.");
        }
    }
    
    /**
     * Komutları kaydeder.
     */
    private void registerCommands() {
        try {
            org.bukkit.command.PluginCommand command = getCommand("turkishprofanity");
            if (command != null) {
                PluginCommand executor = new PluginCommand(this);
                command.setExecutor(executor);
                command.setTabCompleter(executor);
                getLogger().info("Komutlar başarıyla kaydedildi");
            } else {
                getLogger().warning("turkishprofanity komutu plugin.yml'de bulunamadı!");
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Komutlar kaydedilirken hata: " + e.getMessage(), e);
        }
    }
    
    /**
     * API servisi getter metodu.
     *
     * @return ProfanityApiService örneği
     */
    public ProfanityApiService getProfanityApiService() {
        return profanityApiService;
    }
    
    /**
     * Küfür istatistiklerini saklamak için depolama servisi getter metodu.
     *
     * @return ProfanityStorage örneği
     */
    public ProfanityStorage getProfanityStorage() {
        return profanityStorage;
    }
    
    /**
     * Discord webhook servisi getter metodu.
     *
     * @return DiscordWebhook örneği
     */
    public DiscordWebhook getDiscordWebhook() {
        return discordWebhook;
    }
    
    /**
     * Admin GUI servisi getter metodu.
     *
     * @return AdminGui örneği
     */
    public AdminGui getAdminGui() {
        return adminGui;
    }
    
    /**
     * PlaceholderAPI hook servisi getter metodu.
     *
     * @return PlaceholderAPIHook örneği
     */
    public PlaceholderAPIHook getPlaceholderAPIHook() {
        return placeholderAPIHook;
    }
    
    /**
     * Thread havuzu getter metodu.
     *
     * @return ExecutorService örneği
     */
    public ExecutorService getThreadPool() {
        return threadPool;
    }
    
    /**
     * Eklentiyi yeniden yükler.
     */
    public void reloadPlugin() {
        try {
            getLogger().info("Eklenti yeniden yükleniyor...");
            long startTime = System.currentTimeMillis();
            
            // Önbelleği temizle
            messageCache.clear();
            
            // Thread havuzunu yeniden başlat
            if (threadPool != null && !threadPool.isShutdown()) {
                threadPool.shutdown();
                if (!threadPool.awaitTermination(3, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            }
            
            int threadCount = Runtime.getRuntime().availableProcessors();
            threadPool = Executors.newFixedThreadPool(threadCount);
            
            // Config dosyalarını yeniden yükle
            reloadConfig();
            
            // Veri kaynaklarını kapat
            if (profanityStorage != null) {
                profanityStorage.shutdown();
            }
            
            // Servisleri yeniden başlat
            initServices();
            
            // Dinleyicileri yeniden kaydet
            registerListeners();
            
            // Yeniden yükleme tamamlandı
            long elapsedTime = System.currentTimeMillis() - startTime;
            getLogger().info("Eklenti başarıyla yeniden yüklendi! (" + elapsedTime + "ms)");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Eklenti yeniden yüklenirken hata: " + e.getMessage(), e);
        }
    }
    
    /**
     * Önbellek temizleme görevini başlatır.
     * Her 15 dakikada bir önbelleği kontrol eder ve gerekirse eski girişleri temizler.
     */
    private void startCacheCleanupTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            try {
                // Önbellek boyutu sınırı aşıldığında en eski girişlerin %20'sini temizle
                if (messageCache.size() > CACHE_SIZE_LIMIT) {
                    int removeCount = (int) (CACHE_SIZE_LIMIT * 0.2); // %20'sini temizle
                    
                    // En eski 20% girişi temizle - basit bir yaklaşım
                    messageCache.keySet().stream()
                        .limit(removeCount)
                        .forEach(messageCache::remove);
                    
                    getLogger().info("Önbellek temizlendi: " + removeCount + " giriş silindi");
                }
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Önbellek temizlenirken hata: " + e.getMessage(), e);
            }
        }, 20 * 60 * 15, 20 * 60 * 15); // 15 dakikada bir çalıştır (20 tick * 60 saniye * 15 dakika)
    }
    
    /**
     * Mesaj önbelleğini kontrol eder, eğer önbellekte varsa sonucu döner.
     * Yoksa null döner.
     *
     * @param message Kontrol edilecek mesaj
     * @return Önbellekte varsa Boolean değeri, yoksa null
     */
    public Boolean getFromCache(String message) {
        return messageCache.get(message);
    }
    
    /**
     * Bir mesajı ve sonucunu önbelleğe ekler.
     *
     * @param message Önbelleğe eklenecek mesaj
     * @param result Mesajın küfür içerip içermediği
     */
    public void addToCache(String message, boolean result) {
        if (messageCache.size() < CACHE_SIZE_LIMIT) {
            messageCache.put(message, result);
        }
    }
}
