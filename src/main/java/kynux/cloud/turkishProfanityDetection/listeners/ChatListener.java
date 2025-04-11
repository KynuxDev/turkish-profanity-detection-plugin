package kynux.cloud.turkishProfanityDetection.listeners;

import kynux.cloud.turkishProfanityDetection.TurkishProfanityDetection;
import kynux.cloud.turkishProfanityDetection.api.ProfanityResponse;
import kynux.cloud.turkishProfanityDetection.model.ProfanityRecord;
import kynux.cloud.turkishProfanityDetection.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Oyuncu chat mesajlarını dinleyip, küfür kontrolü yapan sınıf.
 */
public class ChatListener implements Listener {
    private final TurkishProfanityDetection plugin;
    private final boolean cancelMessage;
    private final boolean logEnabled;
    private final boolean logConsole;
    private final boolean logFile;
    private final String logFilePath;
    private final boolean commandsEnabled;
    private final List<String> commands;
    private final boolean severityActionsEnabled;
    private final Map<Integer, List<String>> severityCommands;
    private final String bypassPermission;
    private final boolean statisticsEnabled;
    
    // Mesaj hızı sınırlama için değişkenler
    private final boolean rateLimitEnabled;
    private final int maxMessages;
    private final int timeWindow;
    private final String rateLimitAction;
    private final Map<UUID, MessageCounter> playerMessageCounts = new HashMap<>();
    
    /**
     * Chat dinleyiciyi başlatır.
     *
     * @param plugin Eklenti ana sınıfı
     */
    public ChatListener(@NotNull TurkishProfanityDetection plugin) {
        this.plugin = plugin;
        
        // Ayarları yükle
        this.cancelMessage = plugin.getConfig().getBoolean("actions.cancel-message", true);
        this.logEnabled = plugin.getConfig().getBoolean("actions.log.enabled", true);
        this.logConsole = plugin.getConfig().getBoolean("actions.log.console", true);
        this.logFile = plugin.getConfig().getBoolean("actions.log.file", true);
        this.logFilePath = plugin.getConfig().getString("actions.log.file-path", "logs/profanity.log");
        this.commandsEnabled = plugin.getConfig().getBoolean("actions.commands.enabled", true);
        this.commands = plugin.getConfig().getStringList("actions.commands.list");
        this.bypassPermission = plugin.getConfig().getString("permissions.bypass", "turkishprofanitydetection.bypass");
        this.statisticsEnabled = plugin.getConfig().getBoolean("statistics.enabled", true);
        
        // Küfür seviyesine göre komutlar
        this.severityActionsEnabled = plugin.getConfig().getBoolean("severity-actions.enabled", true);
        this.severityCommands = new HashMap<>();
        if (severityActionsEnabled) {
            for (int i = 1; i <= 5; i++) {
                List<String> levelCommands = plugin.getConfig().getStringList("severity-actions.levels." + i + ".commands");
                if (!levelCommands.isEmpty()) {
                    severityCommands.put(i, levelCommands);
                }
            }
        }
        
        // Mesaj hızı sınırlama ayarlarını yükle
        this.rateLimitEnabled = plugin.getConfig().getBoolean("security.rate-limit.enabled", true);
        this.maxMessages = plugin.getConfig().getInt("security.rate-limit.max-messages", 5);
        this.timeWindow = plugin.getConfig().getInt("security.rate-limit.time-window", 10);
        this.rateLimitAction = plugin.getConfig().getString("security.rate-limit.action", "mute %player% 5m Spam");
    }
    
    /**
     * Oyuncu mesaj gönderdiğinde bu metod tetiklenir.
     *
     * @param event Chat olayı
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        // Olay iptal edilmişse veya mesaj boşsa işlem yapma
        if (event.isCancelled() || event.getMessage().trim().isEmpty()) {
            return;
        }
        
        Player player = event.getPlayer();
        String message = event.getMessage();
        
        // op olan oyuncuları atla
        if (player.hasPermission(bypassPermission)) {
            return;
        }
        
        // Mesaj hızı sınırlaması kontrolü
        if (rateLimitEnabled && isRateLimited(player)) {
            event.setCancelled(true);
            
            // Sınırlama komutunu çalıştır
            if (rateLimitAction != null && !rateLimitAction.isEmpty()) {
                executeCommand(rateLimitAction.replace("%player%", player.getName()));
            }
            
            MessageUtils.sendMessage(player, plugin.getConfig().getString("messages.prefix") + 
                    "Çok hızlı mesaj gönderiyorsunuz. Lütfen yavaşlayın.");
            return;
        }
        
        // Önbellek kontrolü - aynı mesaj daha önce kontrol edilmiş mi?
        Boolean cachedResult = plugin.getFromCache(message);
        if (cachedResult != null) {
            // Önbellekte bulunan sonuca göre işlem yap
            if (cachedResult) {
                // Küfür tespit edilmişti, işlemleri yap
                // Önbellekten alınan sonuçlar için tüm parametreleri varsayılan değerlerle gönder
                handleProfanityDetected(player, message, null, true, 0.0, "", "", false);
            }
            return; // Önbellekte bulunan sonucu kullandık, API çağrısı yapmaya gerek yok
        }
        
        // Thread havuzunda asenkron olarak çalıştır (içiçe asenkron işlem yerine)
        plugin.getThreadPool().submit(() -> {
            try {
                // Mesajı API ile kontrol et
                ProfanityResponse response = plugin.getProfanityApiService().checkText(message).get();
                
                // API yanıtı geçerli değilse işlem yapma
                if (!response.isSuccess() || response.getResult() == null) {
                    // Temiz mesajı önbelleğe ekle
                    plugin.addToCache(message, false);
                    return;
                }
                
                // Yeni API yapısı isSwear alanına göre işlem yapma
                if (!response.getResult().isSwear()) {
                    // Temiz mesajı önbelleğe ekle
                    plugin.addToCache(message, false);
                    return;
                }
                
                // Küfürlü mesajı önbelleğe ekle
                plugin.addToCache(message, true);
                
                // Küfür tespit edildi, ayarlanmış işlemleri yap
                ProfanityResponse.Details details = response.getResult().getDetails();
                
                // Ana işleme fonksiyonunu çağır - yeni parametreler ile
                handleProfanityDetected(
                    player, 
                    message, 
                    details, 
                    response.getResult().isAiDetected(), 
                    response.getResult().getConfidence(),
                    response.getResult().getModel(),
                    response.getResult().getActionRecommendation(),
                    response.getResult().isSafeForMinecraft()
                );
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Küfür kontrolü sırasında hata: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Küfür tespit edildiğinde yapılacak işlemleri gerçekleştirir.
     * 
     * @param player Mesajı gönderen oyuncu
     * @param message Gönderilen mesaj
     * @param details Küfür tespiti detayları (önbellekten geliyorsa null olabilir)
     * @param isAiDetected AI tarafından tespit edildi mi
     * @param confidence Tespit güvenilirliği (0.0-1.0 arası)
     * @param model Kullanılan AI modeli
     * @param actionRecommendation Önerilen aksiyon (warn, mute, kick, ban)
     * @param isSafeForMinecraft Minecraft için güvenli içerik mi
     */
    private void handleProfanityDetected(
            Player player, 
            String message, 
            ProfanityResponse.Details details, 
            boolean isAiDetected,
            double confidence,
            String model,
            String actionRecommendation,
            boolean isSafeForMinecraft) {
        try {
            // Mesajı iptal et - sonradan engelleyeceğiz
            if (cancelMessage) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        // Oyuncuya bildirim gönder
                        MessageUtils.sendMessage(player, plugin.getConfig().getString("messages.prefix") + 
                                plugin.getConfig().getString("messages.blocked", "Mesajınız uygunsuz içerik nedeniyle engellendi."));
                        
                        // Uygunsuz mesajı sil (tüm mesajları göndeririz ve bu mesaj hariç)
                        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                            onlinePlayer.hidePlayer(plugin, player);
                            onlinePlayer.showPlayer(plugin, player);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Mesaj iptal edilirken hata: " + e.getMessage(), e);
                    }
                });
            }
            
            // Eğer details null ise (önbellekten gelen bir sonuç), detaylı işlemleri yapamayız
            if (details == null) {
                return;
            }
                
            // İstatistiği kaydet - ayrı bir thread'e gönder
            if (statisticsEnabled) {
                plugin.getThreadPool().submit(() -> {
                    try {
                        ProfanityRecord record = new ProfanityRecord(
                                player.getUniqueId(),
                                player.getName(),
                                details.getWord(),
                                details.getCategory(),
                                details.getSeverityLevel(),
                                details.getDetectedWords(),
                                message,
                                isAiDetected,
                                confidence,
                                model,
                                actionRecommendation,
                                isSafeForMinecraft
                        );
                        
                        plugin.getProfanityStorage().addRecord(record);
                        
                        // Discord webhook'a gönder - ayrı thread'de
                        if (plugin.getConfig().getBoolean("actions.discord.enabled", false)) {
                            plugin.getThreadPool().submit(() -> plugin.getDiscordWebhook().sendProfanityAlert(record));
                        }
                        
                        // PlaceholderAPI hook'unu güncelle
                        if (plugin.getPlaceholderAPIHook() != null) {
                            plugin.getPlaceholderAPIHook().updatePlayerProfanityCount(player.getUniqueId());
                        }
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "İstatistik kaydedilirken hata: " + e.getMessage(), e);
                    }
                });
            }
            
            // Log işlemleri - ayrı bir thread'e gönder
            if (logEnabled) {
                plugin.getThreadPool().submit(() -> {
                    try {
                        String logMessage = player.getName() + " tarafından gönderilen mesajda uygunsuz içerik tespit edildi: " + message;
                        
                        if (logConsole) {
                            plugin.getLogger().info(logMessage);
                        }
                        
                        if (logFile) {
                            logToFile(player.getName(), message, details.getWord(), details.getCategory(), 
                                    details.getSeverityLevel(), isAiDetected);
                        }
                        
                        // Yöneticilere bildirim gönder
                        final String adminMessage = plugin.getConfig().getString("messages.prefix") + 
                                plugin.getConfig().getString("messages.admin-alert", "&c%player% &fmuhtemel küfür kullandı: &7%message%")
                                        .replace("%player%", player.getName())
                                        .replace("%message%", message);
                        
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            for (Player admin : Bukkit.getOnlinePlayers()) {
                                if (admin.hasPermission("turkishprofanitydetection.admin")) {
                                    MessageUtils.sendMessage(admin, adminMessage);
                                }
                            }
                        });
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Log işlemleri sırasında hata: " + e.getMessage(), e);
                    }
                });
            }
            
            // Komut işlemleri ana thread'de çalışmalı
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    // Küfür seviyesine göre komutu çalıştır
                    if (actionRecommendation != null && !actionRecommendation.isEmpty()) {
                        // Yeni API tarafından önerilen aksiyona göre komut çalıştır
                        executeActionRecommendation(player, message, actionRecommendation);
                    } else if (severityActionsEnabled) {
                        // Eski sistem - şiddet seviyesine göre komut çalıştır
                        int severity = details.getSeverityLevel();
                        List<String> levelCommands = severityCommands.get(severity);
                        
                        if (levelCommands != null && !levelCommands.isEmpty()) {
                            for (String cmd : levelCommands) {
                                String finalCmd = cmd.replace("%player%", player.getName()).replace("%message%", message);
                                executeCommand(finalCmd);
                            }
                            return; // Seviye komutları çalıştırıldıysa genel komutları çalıştırma
                        }
                    }
                    
                    // Genel komutları çalıştır
                    if (commandsEnabled && !commands.isEmpty()) {
                        for (String cmd : commands) {
                            String finalCmd = cmd.replace("%player%", player.getName()).replace("%message%", message);
                            executeCommand(finalCmd);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Komut çalıştırılırken hata: " + e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Küfür tespiti sırasında hata: " + e.getMessage(), e);
        }
    }
    
    /**
     * Küfür tespitini dosyaya kaydeder.
     * 
     * @param playerName Oyuncu adı
     * @param message Gönderilen mesaj
     * @param word Tespit edilen kelime
     * @param category Kategori
     * @param severity Şiddet seviyesi
     * @param aiDetected AI tarafından tespit edildi mi
     */
    private void logToFile(String playerName, String message, String word, String category, int severity, boolean aiDetected) {
        if (logFilePath == null || logFilePath.isEmpty()) {
            return;
        }
        
        // Log klasörünü oluştur
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        // Log dosyası yolunu hazırla
        File logFile = new File(dataFolder, logFilePath);
        File parentDir = logFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        // Dosyaya yaz
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = dateFormat.format(new Date());
            
            writer.println(String.format("[%s] %s: '%s' (%s) - Level: %d, AI: %b, Message: \"%s\"",
                    timestamp, playerName, word, category, severity, aiDetected, message));
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Küfür log dosyasına yazılırken hata: " + e.getMessage(), e);
        }
    }
    
    /**
     * API'nin önerdiği aksiyonu uygular. Bu yapay zeka tarafından belirlenen
     * en uygun aksiyon türünü Minecraft komutlarına dönüştürür.
     *
     * @param player Oyuncu
     * @param message Mesaj
     * @param actionType Aksiyon türü (warn, mute, kick, ban)
     */
    private void executeActionRecommendation(Player player, String message, String actionType) {
        String playerName = player.getName();
        
        switch (actionType.toLowerCase()) {
            case "warn":
                // Oyuncuya özel bir uyarı mesajı gönder
                String warnMessage = plugin.getConfig().getString("action-recommendations.warn", 
                        "warn %player% Uygunsuz içerik tespit edildi. Lütfen dikkat ediniz.");
                executeCommand(warnMessage.replace("%player%", playerName));
                break;
                
            case "mute":
                // Oyuncuyu geçici olarak sustur (varsayılan 5 dakika)
                String muteTime = plugin.getConfig().getString("action-recommendations.mute-time", "5m");
                String muteMessage = plugin.getConfig().getString("action-recommendations.mute", 
                        "mute %player% %time% Uygunsuz içerik");
                executeCommand(muteMessage.replace("%player%", playerName).replace("%time%", muteTime));
                break;
                
            case "kick":
                // Oyuncuyu sunucudan at
                String kickMessage = plugin.getConfig().getString("action-recommendations.kick", 
                        "kick %player% Uygunsuz içerik nedeniyle sunucudan atıldınız.");
                executeCommand(kickMessage.replace("%player%", playerName));
                break;
                
            case "ban":
                // Oyuncuyu yasakla (varsayılan 1 gün)
                String banTime = plugin.getConfig().getString("action-recommendations.ban-time", "1d");
                String banMessage = plugin.getConfig().getString("action-recommendations.ban", 
                        "tempban %player% %time% Uygunsuz içerik nedeniyle geçici olarak yasaklandınız.");
                executeCommand(banMessage.replace("%player%", playerName).replace("%time%", banTime));
                break;
                
            default:
                // Tanınmayan aksiyon türü için varsayılan olarak genel komutları çalıştır
                if (commandsEnabled && !commands.isEmpty()) {
                    for (String cmd : commands) {
                        String finalCmd = cmd.replace("%player%", playerName).replace("%message%", message);
                        executeCommand(finalCmd);
                    }
                }
                break;
        }
    }
    
    /**
     * Bir komutu ana thread üzerinde çalıştırır.
     *
     * @param command Çalıştırılacak komut
     */
    private void executeCommand(String command) {
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
    }
    
    /**
     * Bir oyuncunun mesaj hızı sınırını aşıp aşmadığını kontrol eder.
     *
     * @param player Kontrol edilecek oyuncu
     * @return Oyuncu hız sınırını aştıysa true
     */
    private boolean isRateLimited(Player player) {
        if (!rateLimitEnabled || maxMessages <= 0 || timeWindow <= 0) {
            return false;
        }
        
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Oyuncunun sayacını al veya oluştur
        MessageCounter counter = playerMessageCounts.computeIfAbsent(playerId, 
                k -> new MessageCounter(currentTime));
        
        // Zaman penceresi dışındaysa sayacı sıfırla
        if (currentTime - counter.startTime > TimeUnit.SECONDS.toMillis(timeWindow)) {
            counter.count = 1;
            counter.startTime = currentTime;
            return false;
        }
        
        // Mesaj sayısını artır ve limiti kontrol et
        counter.count++;
        return counter.count > maxMessages;
    }
    
    /**
     * Oyuncu mesaj sayısını takip etmek için yardımcı sınıf.
     */
    private static class MessageCounter {
        private int count;
        private long startTime;
        
        public MessageCounter(long startTime) {
            this.count = 1;
            this.startTime = startTime;
        }
    }
}
