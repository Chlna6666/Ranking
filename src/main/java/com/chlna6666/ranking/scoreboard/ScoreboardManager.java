package com.chlna6666.ranking.scoreboard;

import com.chlna6666.ranking.datamanager.DataManager;
import com.chlna6666.ranking.I18n.I18n;
import com.chlna6666.ranking.Ranking;
import com.chlna6666.ranking.utils.Utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;

import java.util.*;


public class ScoreboardManager {
    private final Ranking plugin;
    private final DataManager dataManager;
    private final I18n i18n;

    private final DynamicScoreboard dynamicHandler;

    public ScoreboardManager(Ranking plugin, DataManager dataManager, I18n i18n) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.i18n = i18n;
        this.dynamicHandler = new DynamicScoreboard(plugin, dataManager, i18n);
    }

    /**
     * 切换并执行动态记分板逻辑
     */
    public void dynamicScoreboard(Player player) {
        dynamicHandler.toggle(player);
    }
    /**
     * 切换静态榜单显示/隐藏
     */
    public void toggleScoreboard(Player player, String rankingName, String displayName, JSONObject rankingData) {
        updateScoreboardStatus(player, rankingName);
        int scoreboardStatus = getPlayerScoreboardStatus(player, rankingName);
        if (scoreboardStatus == 1) {
            ScoreboardUtils.clearScoreboard(player);
            player.sendMessage(displayName + i18n.translate("command.enabled"));
            if (Utils.isFolia()) {
                Bukkit.getRegionScheduler().run(plugin, player.getLocation(), scheduledTask ->
                        plugin.updateScoreboards(displayName, rankingData, rankingName));
            } else {
                plugin.updateScoreboards(displayName, rankingData, rankingName);
            }
        } else {
            player.sendMessage(displayName + i18n.translate("command.disabled"));
            ScoreboardUtils.clearScoreboard(player);
        }
    }

    private void updateScoreboardStatus(Player player, String rankingValue) {
        List<String> specificKeys = Arrays.asList("place", "destroys", "deads", "mobdie", "onlinetime", "break_bedrock", "dynamic");
        UUID uuid = player.getUniqueId();
        JSONObject playersData = dataManager.getPlayersData();
        JSONObject playerData = (JSONObject) playersData.get(uuid.toString());
        if (playerData != null) {
            for (String key : specificKeys) {
                if (!key.equals(rankingValue)) {
                    playerData.put(key, 0L);
                }
            }
            if (specificKeys.contains(rankingValue)) {
                long currentValue = playerData.containsKey(rankingValue) ? (Long) playerData.get(rankingValue) : 0;
                long newStatus = (currentValue == 0) ? 1 : 0;
                playerData.put(rankingValue, newStatus);
            }
            dataManager.saveData("data", playersData);
        }
    }

    private int getPlayerScoreboardStatus(Player player, String rankingValue) {
        JSONObject playerData = getPlayerData(player);
        if (playerData != null && playerData.containsKey(rankingValue)) {
            Object value = playerData.get(rankingValue);
            if (value instanceof Long) {
                return ((Long) value).intValue();
            } else if (value instanceof Integer) {
                return (Integer) value;
            }
        }
        return 0;
    }

    private JSONObject getPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        JSONObject playersData = dataManager.getPlayersData();
        return (JSONObject) playersData.get(uuid.toString());
    }
}
