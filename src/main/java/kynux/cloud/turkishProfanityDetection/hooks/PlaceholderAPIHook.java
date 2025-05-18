package kynux.cloud.turkishProfanityDetection.hooks;

import kynux.cloud.turkishProfanityDetection.TurkishProfanityDetection;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlaceholderAPIHook {
    private final TurkishProfanityDetection plugin;
    private boolean placeholderAPIEnabled = false;
    private final Map<UUID, Integer> playerProfanityCount = new HashMap<>();
    
    public PlaceholderAPIHook(@NotNull TurkishProfanityDetection plugin) {
        this.plugin = plugin;
        
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            registerPlaceholders();
            placeholderAPIEnabled = true;
            plugin.getLogger().info("PlaceholderAPI entegrasyonu etkinleştirildi!");
        }
    }
    
    private void registerPlaceholders() {
        try {
            Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion");
            Class<?> placeholderAPI = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            ProfanityExpansion expansion = new ProfanityExpansion();
            boolean registered = false;
            try {
                registered = (boolean) placeholderAPI.getMethod("registerExpansion", Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion"))
                    .invoke(null, expansion);
            } catch (Exception e) {
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
    
    public void updatePlayerProfanityCount(UUID playerId) {
        int currentCount = playerProfanityCount.getOrDefault(playerId, 0);
        playerProfanityCount.put(playerId, currentCount + 1);
    }
    
    public int getPlayerProfanityCount(UUID playerId) {
        return playerProfanityCount.getOrDefault(playerId, 0);
    }
    
    public boolean isPlaceholderAPIEnabled() {
        return placeholderAPIEnabled;
    }
    
    public void clearData() {
        playerProfanityCount.clear();
    }
    
    public void clearPlayerData(UUID playerId) {
        playerProfanityCount.remove(playerId);
    }
    
    public class ProfanityExpansion {
        
        public String getIdentifier() {
            return "tpd";
        }
        
        public String getAuthor() {
            return plugin.getDescription().getAuthors().isEmpty() 
                ? "KynuxCloud" 
                : plugin.getDescription().getAuthors().get(0);
        }
        
        public String getVersion() {
            return plugin.getDescription().getVersion();
        }
        
        public boolean persist() {
            return true;
        }
        
        public boolean register() {
            try {
                Class<?> placeholderAPIClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                Object result = placeholderAPIClass.getMethod("registerExpansion", 
                               Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion"))
                                   .invoke(null, this);
                
                return result instanceof Boolean ? (Boolean) result : false;
            } catch (Exception e) {
                plugin.getLogger().warning("PlaceholderAPI expansion kaydedilemedi: " + e.getMessage());
                return false;
            }
        }
        
        public String onRequest(OfflinePlayer player, String identifier) {
            if (player == null) {
                return "";
            }
            
            if (identifier.equals("count")) {
                return String.valueOf(getPlayerProfanityCount(player.getUniqueId()));
            }
            
            if (identifier.equals("total")) {
                int totalCount = 0;
                for (int count : playerProfanityCount.values()) {
                    totalCount += count;
                }
                return String.valueOf(totalCount);
            }
            
            if (identifier.equals("status")) {
                return "Aktif";
            }
            
            return null;
        }
    }
}
