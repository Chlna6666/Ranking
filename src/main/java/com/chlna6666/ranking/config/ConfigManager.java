package com.chlna6666.ranking.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.logging.Level;

public class ConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        // 加载默认配置文件
        FileConfiguration defaultConfig = loadDefaultConfig();

        // 加载插件数据文件夹中的配置文件
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        config = YamlConfiguration.loadConfiguration(configFile);

        // 将默认配置文件和现有配置文件进行比对和合并
        mergeConfigs(defaultConfig, config);

        // 保存更新后的配置文件
        saveConfig();
    }

    private FileConfiguration loadDefaultConfig() {
        FileConfiguration defaultConfig = null;
        try (InputStream defaultConfigStream = plugin.getResource("config.yml")) {
            if (defaultConfigStream != null) {
                defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfigStream));
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load default config from 'config.yml'", e);
        }
        return defaultConfig;
    }
    private void mergeConfigs(FileConfiguration defaultConfig, FileConfiguration currentConfig) {
        if (defaultConfig == null) return;

        Set<String> keys = defaultConfig.getKeys(true);
        for (String key : keys) {
            if (!currentConfig.contains(key)) {
                currentConfig.set(key, defaultConfig.get(key));
            }
        }
    }

    private void saveConfig() {
        try {
            config.save(new File(plugin.getDataFolder(), "config.yml"));
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save config to 'config.yml'", e);
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    // 获取排行榜开关数据
    public boolean isLeaderboardEnabled(String leaderboard) {
        return config.getBoolean("leaderboards." + leaderboard, false);
    }
}
