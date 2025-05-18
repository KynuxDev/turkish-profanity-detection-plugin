package kynux.cloud.turkishProfanityDetection.listeners;

import kynux.cloud.turkishProfanityDetection.TurkishProfanityDetection;
import kynux.cloud.turkishProfanityDetection.utils.MessageUtils;
import kynux.cloud.turkishProfanityDetection.utils.VersionUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

public class GuiListener implements Listener {
    private final TurkishProfanityDetection plugin;
    private final String statsMenuTitle;
    private final String adminMenuTitle;
    
    public GuiListener(@NotNull TurkishProfanityDetection plugin) {
        this.plugin = plugin;
        
        this.adminMenuTitle = ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfig().getString("gui.admin-menu.title", "&8[&c⚠&8] &fKüfür Koruma Paneli"));
        this.statsMenuTitle = ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfig().getString("gui.stats-menu.title", "&8[&b📊&8] &fKüfür İstatistikleri"));
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        if (title.equals(adminMenuTitle)) {
            event.setCancelled(true);
            
            if (event.getCurrentItem() == null) {
                return;
            }
            
            if (event.getSlot() == 11) {
                plugin.getAdminGui().openPlayerListMenu(player, 0);
            } else if (event.getSlot() == 15) {
                plugin.getProfanityStorage().clearAllRecords();
                MessageUtils.sendMessage(player, plugin.getConfig().getString("messages.prefix") + 
                        "&aTüm küfür istatistikleri temizlendi!");
                player.closeInventory();
            } else if (event.getSlot() == 31) {
                plugin.reloadPlugin();
                MessageUtils.sendMessage(player, plugin.getConfig().getString("messages.prefix") + 
                        plugin.getConfig().getString("messages.reload", "&aEklenti yeniden yüklendi!"));
                player.closeInventory();
            }
        }
        else if (title.startsWith(statsMenuTitle)) {
            event.setCancelled(true);
            
            if (event.getCurrentItem() == null) {
                return;
            }
            
            int currentPage = plugin.getAdminGui().getCurrentPage(player);
            
            if (title.contains("Sayfa")) {
                if (event.getSlot() == event.getInventory().getSize() - 9) {
                    plugin.getAdminGui().openPlayerListMenu(player, currentPage - 1);
                } else if (event.getSlot() == event.getInventory().getSize() - 1) {
                    plugin.getAdminGui().openPlayerListMenu(player, currentPage + 1);
                } else if (event.getSlot() == event.getInventory().getSize() - 5) {
                    plugin.getAdminGui().openAdminMenu(player);
                } else if (event.getCurrentItem().getType() == VersionUtil.getPlayerHeadMaterial()) {
                    ItemStack item = event.getCurrentItem();
                    SkullMeta meta = (SkullMeta) item.getItemMeta();
                    
                    if (meta != null && meta.getOwningPlayer() != null) {
                        OfflinePlayer target = meta.getOwningPlayer();
                        UUID targetId = target.getUniqueId();
                        plugin.getAdminGui().setSelectedPlayer(player, targetId);
                        plugin.getAdminGui().openPlayerStatsMenu(player, targetId, 0);
                    }
                }
            }
            else if (title.contains("(")) {
                UUID selectedPlayerId = plugin.getAdminGui().getSelectedPlayer(player);
                
                if (selectedPlayerId != null) {
                    int page = 0;
                    if (title.contains("(") && title.contains(")")) {
                        String pageStr = title.substring(title.lastIndexOf("(") + 1, title.lastIndexOf(")"));
                        String[] parts = pageStr.split("/");
                        if (parts.length > 0) {
                            try {
                                page = Integer.parseInt(parts[0].trim()) - 1;
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                    
                    if (event.getSlot() == event.getInventory().getSize() - 9) {
                        plugin.getAdminGui().openPlayerStatsMenu(player, selectedPlayerId, page - 1);
                    } else if (event.getSlot() == event.getInventory().getSize() - 1) {
                        plugin.getAdminGui().openPlayerStatsMenu(player, selectedPlayerId, page + 1);
                    } else if (event.getSlot() == event.getInventory().getSize() - 6) {
                        plugin.getAdminGui().openPlayerListMenu(player, currentPage);
                    } else if (event.getSlot() == event.getInventory().getSize() - 4) {
                        plugin.getProfanityStorage().clearPlayerRecords(selectedPlayerId);
                        
                        String playerName = "Bilinmeyen Oyuncu";
                        Map<UUID, String> playerNames = new java.util.HashMap<>();
                        
                        MessageUtils.sendMessage(player, plugin.getConfig().getString("messages.prefix") + 
                                plugin.getConfig().getString("messages.admin.cleared-stats", "&a%player% için tüm küfür istatistikleri temizlendi.")
                                        .replace("%player%", playerName));
                        
                        plugin.getAdminGui().openPlayerListMenu(player, currentPage);
                    }
                }
            }
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
    }
}
