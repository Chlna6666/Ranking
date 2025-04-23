package com.chlna6666.ranking.datamanager;

import com.chlna6666.ranking.I18n.I18n;
import com.chlna6666.ranking.Ranking;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.sql.*;
import java.util.Map;
import java.util.concurrent.*;

public class MySQLDataManager extends DataManager {
    private final I18n i18n ;
    private static final String TABLE_NAME = "ranking_data";
    private final HikariDataSource dataSource;
    private final ExecutorService asyncExecutor;
    private final Map<String, JSONObject> dataCache = new ConcurrentHashMap<>();
    private final JSONParser jsonParser = new JSONParser();

    public MySQLDataManager(Ranking plugin) {
        super(plugin);
        this.i18n = plugin.getI18n();
        plugin.getLogger().info(i18n.translate("data.use_mysql"));
        validateConfig();
        this.dataSource = createDataSource();
        this.asyncExecutor = Executors.newWorkStealingPool(8);
        initializeDatabase();
        loadAllData();
    }

    // region 初始化相关
    private void validateConfig() {
        String[] requiredKeys = {"host", "port", "database", "username"};
        for (String key : requiredKeys) {
            if (plugin.getConfig().getString("data_storage.mysql." + key) == null) {
                throw new IllegalArgumentException(i18n.translate("data.mysql.config_missing") + key);
            }
        }
    }

    /**
     * 创建并配置 HikariCP 数据库连接池
     * @return HikariDataSource 连接池对象
     */
    private HikariDataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(buildJdbcUrl());

        config.setUsername(plugin.getConfig().getString("data_storage.mysql.username"));
        config.setPassword(plugin.getConfig().getString("data_storage.mysql.password"));

        config.setMaximumPoolSize(20); // 连接池最大连接数（建议根据服务器负载调整）
        config.setMinimumIdle(5); // 连接池最小空闲连接数，保证可用性
        config.setConnectionTimeout(3000); // 获取连接的超时时间（毫秒），超时后抛出异常
        config.setIdleTimeout(60000); // 空闲连接的超时时间（毫秒），超时后关闭空闲连接
        config.setMaxLifetime(180000); // 连接的最大生命周期（毫秒），防止长期使用导致性能下降

        config.addDataSourceProperty("cachePrepStmts", "true"); // 启用预编译 SQL 语句缓存
        config.addDataSourceProperty("prepStmtCacheSize", "250"); // 预编译 SQL 语句缓存的最大数量
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048"); // 预编译 SQL 语句缓存的最大长度

        // 创建并返回 HikariCP 数据源（连接池）
        return new HikariDataSource(config);
    }


    private String buildJdbcUrl() {
        return String.format("jdbc:mysql://%s:%d/%s?useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true",
                plugin.getConfig().getString("data_storage.mysql.host"),
                plugin.getConfig().getInt("data_storage.mysql.port"),
                plugin.getConfig().getString("data_storage.mysql.database"));
    }

    private void initializeDatabase() {
        executeUpdate();
    }


    @Override
    public void saveData(String dataType, JSONObject data) {
        asyncExecutor.submit(() -> {
            try {
                executeUpsert(dataType, data);
                dataCache.put(dataType, data); // 更新缓存
            } catch (SQLException e) {
                handleSaveError(dataType, data, e);
            }
        });
    }

    @Override
    public void saveAllData() {
        dataCache.forEach(this::saveData);
    }

    private void executeUpsert(String id, JSONObject json) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO " + TABLE_NAME + " (id, jsonData) VALUES (?, ?) " +
                             "ON DUPLICATE KEY UPDATE jsonData = VALUES(jsonData)")) {

            stmt.setString(1, id);
            stmt.setString(2, json.toJSONString());
            stmt.executeUpdate();
        }
    }

    private void loadAllData() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, jsonData FROM " + TABLE_NAME)) {

            while (rs.next()) {
                String id = rs.getString("id");
                JSONObject data = parseJson(rs.getString("jsonData"));
                dataCache.put(id, data);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe(i18n.translate("data.mysql.load_failed") + e.getMessage());
        }
    }

    private JSONObject parseJson(String jsonStr) {
        try {
            return (JSONObject) jsonParser.parse(jsonStr);
        } catch (ParseException e) {
            plugin.getLogger().warning(i18n.translate("data.mysql.parse_failed") + e.getMessage());
            return new JSONObject();
        }
    }


    // region 错误处理
    private void handleSaveError(String dataType, JSONObject data, SQLException e) {
        plugin.getLogger().warning(i18n.translate("data.mysql.save_failed", dataType));

        // 带指数退避的重试机制
        int maxRetries = 3;
        for (int i = 1; i <= maxRetries; i++) {
            try {
                Thread.sleep(1000L * i); // 退避等待
                executeUpsert(dataType, data);
                return;
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            } catch (SQLException ex) {
                if (i == maxRetries) {
                    plugin.getLogger().severe(i18n.translate("data.mysql.save_final_failed") + ex.getMessage());
                    // TODO: 添加失败数据暂存机制
                }
            }
        }
    }


    // region 连接管理
    private void executeUpdate() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME +
                    " (id VARCHAR(50) PRIMARY KEY, jsonData LONGTEXT NOT NULL, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP) " +
                    "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            plugin.getLogger().severe(i18n.translate("data.mysql.create_table_error") + e.getMessage());
        }
    }


    @Override
    public void shutdownDataManager() {
        asyncExecutor.shutdown();
        try {
            if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    // endregion

    // region 数据访问方法
    @Override
    public JSONObject getPlayersData() {
        return dataCache.getOrDefault("data", new JSONObject());
    }

    @Override
    public JSONObject getPlaceData() {
        return dataCache.getOrDefault("place", new JSONObject());
    }

    @Override
    public JSONObject getDestroysData() {
        return dataCache.getOrDefault("destroys", new JSONObject());
    }

    @Override
    public JSONObject getDeadsData() {
        return dataCache.getOrDefault("deads", new JSONObject());
    }

    @Override
    public JSONObject getMobdieData() {
        return dataCache.getOrDefault("mobdie", new JSONObject());
    }

    @Override
    public JSONObject getOnlinetimeData() {
        return dataCache.getOrDefault("onlinetime", new JSONObject());
    }

    @Override
    public JSONObject getBreakBedrockData() {
        return dataCache.getOrDefault("break_bedrock", new JSONObject());
    }
    // endregion
    @Override
    protected void loadFiles() {
        loadAllData();
    }
}