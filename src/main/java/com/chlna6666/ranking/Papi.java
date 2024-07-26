package com.chlna6666.ranking;

import com.chlna6666.ranking.I18n.I18n;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class Papi extends PlaceholderExpansion {
    private final DataManager dataManager;
    private final I18n i18n;

    public Papi(Ranking pluginInstance, DataManager dataManager, I18n i18n) {
        this.dataManager = dataManager;
        this.i18n = i18n;
    }

    @Override
    public String getAuthor() {
        return "Chlna6666";
    }

    @Override
    public String getIdentifier() {
        return "ranking";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        String leaderboardType = params.split("_")[0].toLowerCase();
        if (!LeaderboardSettings.getInstance().isLeaderboardEnabled(leaderboardType)) {
            return ""; // 如果功能被禁用，返回空字符串
        }

        switch (params.toLowerCase()) {
            case "place":
                return getPlayerCount(player, dataManager.getPlaceData());
            case "destroys":
                return getPlayerCount(player, dataManager.getDestroysData());
            case "deads":
                return getPlayerCount(player, dataManager.getDeadsData());
            case "mobdie":
                return getPlayerCount(player, dataManager.getMobdieData());
            case "onlinetime":
                return getPlayerCount(player, dataManager.getOnlinetimeData());
            case "break_bedrock":
                return getPlayerCount(player, dataManager.getBreakBedrockData());
            default:
                if (params.startsWith("place_")) {
                    return getRankingEntryAsync(player, params, dataManager.getPlaceData());
                } else if (params.startsWith("destroys_")) {
                    return getRankingEntryAsync(player, params, dataManager.getDestroysData());
                } else if (params.startsWith("deads_")) {
                    return getRankingEntryAsync(player, params, dataManager.getDeadsData());
                } else if (params.startsWith("mobdie_")) {
                    return getRankingEntryAsync(player, params, dataManager.getMobdieData());
                } else if (params.startsWith("onlinetime_")) {
                    return getRankingEntryAsync(player, params, dataManager.getOnlinetimeData());
                } else if (params.startsWith("break_bedrock_")) {
                    return getRankingEntryAsync(player, params, dataManager.getBreakBedrockData());
                }
                return null;
        }
    }

    private String getPlayerCount(OfflinePlayer player, JSONObject jsonData) {
        if (jsonData != null && player != null) {
            String playerUUID = player.getUniqueId().toString();
            Object playerCount = jsonData.get(playerUUID);
            if (playerCount != null) {
                return String.valueOf(playerCount);
            }
        }
        return null;
    }

    private String getRankingEntryAsync(OfflinePlayer player, String params, JSONObject jsonData) {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            return getRankingEntry(player, params, jsonData);
        });

        try {
            return future.get(); // 等待异步任务完成并返回结果
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getRankingEntry(OfflinePlayer player, String params, JSONObject jsonData) {
        if (jsonData != null) {
            List<Map.Entry<String, Object>> sortedEntries = new ArrayList<>(jsonData.entrySet());
            sortedEntries.sort((a, b) -> {
                int countA = Integer.parseInt(a.getValue().toString());
                int countB = Integer.parseInt(b.getValue().toString());
                return Integer.compare(countB, countA); // 降序排列
            });

            int totalPlayers = sortedEntries.size();
            OfflinePlayer currentPlayer = Bukkit.getOfflinePlayer(player.getUniqueId());

            for (int i = 1; i <= totalPlayers; i++) {
                Map.Entry<String, Object> entry = sortedEntries.get(i - 1);
                String uuidKey = entry.getKey();
                Object count = entry.getValue();

                if (params.equalsIgnoreCase(params.split("_")[0] + "_" + i)) {
                    OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuidKey));
                    if (targetPlayer != null) {
                        String playerName = targetPlayer.getName();
                        String countStr = String.valueOf(count);
                        if (targetPlayer.equals(currentPlayer)) {
                            countStr += " " + i18n.translate("papi.me");
                        }
                        return playerName + ": " + countStr;
                    }
                }
            }
            return "";
        }
        return null;
    }
}
