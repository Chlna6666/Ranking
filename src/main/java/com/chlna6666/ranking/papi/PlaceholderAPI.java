package com.chlna6666.ranking.papi;

import com.chlna6666.ranking.datamanager.DataManager;
import com.chlna6666.ranking.I18n.I18n;
import com.chlna6666.ranking.leaderboard.LeaderboardSettings;
import com.chlna6666.ranking.Ranking;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

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
    public @NotNull String getAuthor() {
        return "Chlna6666";
    }

    @Override
    public @NotNull String getIdentifier() {
        return "ranking";
    }

    @Override
    public @NotNull String getVersion() {
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

        return switch (params.toLowerCase()) {
            case "place" -> getPlayerCount(player, dataManager.getPlaceData());
            case "destroys" -> getPlayerCount(player, dataManager.getDestroysData());
            case "deads" -> getPlayerCount(player, dataManager.getDeadsData());
            case "mobdie" -> getPlayerCount(player, dataManager.getMobdieData());
            case "onlinetime" -> getPlayerCount(player, dataManager.getOnlinetimeData());
            case "break_bedrock" -> getPlayerCount(player, dataManager.getBreakBedrockData());
            default -> {
                if (params.startsWith("place_")) {
                    yield getRankingEntryAsync(player, params, dataManager.getPlaceData());
                } else if (params.startsWith("destroys_")) {
                    yield getRankingEntryAsync(player, params, dataManager.getDestroysData());
                } else if (params.startsWith("deads_")) {
                    yield getRankingEntryAsync(player, params, dataManager.getDeadsData());
                } else if (params.startsWith("mobdie_")) {
                    yield getRankingEntryAsync(player, params, dataManager.getMobdieData());
                } else if (params.startsWith("onlinetime_")) {
                    yield getRankingEntryAsync(player, params, dataManager.getOnlinetimeData());
                } else if (params.startsWith("break_bedrock_")) {
                    yield getRankingEntryAsync(player, params, dataManager.getBreakBedrockData());
                }
                yield null;
            }
        };
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
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() ->
                getRankingEntry(player, params, jsonData));

        try {
            return future.get(); // 等待异步任务完成并返回结果
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error occurred while getting ranking entry", e);
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