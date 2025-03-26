package kynux.cloud.turkishProfanityDetection.commands;

import kynux.cloud.turkishProfanityDetection.TurkishProfanityDetection;
import kynux.cloud.turkishProfanityDetection.model.ProfanityRecord;
import kynux.cloud.turkishProfanityDetection.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Eklentinin komutlarını işleyen sınıf.
 */
public class PluginCommand implements CommandExecutor, TabCompleter {
    private final TurkishProfanityDetection plugin;
    private final String permissionCommands;
    private final String permissionAdmin;
    private final String permissionStats;

    /**
     * Komut işleyiciyi başlatır.
     *
     * @param plugin Eklenti ana sınıfı
     */
    public PluginCommand(@NotNull TurkishProfanityDetection plugin) {
        this.plugin = plugin;
        this.permissionCommands = plugin.getConfig().getString("permissions.commands", "turkishprofanitydetection.commands");
        this.permissionAdmin = plugin.getConfig().getString("permissions.admin", "turkishprofanitydetection.admin");
        this.permissionStats = plugin.getConfig().getString("permissions.statistics", "turkishprofanitydetection.statistics");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Yetki kontrolü
        if (!sender.hasPermission(permissionCommands)) {
            MessageUtils.sendMessage(sender, plugin.getConfig().getString("messages.prefix") +
                    plugin.getConfig().getString("messages.no-permission", "&cBu komutu kullanmak için yetkiniz yok."));
            return true;
        }

        // Parametresiz komut - yardım menüsü
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        // Alt komutları işle
        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reloadPlugin();
                MessageUtils.sendMessage(sender, plugin.getConfig().getString("messages.prefix") +
                        plugin.getConfig().getString("messages.reload", "&aEklenti yeniden yüklendi!"));
                break;
                
            case "version":
                MessageUtils.sendMessage(sender, plugin.getConfig().getString("messages.prefix") +
                        "&bTurkish Profanity Detection &fv" + plugin.getDescription().getVersion());
                MessageUtils.sendMessage(sender, plugin.getConfig().getString("messages.prefix") +
                        "&7Geliştirici: &fKynux Cloud");
                break;
                
            case "admin":
                if (sender instanceof Player) {
                    if (sender.hasPermission(permissionAdmin)) {
                        Player player = (Player) sender;
                        plugin.getAdminGui().openAdminMenu(player);
                    } else {
                        MessageUtils.sendMessage(sender, plugin.getConfig().getString("messages.prefix") +
                                plugin.getConfig().getString("messages.no-permission"));
                    }
                } else {
                    MessageUtils.sendMessage(sender, plugin.getConfig().getString("messages.prefix") +
                            "&cBu komutu yalnızca oyuncular kullanabilir.");
                }
                break;
                
            case "stats":
                if (sender.hasPermission(permissionStats)) {
                    if (args.length < 2) {
                        MessageUtils.sendMessage(sender, plugin.getConfig().getString("messages.prefix") +
                                "&cLütfen bir oyuncu adı belirtin: /tpd stats <oyuncu>");
                    } else {
                        String playerName = args[1];
                        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
                        
                        if (target == null || !target.hasPlayedBefore()) {
                            MessageUtils.sendMessage(sender, plugin.getConfig().getString("messages.prefix") +
                                    plugin.getConfig().getString("messages.admin.player-not-found", 
                                            "&cBelirtilen oyuncu bulunamadı."));
                            return true;
                        }
                        
                        UUID targetId = target.getUniqueId();
                        List<ProfanityRecord> records = plugin.getProfanityStorage().getPlayerRecords(targetId);
                        
                        if (records.isEmpty()) {
                            MessageUtils.sendMessage(sender, plugin.getConfig().getString("messages.prefix") +
                                    plugin.getConfig().getString("messages.admin.no-stats", "&7%player% için kayıtlı küfür istatistiği bulunamadı.")
                                            .replace("%player%", playerName));
                            return true;
                        }
                        
                        // Oyuncu istatistiklerini göster
                        MessageUtils.sendMessage(sender, plugin.getConfig().getString("messages.prefix") +
                                plugin.getConfig().getString("messages.admin.stats-header", "&6%player% &fiçin küfür istatistikleri:")
                                        .replace("%player%", playerName));
                        
                        int count = 0;
                        for (ProfanityRecord record : records) {
                            count++;
                            if (count > 10) {
                                MessageUtils.sendMessage(sender, "&7... ve " + (records.size() - 10) + " kayıt daha");
                                break;
                            }
                            
                            MessageUtils.sendMessage(sender, plugin.getConfig().getString("messages.admin.stats-line", 
                                    "&7- &f%date%: &c%word% &7(%severity% seviye)")
                                    .replace("%date%", record.getFormattedTimestamp())
                                    .replace("%word%", record.getWord())
                                    .replace("%severity%", String.valueOf(record.getSeverityLevel())));
                        }
                    }
                } else {
                    MessageUtils.sendMessage(sender, plugin.getConfig().getString("messages.prefix") +
                            plugin.getConfig().getString("messages.no-permission"));
                }
                break;
                
            case "clear":
                if (sender.hasPermission(permissionAdmin)) {
                    if (args.length < 2) {
                        MessageUtils.sendMessage(sender, plugin.getConfig().getString("messages.prefix") +
                                "&cLütfen bir oyuncu adı belirtin: /tpd clear <oyuncu>");
                    } else {
                        String playerName = args[1];
                        
                        if (playerName.equalsIgnoreCase("all")) {
                            plugin.getProfanityStorage().clearAllRecords();
                            MessageUtils.sendMessage(sender, plugin.getConfig().getString("messages.prefix") +
                                    "&aTüm küfür istatistikleri temizlendi!");
                            return true;
                        }
                        
                        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
                        
                        if (target == null || !target.hasPlayedBefore()) {
                            MessageUtils.sendMessage(sender, plugin.getConfig().getString("messages.prefix") +
                                    plugin.getConfig().getString("messages.admin.player-not-found"));
                            return true;
                        }
                        
                        UUID targetId = target.getUniqueId();
                        plugin.getProfanityStorage().clearPlayerRecords(targetId);
                        
                        MessageUtils.sendMessage(sender, plugin.getConfig().getString("messages.prefix") +
                                plugin.getConfig().getString("messages.admin.cleared-stats", "&a%player% için tüm küfür istatistikleri temizlendi.")
                                        .replace("%player%", playerName));
                    }
                } else {
                    MessageUtils.sendMessage(sender, plugin.getConfig().getString("messages.prefix") +
                            plugin.getConfig().getString("messages.no-permission"));
                }
                break;
                
            case "help":
            default:
                showHelp(sender);
                break;
        }

        return true;
    }

    /**
     * Yardım menüsünü gösterir.
     *
     * @param sender Komut gönderen
     */
    private void showHelp(CommandSender sender) {
        String prefix = plugin.getConfig().getString("messages.prefix");
        
        MessageUtils.sendMessage(sender, "&8&m----------------------------------------");
        MessageUtils.sendMessage(sender, prefix + "&bTurkish Profanity Detection &f- Komutlar");
        MessageUtils.sendMessage(sender, "&8&m----------------------------------------");
        MessageUtils.sendMessage(sender, prefix + "&f/tpd help &7- Bu yardım menüsünü gösterir");
        MessageUtils.sendMessage(sender, prefix + "&f/tpd reload &7- Eklentiyi yeniden yükler");
        MessageUtils.sendMessage(sender, prefix + "&f/tpd version &7- Eklenti sürümünü gösterir");
        
        // Admin komutları
        if (sender.hasPermission(permissionAdmin)) {
            MessageUtils.sendMessage(sender, "&8&m----------------------------------------");
            MessageUtils.sendMessage(sender, prefix + "&c&lAdmin Komutları:");
            MessageUtils.sendMessage(sender, prefix + "&f/tpd admin &7- Admin arayüzünü açar");
            MessageUtils.sendMessage(sender, prefix + "&f/tpd stats <oyuncu> &7- Oyuncu istatistiklerini gösterir");
            MessageUtils.sendMessage(sender, prefix + "&f/tpd clear <oyuncu|all> &7- İstatistikleri temizler");
        }
        
        MessageUtils.sendMessage(sender, "&8&m----------------------------------------");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(Arrays.asList("help", "reload", "version"));
            
            // Admin komutları için yetki kontrolü
            if (sender.hasPermission(permissionAdmin)) {
                completions.add("admin");
                completions.add("stats");
                completions.add("clear");
            }
            
            // Yetki kontrolü
            if (!sender.hasPermission(permissionCommands)) {
                return new ArrayList<>();
            }
            
            // Başlangıç filtreleme
            String arg = args[0].toLowerCase();
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(arg))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            // İkinci parametre için tamamlama
            if (args[0].equalsIgnoreCase("stats") || args[0].equalsIgnoreCase("clear")) {
                if (sender.hasPermission(permissionAdmin)) {
                    List<String> completions = new ArrayList<>();
                    
                    // Tüm çevrimiçi oyuncuları ekle
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        completions.add(player.getName());
                    }
                    
                    // "clear" komutu için "all" seçeneğini ekle
                    if (args[0].equalsIgnoreCase("clear")) {
                        completions.add("all");
                    }
                    
                    // Başlangıç filtreleme
                    String arg = args[1].toLowerCase();
                    return completions.stream()
                            .filter(s -> s.toLowerCase().startsWith(arg))
                            .collect(Collectors.toList());
                }
            }
        }
        
        return new ArrayList<>();
    }
}
