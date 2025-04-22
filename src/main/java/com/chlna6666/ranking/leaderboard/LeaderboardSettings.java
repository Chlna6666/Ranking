package com.chlna6666.ranking.leaderboard;

import com.chlna6666.ranking.config.ConfigManager;

import java.util.HashMap;
import java.util.Map;


public class LeaderboardSettings {

    private static LeaderboardSettings instance;
    private final Map<String, Boolean> settingsCache = new HashMap<>();

    private LeaderboardSettings() {}

    public static LeaderboardSettings getInstance() {
        if (instance == null) {
            instance = new LeaderboardSettings();
        }
        return instance;
    }

    public void loadSettings(ConfigManager configManager) {
        settingsCache.put("place", configManager.isLeaderboardEnabled("place"));
        settingsCache.put("destroys", configManager.isLeaderboardEnabled("destroys"));
        settingsCache.put("deads", configManager.isLeaderboardEnabled("deads"));
        settingsCache.put("mobdie", configManager.isLeaderboardEnabled("mobdie"));
        settingsCache.put("onlinetime", configManager.isLeaderboardEnabled("onlinetime"));
        settingsCache.put("break_bedrock", configManager.isLeaderboardEnabled("break_bedrock"));
    }

    public boolean isLeaderboardEnabled(String leaderboard) {
        return settingsCache.getOrDefault(leaderboard, false);
    }
    }


