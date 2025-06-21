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
            plugin.getLogger().info("PlaceholderAPI tespit edildi, ancak expansion kaydı devre dışı bırakıldı.");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("PlaceholderAPI bulunamadı veya eski bir sürüm kullanılıyor.");
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

    private class ProfanityExpansion /* extends PlaceholderExpansion */ {
        
        public String getIdentifier() {
            return "tpd";
        }

        public String getAuthor() {
            return plugin.getDescription().getAuthors().get(0);
        }

        public String getVersion() {
            return plugin.getDescription().getVersion();
        }
        public boolean persist() {
            return true;
        }
        
        public boolean register() {
            try {
                Class<?> expansionClass = Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion");
                Object expansion = getClass().newInstance();
                
                expansionClass.getMethod("register").invoke(expansion);
                return true;
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
