package kynux.cloud.turkishProfanityDetection.gui;

import kynux.cloud.turkishProfanityDetection.TurkishProfanityDetection;
import kynux.cloud.turkishProfanityDetection.model.ProfanityRecord;
import kynux.cloud.turkishProfanityDetection.storage.ProfanityStorage;
import kynux.cloud.turkishProfanityDetection.utils.*;
import kynux.cloud.turkishProfanityDetection.utils.VersionUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class AdminGui {
    private final TurkishProfanityDetection plugin;
    private final ProfanityStorage storage;
    private final String adminMenuTitle;
    private final String statsMenuTitle;
    private final int adminMenuRows;
    private final int statsMenuRows;
    private final int itemsPerPage;
    
    private static final Map<UUID, Integer> playerStatsPages = new HashMap<>();
    private static final Map<UUID, UUID> selectedPlayers = new HashMap<>();
    
    public AdminGui(@NotNull TurkishProfanityDetection plugin, @NotNull ProfanityStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
        
        FileConfiguration config = plugin.getConfig();
        
        this.adminMenuTitle = ChatColor.translateAlternateColorCodes('&', 
                config.getString("gui.admin-menu.title", "&8[&c⚠&8] &fKüfür Koruma Paneli"));
        this.statsMenuTitle = ChatColor.translateAlternateColorCodes('&', 
                config.getString("gui.stats-menu.title", "&8[&b📊&8] &fKüfür İstatistikleri"));
        
        this.adminMenuRows = Math.min(Math.max(config.getInt("gui.admin-menu.rows", 5), 1), 6);
        this.statsMenuRows = Math.min(Math.max(config.getInt("gui.stats-menu.rows", 5), 1), 6);
        this.itemsPerPage = Math.min(Math.max(config.getInt("gui.stats-menu.items-per-page", 36), 1), 45);
    }
    
    public void openAdminMenu(@NotNull Player player) {
        Inventory inventory = Bukkit.createInventory(null, adminMenuRows * 9, adminMenuTitle);
        
        ItemStack playersItem = createItem(VersionUtil.getPlayerHeadMaterial(), 
                "&b&lOyuncu Listesi", 
                Arrays.asList(
                        "&7Küfür eden oyuncuların listesini gösterir.",
                        "&7İstatistikleri incelemek için tıklayın."
                ));
        inventory.setItem(11, playersItem);
        
        ItemStack clearAllItem = createItem(VersionUtil.getBarrierMaterial(), 
                "&c&lTüm İstatistikleri Temizle", 
                Arrays.asList(
                        "&7Tüm oyuncuların küfür istatistiklerini",
                        "&7kalıcı olarak temizler.",
                        "&c&lDikkat: &7Bu işlem geri alınamaz!"
                ));
        inventory.setItem(15, clearAllItem);
        
        ItemStack reloadItem = createItem(VersionUtil.getRepeaterMaterial(), 
                "&a&lYeniden Yükle", 
                Arrays.asList(
                        "&7Eklenti yapılandırmasını yeniler.",
                        "&7Ayarlar dosyasına yapılan değişiklikler",
                        "&7yeniden başlatmadan etkinleştirilir."
                ));
        inventory.setItem(31, reloadItem);
        
        ItemStack infoItem = createItem(VersionUtil.getBookMaterial(), 
                "&e&lEklenti Bilgisi", 
                Arrays.asList(
                        "&fTurkish Profanity Detection",
                        "&7Sürüm: &f" + plugin.getDescription().getVersion(),
                        "&7Yapımcı: &fKynux Cloud",
                        "",
                        "&7Türkçe küfür ve hakaret tespiti"
                ));
        inventory.setItem(40, infoItem);
        
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                if (i < 9 || i >= inventory.getSize() - 9 || i % 9 == 0 || i % 9 == 8) {
                    inventory.setItem(i, createItem(VersionUtil.getBlackGlassPaneMaterial(), " ", null));
                }
            }
        }
        
        player.openInventory(inventory);
    }
    
    public void openPlayerListMenu(@NotNull Player player, int page) {
        Map<UUID, List<ProfanityRecord>> allRecords = storage.getAllRecords();
        
        if (allRecords.isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfig().getString("messages.prefix") + "&7Hiç küfür istatistiği bulunamadı."));
            return;
        }
        
        List<UUID> playerIds = new ArrayList<>(allRecords.keySet());
        
        int maxPages = (int) Math.ceil((double) playerIds.size() / itemsPerPage);
        if (page < 0) page = 0;
        if (page >= maxPages) page = maxPages - 1;
        playerStatsPages.put(player.getUniqueId(), page);
        
        Inventory inventory = Bukkit.createInventory(null, statsMenuRows * 9, 
                statsMenuTitle + " - Sayfa " + (page + 1) + "/" + maxPages);
        
        if (page > 0) {
            inventory.setItem(statsMenuRows * 9 - 9, createItem(VersionUtil.getArrowMaterial(), "&7Önceki Sayfa", null));
        }
        
        if (page < maxPages - 1) {
            inventory.setItem(statsMenuRows * 9 - 1, createItem(VersionUtil.getArrowMaterial(), "&7Sonraki Sayfa", null));
        }
        
        inventory.setItem(statsMenuRows * 9 - 5, createItem(VersionUtil.getBarrierMaterial(), "&cGeri Dön", null));
        
        int startIdx = page * itemsPerPage;
        int endIdx = Math.min(startIdx + itemsPerPage, playerIds.size());
        
        for (int i = startIdx; i < endIdx; i++) {
            UUID playerId = playerIds.get(i);
            List<ProfanityRecord> playerRecords = allRecords.get(playerId);
            
            if (playerRecords != null && !playerRecords.isEmpty()) {
                ProfanityRecord latestRecord = playerRecords.get(playerRecords.size() - 1);
                String playerName = latestRecord.getPlayerName();
                
                ItemStack playerHead = VersionUtil.createPlayerHead(playerName);
                SkullMeta skullMeta = (SkullMeta) playerHead.getItemMeta();
                
                if (skullMeta != null) {
                    skullMeta.setDisplayName(ChatColor.GOLD + playerName);
                    
                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.GRAY + "Toplam Küfür: " + ChatColor.WHITE + playerRecords.size());
                    lore.add(ChatColor.GRAY + "Son Küfür: " + ChatColor.WHITE + latestRecord.getFormattedTimestamp());
                    lore.add("");
                    lore.add(ChatColor.YELLOW + "Detaylar için tıklayın");
                    
                    skullMeta.setLore(lore);
                    skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(playerName));
                    playerHead.setItemMeta(skullMeta);
                }
                
                inventory.addItem(playerHead);
            }
        }
        
        player.openInventory(inventory);
    }
    
    public void openPlayerStatsMenu(@NotNull Player viewer, @NotNull UUID targetPlayerId, int page) {
        List<ProfanityRecord> records = storage.getPlayerRecords(targetPlayerId);
        
        String playerName = "Bilinmeyen Oyuncu";
        if (!records.isEmpty()) {
            playerName = records.get(0).getPlayerName();
        } else {
            viewer.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfig().getString("messages.prefix") + 
                    plugin.getConfig().getString("messages.admin.no-stats", "&7%player% için kayıtlı küfür istatistiği bulunamadı.")
                            .replace("%player%", playerName)));
            return;
        }
        
        int maxPages = (int) Math.ceil((double) records.size() / itemsPerPage);
        if (page < 0) page = 0;
        if (page >= maxPages) page = maxPages - 1;
        
        Inventory inventory = Bukkit.createInventory(null, statsMenuRows * 9, 
                statsMenuTitle + " - " + playerName + " (" + (page + 1) + "/" + maxPages + ")");
        
        if (page > 0) {
            inventory.setItem(statsMenuRows * 9 - 9, createItem(VersionUtil.getArrowMaterial(), "&7Önceki Sayfa", null));
        }
        
        if (page < maxPages - 1) {
            inventory.setItem(statsMenuRows * 9 - 1, createItem(VersionUtil.getArrowMaterial(), "&7Sonraki Sayfa", null));
        }
        
        inventory.setItem(statsMenuRows * 9 - 6, createItem(VersionUtil.getArrowMaterial(), "&aOyuncu Listesine Dön", null));
        
        inventory.setItem(statsMenuRows * 9 - 4, createItem(VersionUtil.getBarrierMaterial(), 
                "&c" + playerName + " İstatistiklerini Temizle", 
                Collections.singletonList("&7Bu oyuncunun tüm istatistiklerini siler.")));
        
        Collections.reverse(records);
        int startIdx = page * itemsPerPage;
        int endIdx = Math.min(startIdx + itemsPerPage, records.size());
        
        for (int i = startIdx; i < endIdx; i++) {
            ProfanityRecord record = records.get(i);
            
            Material material;
            switch (record.getSeverityLevel()) {
                case 5:
                    material = VersionUtil.getColoredWoolMaterial("RED");
                    break;
                case 4:
                    material = VersionUtil.getColoredWoolMaterial("ORANGE");
                    break;
                case 3:
                    material = VersionUtil.getColoredWoolMaterial("YELLOW");
                    break;
                case 2:
                    material = VersionUtil.getColoredWoolMaterial("LIME");
                    break;
                default:
                    material = VersionUtil.getColoredWoolMaterial("GREEN");
                    break;
            }
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Zaman: " + ChatColor.WHITE + record.getFormattedTimestamp());
            lore.add(ChatColor.GRAY + "Kelime: " + ChatColor.RED + record.getWord());
            lore.add(ChatColor.GRAY + "Kategori: " + ChatColor.WHITE + record.getCategory());
            lore.add(ChatColor.GRAY + "Şiddet: " + ChatColor.WHITE + record.getSeverityLevel() + "/5");
            lore.add(ChatColor.GRAY + "AI Tarafından: " + ChatColor.WHITE + (record.isAiDetected() ? "Evet" : "Hayır"));
            lore.add("");
            lore.add(ChatColor.GRAY + "Orijinal Mesaj:");
            
            String message = record.getOriginalMessage();
            for (int j = 0; j < message.length(); j += 30) {
                int end = Math.min(j + 30, message.length());
                lore.add(ChatColor.WHITE + "" + ChatColor.ITALIC + message.substring(j, end));
            }
            
            inventory.addItem(createItem(material, 
                    "&f" + record.getFormattedTimestamp(), lore));
        }
        
        viewer.openInventory(inventory);
    }
    
    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item;
        if (!VersionUtil.isVersion1_13Plus() && material == VersionUtil.getPlayerHeadMaterial()) {
            item = VersionUtil.createItem(material, (byte) 3);
        } else {
            item = new ItemStack(material);
        }
        
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            if (name != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            }
            
            if (lore != null) {
                meta.setLore(lore.stream()
                        .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                        .collect(Collectors.toList()));
            }
            
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    public UUID getSelectedPlayer(Player player) {
        return selectedPlayers.get(player.getUniqueId());
    }
    
    public void setSelectedPlayer(Player player, UUID targetId) {
        selectedPlayers.put(player.getUniqueId(), targetId);
    }
    
    public int getCurrentPage(Player player) {
        return playerStatsPages.getOrDefault(player.getUniqueId(), 0);
    }
}
