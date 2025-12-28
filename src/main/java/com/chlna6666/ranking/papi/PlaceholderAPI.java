package com.chlna6666.ranking.papi;

import com.chlna6666.ranking.datamanager.DataManager;
import com.chlna6666.ranking.I18n.I18n;
import com.chlna6666.ranking.enums.LeaderboardType;
import com.chlna6666.ranking.leaderboard.LeaderboardSettings;
import com.chlna6666.ranking.Ranking;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class PlaceholderAPI extends PlaceholderExpansion {
    private final Ranking plugin;
    private final DataManager dataManager;
    private final I18n i18n;

    public PlaceholderAPI(Ranking plugin, DataManager dataManager, I18n i18n) {
        this.dataManager = dataManager;
        this.i18n = i18n;
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getAuthor() { return "Chlna6666"; }
    @Override
    public @NotNull String getIdentifier() { return "ranking"; }
    @Override
    public @NotNull String getVersion() { return "1.0.0"; }
    @Override
    public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        String[] parts = params.split("_");
        String typeStr = parts[0].toLowerCase(); // e.g. "place" from "place_1"

        LeaderboardType type = LeaderboardType.fromString(typeStr);
        if (type == null || !LeaderboardSettings.getInstance().isLeaderboardEnabled(typeStr)) {
            return "";
        }

        // 1. 获取自己数据: %ranking_place%
        if (parts.length == 1) {
            return getPlayerCount(player, dataManager.getData(type));
        }

        // 2. 获取排行数据: %ranking_place_1%
        else if (parts.length == 2) {
            return getRankingEntryAsync(player, params, dataManager.getData(type));
        }

        return null;
    }

    private String getPlayerCount(OfflinePlayer player, JSONObject jsonData) {
        if (jsonData != null && player != null) {
            Object count = jsonData.get(player.getUniqueId().toString());
            return count != null ? String.valueOf(count) : "0";
        }
        return "0";
    }

    private String getRankingEntryAsync(OfflinePlayer player, String params, JSONObject jsonData) {
        try {
            return CompletableFuture.supplyAsync(() -> getRankingEntry(player, params, jsonData)).get();
        } catch (Exception e) {
            return "Error";
        }
    }

    private String getRankingEntry(OfflinePlayer player, String params, JSONObject jsonData) {
        if (jsonData == null) return "";

        // 这里可以优化：避免每次请求都排序
        List<Map.Entry<String, Object>> sorted = new ArrayList<>(jsonData.entrySet());
        sorted.sort((a, b) -> {
            long cA = Long.parseLong(a.getValue().toString());
            long cB = Long.parseLong(b.getValue().toString());
            return Long.compare(cB, cA);
        });

        try {
            int rank = Integer.parseInt(params.split("_")[1]);
            if (rank > sorted.size() || rank <= 0) return "---";

            Map.Entry<String, Object> entry = sorted.get(rank - 1);
            OfflinePlayer target = Bukkit.getOfflinePlayer(UUID.fromString(entry.getKey()));
            String name = target.getName() != null ? target.getName() : "Unknown";
            String suffix = target.getUniqueId().equals(player.getUniqueId()) ? " " + i18n.translate("papi.me") : "";

            return name + ": " + entry.getValue() + suffix;
        } catch (Exception e) {
            return "";
        }
    }
}