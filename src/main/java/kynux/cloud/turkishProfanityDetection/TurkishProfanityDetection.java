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

public final class TurkishProfanityDetection extends JavaPlugin {
    private ProfanityApiService profanityApiService;
    private ChatListener chatListener;
    private ProfanityStorage profanityStorage;
    private DiscordWebhook discordWebhook;
    private AdminGui adminGui;
    private PlaceholderAPIHook placeholderAPIHook;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        this.profanityApiService = new ProfanityApiService(getConfig(), getLogger());
        this.profanityStorage = new ProfanityStorage(this);
        this.discordWebhook = new DiscordWebhook(this);
        this.adminGui = new AdminGui(this, profanityStorage);
        this.placeholderAPIHook = new PlaceholderAPIHook(this);
        
        this.chatListener = new ChatListener(this);
        getServer().getPluginManager().registerEvents(chatListener, this);
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        
        registerCommands();
        
        getLogger().info("Turkish Profanity Detection eklentisi etkinleştirildi!");
        getLogger().info("Küfür tespit API'si: " + getConfig().getString("api.url", "https://ai.kynux.cloud/v1"));
        
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
        if (profanityStorage != null) {
            profanityStorage.shutdown();
        }
        
        getLogger().info("Turkish Profanity Detection eklentisi devre dışı bırakıldı.");
    }
    
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
    
    public ProfanityApiService getProfanityApiService() {
        return profanityApiService;
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
    
    public void reloadPlugin() {
        try {
            reloadConfig();
            
            if (profanityStorage != null) {
                profanityStorage.shutdown();
            }
            
            this.profanityApiService = new ProfanityApiService(getConfig(), getLogger());
            this.profanityStorage = new ProfanityStorage(this);
            this.discordWebhook = new DiscordWebhook(this);
            this.adminGui = new AdminGui(this, profanityStorage);
            this.placeholderAPIHook = new PlaceholderAPIHook(this);
            
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
