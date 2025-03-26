package kynux.cloud.turkishProfanityDetection.hooks;

import kynux.cloud.turkishProfanityDetection.TurkishProfanityDetection;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * PlaceholderAPI entegrasyonu için sınıf.
 * Eğer PlaceholderAPI yüklüyse, bazı placeholderlerin kullanılabilir olmasını sağlar.
 */
public class PlaceholderAPIHook {
    private final TurkishProfanityDetection plugin;
    private boolean placeholderAPIEnabled = false;
    private final Map<UUID, Integer> playerProfanityCount = new HashMap<>();
    
    /**
     * PlaceholderAPI hook başlatıcı.
     *
     * @param plugin Plugin ana sınıfı
     */
    public PlaceholderAPIHook(@NotNull TurkishProfanityDetection plugin) {
        this.plugin = plugin;
        
        // PlaceholderAPI entegrasyonunu kontrol et
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            registerPlaceholders();
            placeholderAPIEnabled = true;
            plugin.getLogger().info("PlaceholderAPI entegrasyonu etkinleştirildi!");
        }
    }
    
    /**
     * Placeholderleri kaydeder.
     */
    private void registerPlaceholders() {
        // PlaceholderAPI sınıfı reflection ile yüklenir çünkü plugin olmayabilir
        try {
            Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion");
            new ProfanityExpansion().register();
        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("PlaceholderAPI bulunamadı veya eski bir sürüm kullanılıyor.");
        }
    }
    
    /**
     * Bir oyuncunun küfür sayısını günceller.
     *
     * @param playerId Oyuncu UUID'si
     */
    public void updatePlayerProfanityCount(UUID playerId) {
        int currentCount = playerProfanityCount.getOrDefault(playerId, 0);
        playerProfanityCount.put(playerId, currentCount + 1);
    }
    
    /**
     * Bir oyuncunun küfür sayısını getirir.
     *
     * @param playerId Oyuncu UUID'si
     * @return Küfür sayısı
     */
    public int getPlayerProfanityCount(UUID playerId) {
        return playerProfanityCount.getOrDefault(playerId, 0);
    }
    
    /**
     * PlaceholderAPI yüklü mü kontrolü.
     *
     * @return PlaceholderAPI yüklüyse true
     */
    public boolean isPlaceholderAPIEnabled() {
        return placeholderAPIEnabled;
    }
    
    /**
     * İstatistik verilerini sıfırlar.
     */
    public void clearData() {
        playerProfanityCount.clear();
    }
    
    /**
     * Verilen oyuncunun istatistiklerini siler.
     *
     * @param playerId Oyuncu UUID'si
     */
    public void clearPlayerData(UUID playerId) {
        playerProfanityCount.remove(playerId);
    }
    
    /**
     * PlaceholderAPI Expansion sınıfı.
     * Bu sınıf, PlaceholderAPI olmadığında da compile edilebilmesi için içeride tanımlanmıştır.
     */
    private class ProfanityExpansion /* extends PlaceholderExpansion */ {
        
        /**
         * Expansion kimliğini döndürür.
         */
        public String getIdentifier() {
            return "tpd";
        }
        
        /**
         * Expansion yazarını döndürür.
         */
        public String getAuthor() {
            return plugin.getDescription().getAuthors().get(0);
        }
        
        /**
         * Expansion versiyonunu döndürür.
         */
        public String getVersion() {
            return plugin.getDescription().getVersion();
        }
        
        /**
         * Sürekli olduğunu belirtir (reload gerektirmez).
         */
        public boolean persist() {
            return true;
        }
        
        /**
         * Expansionu kaydeder.
         */
        public boolean register() {
            // PlaceholderAPI yüklü değilse reflection hatası olmaz
            try {
                Class<?> expansionClass = Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion");
                Object expansion = getClass().newInstance();
                
                // register() metodunu çağır
                expansionClass.getMethod("register").invoke(expansion);
                return true;
            } catch (Exception e) {
                plugin.getLogger().warning("PlaceholderAPI expansion kaydedilemedi: " + e.getMessage());
                return false;
            }
        }
        
        /**
         * Placeholder değerini döndürür.
         */
        public String onRequest(OfflinePlayer player, String identifier) {
            if (player == null) {
                return "";
            }
            
            // %tpd_count% - Oyuncunun toplam küfür sayısı
            if (identifier.equals("count")) {
                return String.valueOf(getPlayerProfanityCount(player.getUniqueId()));
            }
            
            // %tpd_total% - Sunucudaki toplam küfür sayısı
            if (identifier.equals("total")) {
                int totalCount = 0;
                for (int count : playerProfanityCount.values()) {
                    totalCount += count;
                }
                return String.valueOf(totalCount);
            }
            
            // %tpd_status% - API durumu
            if (identifier.equals("status")) {
                return "Aktif"; // Gelecekte API'nin gerçekten çalışıp çalışmadığını kontrol edebiliriz
            }
            
            return null;
        }
    }
}
