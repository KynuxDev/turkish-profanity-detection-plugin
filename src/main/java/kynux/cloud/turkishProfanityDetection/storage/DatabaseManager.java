package kynux.cloud.turkishProfanityDetection.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import kynux.cloud.turkishProfanityDetection.model.ProfanityRecord;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseManager {
    private final JavaPlugin plugin;
    private final Logger logger;
    private final String storageType;
    private final String prefix;
    private HikariDataSource dataSource;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // SQL sorgular
    private final String CREATE_PLAYERS_TABLE;
    private final String CREATE_RECORDS_TABLE;
    private final String INSERT_PLAYER;
    private final String GET_PLAYER_BY_UUID;
    private final String INSERT_RECORD;
    private final String GET_PLAYER_RECORDS;
    private final String GET_ALL_RECORDS;
    private final String DELETE_PLAYER_RECORDS;
    private final String DELETE_ALL_RECORDS;
    private final String DELETE_OLD_RECORDS;
    
    public DatabaseManager(JavaPlugin plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.storageType = config.getString("statistics.storage-type", "file").toLowerCase();
        this.prefix = config.getString("statistics.mysql.table-prefix", "tpd_");
        
        CREATE_PLAYERS_TABLE = "CREATE TABLE IF NOT EXISTS " + prefix + "players (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "uuid VARCHAR(36) UNIQUE NOT NULL, " +
                "player_name VARCHAR(36) NOT NULL, " +
                "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ");";
        
        CREATE_RECORDS_TABLE = "CREATE TABLE IF NOT EXISTS " + prefix + "records (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "player_id INT NOT NULL, " +
                "word VARCHAR(255) NOT NULL, " +
                "category VARCHAR(100) NOT NULL, " +
                "severity_level INT NOT NULL, " +
                "original_message TEXT NOT NULL, " +
                "detected_words TEXT NOT NULL, " +
                "timestamp DATETIME NOT NULL, " +
                "ai_detected BOOLEAN NOT NULL, " +
                "FOREIGN KEY (player_id) REFERENCES " + prefix + "players(id) ON DELETE CASCADE" +
                ");";
        
        INSERT_PLAYER = "INSERT INTO " + prefix + "players (uuid, player_name) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE player_name = VALUES(player_name);";
        
        GET_PLAYER_BY_UUID = "SELECT id FROM " + prefix + "players WHERE uuid = ?;";
        
        INSERT_RECORD = "INSERT INTO " + prefix + "records " +
                "(player_id, word, category, severity_level, original_message, detected_words, timestamp, ai_detected) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
        
        GET_PLAYER_RECORDS = "SELECT r.*, p.uuid, p.player_name FROM " + prefix + "records r " +
                "JOIN " + prefix + "players p ON r.player_id = p.id " +
                "WHERE p.uuid = ? ORDER BY r.timestamp DESC;";
        
        GET_ALL_RECORDS = "SELECT r.*, p.uuid, p.player_name FROM " + prefix + "records r " +
                "JOIN " + prefix + "players p ON r.player_id = p.id " +
                "ORDER BY r.timestamp DESC;";
        
        DELETE_PLAYER_RECORDS = "DELETE FROM " + prefix + "records WHERE player_id = " +
                "(SELECT id FROM " + prefix + "players WHERE uuid = ?);";
        
        DELETE_ALL_RECORDS = "DELETE FROM " + prefix + "records;";
        
        DELETE_OLD_RECORDS = "DELETE FROM " + prefix + "records WHERE timestamp < ?;";
        
        if (storageType.equals("mysql")) {
            setupMySQLConnection(config);
        }
    }
    
    private void setupMySQLConnection(FileConfiguration config) {
        long startTime = System.currentTimeMillis();
        
        String host = config.getString("statistics.mysql.host", "localhost");
        int port = config.getInt("statistics.mysql.port", 3306);
        String database = config.getString("statistics.mysql.database", "minecraft");
        String username = config.getString("statistics.mysql.username", "root");
        String password = config.getString("statistics.mysql.password", "");
        
        try {
            int availableCores = Runtime.getRuntime().availableProcessors();
            int poolSize = Math.min(availableCores * 2, 20);
            
            HikariConfig hikariConfig = new HikariConfig();
            
            hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + 
                    "?useSSL=false&useUnicode=true&characterEncoding=utf8" +
                    "&rewriteBatchedStatements=true" +
                    "&useLocalSessionState=true" +
                    "&cacheServerConfiguration=true" +
                    "&cacheResultSetMetadata=true" +
                    "&elideSetAutoCommits=true" +
                    "&maintainTimeStats=false" +
                    "&useCompression=true");
            
            hikariConfig.setUsername(username);
            hikariConfig.setPassword(password);
            
            hikariConfig.setMaximumPoolSize(poolSize);
            hikariConfig.setMinimumIdle(Math.max(2, poolSize / 4));
            hikariConfig.setConnectionTimeout(10000);
            hikariConfig.setIdleTimeout(300000);
            hikariConfig.setMaxLifetime(900000);
            hikariConfig.setAutoCommit(true);
            hikariConfig.setKeepaliveTime(60000);
            
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "500");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "4096");
            hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
            hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
            hikariConfig.addDataSourceProperty("alwaysSendSetIsolation", "false");
            hikariConfig.addDataSourceProperty("cacheCallableStmts", "true");
            hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
            hikariConfig.addDataSourceProperty("metadataCacheSize", "1024");
            hikariConfig.addDataSourceProperty("tcpKeepAlive", "true");
            
            hikariConfig.setConnectionTestQuery("SELECT 1");
            hikariConfig.setLeakDetectionThreshold(60000);
            
            hikariConfig.setPoolName("TPD-MySQL-Pool");
            
            hikariConfig.setRegisterMbeans(true);
            
            dataSource = new HikariDataSource(hikariConfig);
            
            boolean tablesCreated = createTables();
            
            if (tablesCreated) {
                optimizeTables();
            }
            
            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info("MySQL veritabanına başarıyla bağlanıldı: " + host + ":" + port + "/" + database + 
                       " (" + elapsedTime + "ms, Havuz Boyutu: " + poolSize + ")");
            
        } catch (Exception e) {
            String errorMessage = (e != null) ? (e.getMessage() != null ? e.getMessage() : "Bilinmeyen hata") : "Null hata nesnesi";
            logger.severe("MySQL bağlantısı kurulamadı: " + errorMessage);
            if (e != null) {
                e.printStackTrace();
            }
        }
    }
    
    private boolean createTables() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute(CREATE_PLAYERS_TABLE);
            stmt.execute(CREATE_RECORDS_TABLE);
            logger.info("Veritabanı tabloları başarıyla oluşturuldu veya güncellendi");
            return true;
            
        } catch (SQLException e) {
            logger.severe("Tablolar oluşturulurken hata: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private void optimizeTables() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            try {
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_" + prefix + "players_uuid ON " + 
                           prefix + "players (uuid)");
                
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_" + prefix + "records_player_id ON " + 
                           prefix + "records (player_id)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_" + prefix + "records_timestamp ON " + 
                           prefix + "records (timestamp)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_" + prefix + "records_severity ON " + 
                           prefix + "records (severity_level)");
                
                logger.info("Veritabanı indeksleri başarıyla oluşturuldu");
            } catch (SQLException e) {
                logger.warning("İndeksler oluşturulurken hata: " + e.getMessage() + 
                             " - Bu veritabanı sürümünde indeks oluşturma sözdizimi desteklenmeyebilir");
            }
            
            try {
                stmt.execute("ANALYZE TABLE " + prefix + "players");
                stmt.execute("ANALYZE TABLE " + prefix + "records");
                logger.info("Tablo istatistikleri güncellendi");
            } catch (SQLException e) {
                logger.fine("Tablo istatistikleri güncellenemedi: " + e.getMessage());
            }
            
        } catch (SQLException e) {
            logger.warning("Veritabanı optimizasyonu yapılırken hata: " + e.getMessage());
        }
    }
    
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("MySQL bağlantısı kapatıldı.");
        }
    }
    
    private Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Veritabanı bağlantısı kullanılamıyor.");
        }
        return dataSource.getConnection();
    }
    
    private int ensurePlayerExists(UUID playerId, String playerName) {
        try (Connection conn = getConnection();
             PreparedStatement insertStmt = conn.prepareStatement(INSERT_PLAYER);
             PreparedStatement selectStmt = conn.prepareStatement(GET_PLAYER_BY_UUID)) {
            
            insertStmt.setString(1, playerId.toString());
            insertStmt.setString(2, playerName);
            insertStmt.executeUpdate();
            
            selectStmt.setString(1, playerId.toString());
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
            
            throw new SQLException("Oyuncu ID'si alınamadı");
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Oyuncu kaydedilirken hata: " + e.getMessage(), e);
            return -1;
        }
    }
    
    public boolean addRecord(@NotNull ProfanityRecord record) {
        if (!storageType.equals("mysql") || dataSource == null) {
            return false;
        }
        
        int playerId = ensurePlayerExists(record.getPlayerId(), record.getPlayerName());
        if (playerId == -1) {
            return false;
        }
        
        long startTime = System.currentTimeMillis();
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_RECORD)) {
            
            stmt.setInt(1, playerId);
            stmt.setString(2, record.getWord());
            stmt.setString(3, record.getCategory());
            stmt.setInt(4, record.getSeverityLevel());
            stmt.setString(5, record.getOriginalMessage());
            
            stmt.setString(6, String.join(",", record.getDetectedWords()));
            
            stmt.setString(7, record.getTimestamp().format(dateFormatter));
            stmt.setBoolean(8, record.isAiDetected());
            
            boolean result = stmt.executeUpdate() > 0;
            
            long queryTime = System.currentTimeMillis() - startTime;
            if (queryTime > 100) {
                logger.fine("Yavaş küfür kaydı ekleme sorgusu: " + queryTime + "ms");
            }
            
            return result;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Küfür kaydı eklenirken hata: " + e.getMessage(), e);
            return false;
        }
    }
    
    public int addRecordsBatch(List<ProfanityRecord> records) {
        if (!storageType.equals("mysql") || dataSource == null || records == null || records.isEmpty()) {
            return 0;
        }
        
        int addedCount = 0;
        
        try (Connection conn = getConnection();
             PreparedStatement playerStmt = conn.prepareStatement(INSERT_PLAYER, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement selectPlayerStmt = conn.prepareStatement(GET_PLAYER_BY_UUID);
             PreparedStatement recordStmt = conn.prepareStatement(INSERT_RECORD)) {
            
            conn.setAutoCommit(false);
            
            Map<UUID, Integer> playerIdCache = new HashMap<>();
            
            for (ProfanityRecord record : records) {
                int playerId;
                UUID playerUUID = record.getPlayerId();
                
                if (playerIdCache.containsKey(playerUUID)) {
                    playerId = playerIdCache.get(playerUUID);
                } else {
                    selectPlayerStmt.setString(1, playerUUID.toString());
                    try (ResultSet rs = selectPlayerStmt.executeQuery()) {
                        if (rs.next()) {
                            playerId = rs.getInt("id");
                        } else {
                            playerStmt.setString(1, playerUUID.toString());
                            playerStmt.setString(2, record.getPlayerName());
                            playerStmt.executeUpdate();
                            
                            try (ResultSet generatedKeys = playerStmt.getGeneratedKeys()) {
                                if (generatedKeys.next()) {
                                    playerId = generatedKeys.getInt(1);
                                } else {
                                    throw new SQLException("Oyuncu oluşturuldu fakat ID alınamadı");
                                }
                            }
                        }
                    }
                    
                    playerIdCache.put(playerUUID, playerId);
                }
                
                recordStmt.setInt(1, playerId);
                recordStmt.setString(2, record.getWord());
                recordStmt.setString(3, record.getCategory());
                recordStmt.setInt(4, record.getSeverityLevel());
                recordStmt.setString(5, record.getOriginalMessage());
                recordStmt.setString(6, String.join(",", record.getDetectedWords()));
                recordStmt.setString(7, record.getTimestamp().format(dateFormatter));
                recordStmt.setBoolean(8, record.isAiDetected());
                recordStmt.addBatch();
                
                if (++addedCount % 100 == 0) {
                    recordStmt.executeBatch();
                    recordStmt.clearBatch();
                }
            }
            
            int[] results = recordStmt.executeBatch();
            
            conn.commit();
            
            addedCount = 0;
            for (int result : results) {
                if (result > 0) {
                    addedCount += result;
                }
            }
            
            return addedCount;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Toplu küfür kaydı eklenirken hata: " + e.getMessage(), e);
            return 0;
        }
    }
    
    public List<ProfanityRecord> getPlayerRecords(UUID playerId) {
        if (!storageType.equals("mysql") || dataSource == null) {
            return new ArrayList<>();
        }
        
        List<ProfanityRecord> records = new ArrayList<>();
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_PLAYER_RECORDS)) {
            
            stmt.setString(1, playerId.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    records.add(extractRecordFromResultSet(rs));
                }
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Oyuncu kayıtları getirilirken hata: " + e.getMessage(), e);
        }
        
        return records;
    }
    
    public Map<UUID, List<ProfanityRecord>> getAllRecords() {
        if (!storageType.equals("mysql") || dataSource == null) {
            return new HashMap<>();
        }
        
        Map<UUID, List<ProfanityRecord>> records = new HashMap<>();
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_ALL_RECORDS);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                ProfanityRecord record = extractRecordFromResultSet(rs);
                UUID playerId = record.getPlayerId();
                
                records.computeIfAbsent(playerId, k -> new ArrayList<>()).add(record);
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Tüm kayıtlar getirilirken hata: " + e.getMessage(), e);
        }
        
        return records;
    }
    
    public boolean clearPlayerRecords(UUID playerId) {
        if (!storageType.equals("mysql") || dataSource == null) {
            return false;
        }
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_PLAYER_RECORDS)) {
            
            conn.setAutoCommit(false);
            
            stmt.setString(1, playerId.toString());
            int affectedRows = stmt.executeUpdate();
            
            conn.commit();
            
            if (affectedRows > 0) {
                logger.info(playerId + " ID'li oyuncunun " + affectedRows + " kaydı silindi");
            } else {
                logger.fine(playerId + " ID'li oyuncunun silinecek kaydı bulunamadı");
            }
            
            return affectedRows > 0;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Oyuncu kayıtları temizlenirken hata: " + e.getMessage(), e);
            return false;
        }
    }
    
    public boolean clearAllRecords() {
        if (!storageType.equals("mysql") || dataSource == null) {
            return false;
        }
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_ALL_RECORDS)) {
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Tüm kayıtlar temizlenirken hata: " + e.getMessage(), e);
            return false;
        }
    }
    
    public boolean cleanupOldData(int days) {
        if (!storageType.equals("mysql") || dataSource == null || days <= 0) {
            return false;
        }
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_OLD_RECORDS)) {
            
            stmt.setString(1, cutoffDate.format(dateFormatter));
            int affected = stmt.executeUpdate();
            
            if (affected > 0) {
                logger.info(days + " günden eski " + affected + " kayıt temizlendi.");
                return true;
            }
            
            return false;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Eski kayıtlar temizlenirken hata: " + e.getMessage(), e);
            return false;
        }
    }
    
    private ProfanityRecord extractRecordFromResultSet(ResultSet rs) throws SQLException {
        UUID playerId = UUID.fromString(rs.getString("uuid"));
        String playerName = rs.getString("player_name");
        String word = rs.getString("word");
        String category = rs.getString("category");
        int severityLevel = rs.getInt("severity_level");
        String originalMessage = rs.getString("original_message");
        String[] detectedWordsArray = rs.getString("detected_words").split(",");
        List<String> detectedWords = Arrays.asList(detectedWordsArray);
        LocalDateTime timestamp = LocalDateTime.parse(rs.getString("timestamp"), dateFormatter);
        boolean aiDetected = rs.getBoolean("ai_detected");
        
        return new ProfanityRecord(
                playerId, playerName, word, category, severityLevel,
                detectedWords, originalMessage, aiDetected, timestamp);
    }
}
