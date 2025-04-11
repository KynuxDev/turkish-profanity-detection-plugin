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
        try {
            // PlaceholderExpansion sınıfını reflection ile yükle
            Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion");
            
            // PAPI registerExpansion metodunu reflection ile çağır
            Class<?> placeholderAPI = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            
            // Expansion nesnesini oluştur
            ProfanityExpansion expansion = new ProfanityExpansion();
            
            // Expansion'ı kaydet
            boolean registered = false;
            try {
                // Önce PlaceholderAPI class'ındaki registerExpansion metodunu bulmayı dene
                registered = (boolean) placeholderAPI.getMethod("registerExpansion", Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion"))
                    .invoke(null, expansion);
            } catch (Exception e) {
                // Eğer yukarıdaki metod bulunamazsa, expansion'daki register metodunu dene
                registered = (boolean) expansion.getClass().getMethod("register").invoke(expansion);
            }
            
            if (registered) {
                plugin.getLogger().info("PlaceholderAPI expansion başarıyla kaydedildi!");
            } else {
                plugin.getLogger().warning("PlaceholderAPI expansion kaydedilemedi!");
            }
        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("PlaceholderAPI bulunamadı! Placeholder desteği devre dışı.");
        } catch (Exception e) {
            plugin.getLogger().warning("PlaceholderAPI expansion kaydedilirken hata: " + e.getMessage());
            e.printStackTrace();
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
     * Bu sınıf, PlaceholderAPI eklentiden bağımsız olarak derlenebilmesi için
     * özel olarak tanımlanmıştır. PlaceholderExpansion'ı direkt extend etmek yerine
     * reflection ile metotları çağırıyoruz.
     */
    public class ProfanityExpansion /* extends PlaceholderExpansion */ {
        
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
            return plugin.getDescription().getAuthors().isEmpty() 
                ? "KynuxCloud" 
                : plugin.getDescription().getAuthors().get(0);
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
         * Expansion'ı kaydeder.
         */
        public boolean register() {
            try {
                // PlaceholderAPI ile ilgili sınıfları reflection ile yükle
                Class<?> placeholderAPIClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                
                // register metodunu çağır
                Object result = placeholderAPIClass.getMethod("registerExpansion", 
                               Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion"))
                                   .invoke(null, this);
                
                return result instanceof Boolean ? (Boolean) result : false;
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
                return "Aktif";
            }
            
            return null;
        }
    }
}
