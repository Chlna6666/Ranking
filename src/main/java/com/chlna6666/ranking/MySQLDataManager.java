package com.chlna6666.ranking;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.sql.*;
import java.util.logging.Level;

public class MySQLDataManager extends DataManager {
    // 使用 HikariCP 连接池
    private HikariDataSource dataSource;
    private static final String TABLE_NAME = "ranking_data";

    public MySQLDataManager(Ranking plugin) {
        super(plugin);
        initDataSource();
        initializeDatabase();
        loadAllData();
    }

    // 初始化 HikariCP 连接池
    private void initDataSource() {
        HikariConfig config = new HikariConfig();
        String host = plugin.getConfig().getString("data_storage.mysql.host");
        int port = plugin.getConfig().getInt("data_storage.mysql.port");
        String database = plugin.getConfig().getString("data_storage.mysql.database");
        String username = plugin.getConfig().getString("data_storage.mysql.username");
        String password = plugin.getConfig().getString("data_storage.mysql.password");
        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&serverTimezone=UTC";

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        // 连接池参数，根据实际情况进行调整
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        dataSource = new HikariDataSource(config);
        plugin.getLogger().info("HikariCP 数据库连接池初始化成功");
    }

    // 获取数据库连接
    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    // 初始化数据表（如果不存在则创建）
    private void initializeDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                "id VARCHAR(50) PRIMARY KEY," +
                "jsonData LONGTEXT NOT NULL" +
                ")";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "创建数据表失败", e);
        }
    }

    // 根据 key 加载 JSON 数据
    private JSONObject loadJSON(String id) {
        String sql = "SELECT jsonData FROM " + TABLE_NAME + " WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String jsonStr = rs.getString("jsonData");
                    JSONParser parser = new JSONParser();
                    Object parsed = parser.parse(jsonStr);
                    if (parsed instanceof JSONObject) {
                        return (JSONObject) parsed;
                    }
                }
            }
        } catch (SQLException | ParseException e) {
            plugin.getLogger().log(Level.SEVERE, "加载 JSON 数据失败，key=" + id, e);
        }
        // 返回空 JSON 对象
        return new JSONObject();
    }

    // 保存 JSON 数据到数据库：若存在则更新，否则插入新记录
    private void saveJSON(String id, JSONObject json) {
        String selectSql = "SELECT id FROM " + TABLE_NAME + " WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
            selectStmt.setString(1, id);
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    // 更新操作
                    String updateSql = "UPDATE " + TABLE_NAME + " SET jsonData = ? WHERE id = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setString(1, json.toJSONString());
                        updateStmt.setString(2, id);
                        updateStmt.executeUpdate();
                    }
                } else {
                    // 插入操作
                    String insertSql = "INSERT INTO " + TABLE_NAME + " (id, jsonData) VALUES (?, ?)";
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                        insertStmt.setString(1, id);
                        insertStmt.setString(2, json.toJSONString());
                        insertStmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "保存 JSON 数据失败，key=" + id, e);
        }
    }

    // 从数据库加载所有数据，并通过 setter 替换父类中的数据对象
    private void loadAllData() {
        JSONObject playersDataFromDB = loadJSON("data");
        JSONObject placeDataFromDB = loadJSON("place");
        JSONObject destroysDataFromDB = loadJSON("destroys");
        JSONObject deadsDataFromDB = loadJSON("deads");
        JSONObject mobdieDataFromDB = loadJSON("mobdie");
        JSONObject onlinetimeDataFromDB = loadJSON("onlinetime");
        JSONObject breakBedrockDataFromDB = loadJSON("breakBedrock");

        setPlayersData(playersDataFromDB);
        setPlaceData(placeDataFromDB);
        setDestroysData(destroysDataFromDB);
        setDeadsData(deadsDataFromDB);
        setMobdieData(mobdieDataFromDB);
        setOnlinetimeData(onlinetimeDataFromDB);
        setBreakBedrockData(breakBedrockDataFromDB);
    }

    // setter 方法
    public void setPlayersData(JSONObject playersData) {
        this.playersData = playersData;
    }

    public void setPlaceData(JSONObject placeData) {
        this.placeData = placeData;
    }

    public void setDestroysData(JSONObject destroysData) {
        this.destroysData = destroysData;
    }

    public void setDeadsData(JSONObject deadsData) {
        this.deadsData = deadsData;
    }

    public void setMobdieData(JSONObject mobdieData) {
        this.mobdieData = mobdieData;
    }

    public void setOnlinetimeData(JSONObject onlinetimeData) {
        this.onlinetimeData = onlinetimeData;
    }

    public void setBreakBedrockData(JSONObject breakBedrockData) {
        this.breakBedrockData = breakBedrockData;
    }

    @Override
    public void saveAllData() {
        saveJSON("data", getPlayersData());
        saveJSON("place", getPlaceData());
        saveJSON("destroys", getDestroysData());
        saveJSON("deads", getDeadsData());
        saveJSON("mobdie", getMobdieData());
        saveJSON("onlinetime", getOnlinetimeData());
        saveJSON("breakBedrock", getBreakBedrockData());
    }

   //重构 DataManager
    @Override
    public void saveJSON(JSONObject json, File file) {
        String key = file.getName().replace(".json", "");
        saveJSON(key, json);
    }

    @Override
    public void saveJSONAsync(JSONObject json, File file) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String key = file.getName().replace(".json", "");
            saveJSON(key, json);
        });
    }
}
