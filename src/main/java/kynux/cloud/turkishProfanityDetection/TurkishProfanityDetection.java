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

    @Override
    public void onEnable() {
        // Config dosyasını oluştur/yükle
        saveDefaultConfig();
        
        // Servisleri başlat
        this.profanityApiService = new ProfanityApiService(getConfig(), getLogger());
        this.profanityStorage = new ProfanityStorage(this);
        this.discordWebhook = new DiscordWebhook(this);
        this.adminGui = new AdminGui(this, profanityStorage);
        this.placeholderAPIHook = new PlaceholderAPIHook(this);
        
        // Listener'ları kaydet
        this.chatListener = new ChatListener(this);
        getServer().getPluginManager().registerEvents(chatListener, this);
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        
        // Komutları kaydet
        registerCommands();
        
        getLogger().info("Turkish Profanity Detection eklentisi etkinleştirildi!");
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
    }

    @Override
    public void onDisable() {
        // Veritabanı bağlantılarını ve açık kaynakları kapat
        if (profanityStorage != null) {
            profanityStorage.shutdown();
        }
        
        getLogger().info("Turkish Profanity Detection eklentisi devre dışı bırakıldı.");
    }
    
    /**
     * Komutları kaydeder.
     */
    private void registerCommands() {
        org.bukkit.command.PluginCommand command = getCommand("turkishprofanity");
        if (command != null) {
            PluginCommand executor = new PluginCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        } else {
            getLogger().warning("turkishprofanity komutu plugin.yml'de bulunamadı!");
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
     * Eklentiyi yeniden yükler.
     */
    public void reloadPlugin() {
        try {
            // Config dosyalarını yeniden yükle
            reloadConfig();
            
            // Veri kaynaklarını kapat
            if (profanityStorage != null) {
                profanityStorage.shutdown();
            }
            
            // Servisleri yeniden başlat
            this.profanityApiService = new ProfanityApiService(getConfig(), getLogger());
            this.profanityStorage = new ProfanityStorage(this);
            this.discordWebhook = new DiscordWebhook(this);
            this.adminGui = new AdminGui(this, profanityStorage);
            this.placeholderAPIHook = new PlaceholderAPIHook(this);
            
            // Listener'ları yeniden kaydet
            Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);
            Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);
            
            String storageType = getConfig().getString("statistics.storage-type", "mysql");
            getLogger().info("Depolama tipi: " + storageType);
            
            getLogger().info("Eklenti başarıyla yeniden yüklendi!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Eklenti yeniden yüklenirken hata: " + e.getMessage(), e);
        }
    }
}
