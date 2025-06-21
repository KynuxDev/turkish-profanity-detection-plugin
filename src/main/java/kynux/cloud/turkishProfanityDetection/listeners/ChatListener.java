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
    
    private final boolean rateLimitEnabled;
    private final int maxMessages;
    private final int timeWindow;
    private final String rateLimitAction;
    private final Map<UUID, MessageCounter> playerMessageCounts = new HashMap<>();
    
    public ChatListener(@NotNull TurkishProfanityDetection plugin) {
        this.plugin = plugin;
        
        this.cancelMessage = plugin.getConfig().getBoolean("actions.cancel-message", true);
        this.logEnabled = plugin.getConfig().getBoolean("actions.log.enabled", true);
        this.logConsole = plugin.getConfig().getBoolean("actions.log.console", true);
        this.logFile = plugin.getConfig().getBoolean("actions.log.file", true);
        this.logFilePath = plugin.getConfig().getString("actions.log.file-path", "logs/profanity.log");
        this.commandsEnabled = plugin.getConfig().getBoolean("actions.commands.enabled", true);
        this.commands = plugin.getConfig().getStringList("actions.commands.list");
        this.bypassPermission = plugin.getConfig().getString("permissions.bypass", "turkishprofanitydetection.bypass");
        this.statisticsEnabled = plugin.getConfig().getBoolean("statistics.enabled", true);
        
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
        
        this.rateLimitEnabled = plugin.getConfig().getBoolean("security.rate-limit.enabled", true);
        this.maxMessages = plugin.getConfig().getInt("security.rate-limit.max-messages", 5);
        this.timeWindow = plugin.getConfig().getInt("security.rate-limit.time-window", 10);
        this.rateLimitAction = plugin.getConfig().getString("security.rate-limit.action", "mute %player% 5m Spam");
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled() || event.getMessage().trim().isEmpty()) {
            return;
        }
        
        Player player = event.getPlayer();
        String message = event.getMessage();
        
        if (player.hasPermission(bypassPermission)) {
            return;
        }
        
        if (rateLimitEnabled && isRateLimited(player)) {
            event.setCancelled(true);
            
            if (rateLimitAction != null && !rateLimitAction.isEmpty()) {
                executeCommand(rateLimitAction.replace("%player%", player.getName()));
            }
            
            MessageUtils.sendMessage(player, plugin.getConfig().getString("messages.prefix") + 
                    "Çok hızlı mesaj gönderiyorsunuz. Lütfen yavaşlayın.");
            return;
        }
        
        plugin.getProfanityApiService().checkText(message).thenAccept(response -> {
            try {
                if (!response.isSuccess() || response.getResult() == null || !response.getResult().isSwear()) {
                    return;
                }
                
                ProfanityResponse.Details details = response.getResult().getDetails();
                
                if (statisticsEnabled && details != null) {
                    ProfanityRecord record = new ProfanityRecord(
                            player.getUniqueId(),
                            player.getName(),
                            details.getWord(),
                            details.getCategory(),
                            details.getSeverityLevel(),
                            details.getDetectedWords(),
                            message,
                            response.getResult().isAiDetected()
                    );
                    
                    plugin.getProfanityStorage().addRecord(record);
                    
                    plugin.getDiscordWebhook().sendProfanityAlert(record);
                    
                    plugin.getPlaceholderAPIHook().updatePlayerProfanityCount(player.getUniqueId());
                }
                
                if (logEnabled) {
                    String logMessage = player.getName() + " tarafından gönderilen mesajda uygunsuz içerik tespit edildi: " + message;
                    
                    if (logConsole) {
                        plugin.getLogger().info(logMessage);
                    }
                    
                    if (logFile && details != null) {
                        logToFile(player.getName(), message, details.getWord(), details.getCategory(), 
                                details.getSeverityLevel(), response.getResult().isAiDetected());
                    }
                    
                    String adminMessage = plugin.getConfig().getString("messages.prefix") + 
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
                }
                
                if (severityActionsEnabled && details != null) {
                    int severity = details.getSeverityLevel();
                    List<String> levelCommands = severityCommands.get(severity);
                    
                    if (levelCommands != null && !levelCommands.isEmpty()) {
                        for (String cmd : levelCommands) {
                            String finalCmd = cmd.replace("%player%", player.getName()).replace("%message%", message);
                            executeCommand(finalCmd);
                        }
                        return;
                    }
                }
                
                if (commandsEnabled && !commands.isEmpty()) {
                    for (String cmd : commands) {
                        String finalCmd = cmd.replace("%player%", player.getName()).replace("%message%", message);
                        executeCommand(finalCmd);
                    }
                }
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Küfür tespiti sırasında hata: " + e.getMessage(), e);
            }
        });
    }
    
    private void logToFile(String playerName, String message, String word, String category, int severity, boolean aiDetected) {
        if (logFilePath == null || logFilePath.isEmpty()) {
            return;
        }
        
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        File logFile = new File(dataFolder, logFilePath);
        File parentDir = logFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = dateFormat.format(new Date());
            
            writer.println(String.format("[%s] %s: '%s' (%s) - Level: %d, AI: %b, Message: \"%s\"",
                    timestamp, playerName, word, category, severity, aiDetected, message));
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Küfür log dosyasına yazılırken hata: " + e.getMessage(), e);
        }
    }
 
    private void executeCommand(String command) {
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
    }

    private boolean isRateLimited(Player player) {
        if (!rateLimitEnabled || maxMessages <= 0 || timeWindow <= 0) {
            return false;
        }
        
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        MessageCounter counter = playerMessageCounts.computeIfAbsent(playerId, 
                k -> new MessageCounter(currentTime));
        
        if (currentTime - counter.startTime > TimeUnit.SECONDS.toMillis(timeWindow)) {
            counter.count = 1;
            counter.startTime = currentTime;
            return false;
        }
        
        counter.count++;
        return counter.count > maxMessages;
    }

    private static class MessageCounter {
        private int count;
        private long startTime;
        
        public MessageCounter(long startTime) {
            this.count = 1;
            this.startTime = startTime;
        }
    }
}
