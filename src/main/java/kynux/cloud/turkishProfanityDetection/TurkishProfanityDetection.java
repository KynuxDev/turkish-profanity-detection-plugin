package kynux.cloud.turkishProfanityDetection;

import kynux.cloud.turkishProfanityDetection.api.KynuxAIService;
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

public final class TurkishProfanityDetection extends JavaPlugin {
    private ChatListener chatListener;
    private ProfanityStorage profanityStorage;
    private DiscordWebhook discordWebhook;
    private AdminGui adminGui;
    private PlaceholderAPIHook placeholderAPIHook;
    private KynuxAIService kynuxAIService;
    
    private final Map<String, Boolean> messageCache = new ConcurrentHashMap<>();
    private final int CACHE_SIZE_LIMIT = 1000;
    private ExecutorService threadPool;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        getLogger().info("Turkish Profanity Detection başlatılıyor...");
        
        try {
            saveDefaultConfig();
            
            int threadCount = Runtime.getRuntime().availableProcessors();
            threadPool = Executors.newFixedThreadPool(threadCount);
            getLogger().info("Thread havuzu başlatıldı: " + threadCount + " thread");
            
            initServices();
            registerListeners();
            registerCommands();
            
            long elapsedTime = System.currentTimeMillis() - startTime;
            getLogger().info("Turkish Profanity Detection eklentisi başarıyla etkinleştirildi! (" + elapsedTime + "ms)");
            getLogger().info("Kynux AI API URL: " + getConfig().getString("kynux_api.url"));
            
            if (getConfig().getBoolean("actions.discord.enabled", false)) {
                getLogger().info("Discord webhook entegrasyonu aktif!");
            }
            
            String storageType = getConfig().getString("statistics.storage-type", "mysql");
            if (getConfig().getBoolean("statistics.enabled", true)) {
                getLogger().info("İstatistik depolama aktif: " + storageType);
                if (storageType.equals("mysql")) {
                    getLogger().info("MySQL veritabanı kullanılıyor.");
                } else {
                    getLogger().warning("File tabanlı depolama kullanılıyor. Veriler sunucu yeniden başlatıldığında kaybolabilir.");
                }
            }
            
            startCacheCleanupTask();
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Eklenti başlatılırken ciddi bir hata oluştu: " + e.getMessage(), e);
        }
    }
    
    private void initServices() {
        try {
            this.kynuxAIService = new KynuxAIService(this);
            getLogger().info("Kynux AI servisi başlatıldı");
            
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
            messageCache.clear();
            
            if (threadPool != null && !threadPool.isShutdown()) {
                threadPool.shutdown();
                try {
                    if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                        threadPool.shutdownNow();
                    }
                    getLogger().info("Thread havuzu başarıyla kapatıldı");
                } catch (InterruptedException e) {
                    threadPool.shutdownNow();
                    Thread.currentThread().interrupt();
                    getLogger().warning("Thread havuzu kapatılırken kesinti oldu");
                }
            }
            
            if (profanityStorage != null) {
                profanityStorage.shutdown();
                getLogger().info("Depolama servisi kapatıldı");
            }
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Eklenti kapatılırken hata: " + e.getMessage(), e);
        } finally {
            getLogger().info("Turkish Profanity Detection eklentisi devre dışı bırakıldı.");
        }
    }
    
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
    
    public ProfanityStorage getProfanityStorage() {
        return profanityStorage;
    }
    
    public DiscordWebhook getDiscordWebhook() {
        return discordWebhook;
    }
    
    public AdminGui getAdminGui() {
        return adminGui;
    }
    
    public PlaceholderAPIHook getPlaceholderAPIHook() {
        return placeholderAPIHook;
    }

    public KynuxAIService getKynuxAIService() {
        return kynuxAIService;
    }
    
    public ExecutorService getThreadPool() {
        return threadPool;
    }
    
    public void reloadPlugin() {
        try {
            getLogger().info("Eklenti yeniden yükleniyor...");
            long startTime = System.currentTimeMillis();
            
            messageCache.clear();
            
            if (threadPool != null && !threadPool.isShutdown()) {
                threadPool.shutdown();
                if (!threadPool.awaitTermination(3, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            }
            
            int threadCount = Runtime.getRuntime().availableProcessors();
            threadPool = Executors.newFixedThreadPool(threadCount);
            
            reloadConfig();
            
            if (profanityStorage != null) {
                profanityStorage.shutdown();
            }
            
            initServices();
            if (kynuxAIService != null) {
                kynuxAIService.reloadConfig();
            }
            
            registerListeners();
            
            long elapsedTime = System.currentTimeMillis() - startTime;
            getLogger().info("Eklenti başarıyla yeniden yüklendi! (" + elapsedTime + "ms)");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Eklenti yeniden yüklenirken hata: " + e.getMessage(), e);
        }
    }
    
    private void startCacheCleanupTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            try {
                if (messageCache.size() > CACHE_SIZE_LIMIT) {
                    int removeCount = (int) (CACHE_SIZE_LIMIT * 0.2);
                    
                    messageCache.keySet().stream()
                        .limit(removeCount)
                        .forEach(messageCache::remove);
                    
                    getLogger().info("Önbellek temizlendi: " + removeCount + " giriş silindi");
                }
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Önbellek temizlenirken hata: " + e.getMessage(), e);
            }
        }, 20 * 60 * 15, 20 * 60 * 15);
    }
    
    public Boolean getFromCache(String message) {
        return messageCache.get(message);
    }
    
    public void addToCache(String message, boolean result) {
        if (messageCache.size() < CACHE_SIZE_LIMIT) {
            messageCache.put(message, result);
        }
    }
}
