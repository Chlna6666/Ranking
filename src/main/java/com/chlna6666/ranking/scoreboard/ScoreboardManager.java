package com.chlna6666.ranking.scoreboard;

import com.chlna6666.ranking.datamanager.DataManager;
import com.chlna6666.ranking.I18n.I18n;
import com.chlna6666.ranking.Ranking;
import com.chlna6666.ranking.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;


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
    public void toggleScoreboard(Player player, String type) {
        // 先更新状态
        updateScoreboardStatus(player, type);

        // 然后获取标题和数据
        String title = ScoreboardUtils.getTitle(i18n, type);
        JSONObject data = ScoreboardUtils.getData(dataManager, type);

        // 检查更新后的状态
        if (isScoreboardEnabledFor(player)) {
            // 显示榜单
            ScoreboardUtils.clearScoreboard(player);
            player.sendMessage(title + " " + i18n.translate("command.enabled"));
            runUpdate(player, title, data, type);
        } else {
            // 隐藏榜单
            player.sendMessage(title + " " + i18n.translate("command.disabled"));
            ScoreboardUtils.clearScoreboard(player);
        }
    }

    /**
     * 刷新所有排行榜（所有玩家已开启的）
     */
    public void refreshAllScoreboards() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            JSONObject playerData = getPlayerData(player);
            if (playerData == null) continue;
            for (String type : DataManager.SUPPORTED_TYPES) {
                if (isScoreboardEnabled(playerData, type)) {
                    updateDisplay(player, type);
                }
            }
        }
    }

    /**
     * 刷新指定排行榜（开启它的玩家）
     */
    public void refreshScoreboard(String type) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            JSONObject playerData = getPlayerData(player);
            if (playerData == null) continue;
            if (isScoreboardEnabled(playerData, type)) {
                updateDisplay(player, type);
            }
        }
    }

    // Internal helper: update display for one player and one type
    private void updateDisplay(Player player, String type) {
        String title = ScoreboardUtils.getTitle(i18n, type);
        JSONObject data = ScoreboardUtils.getData(dataManager, type);
        ScoreboardUtils.clearScoreboard(player);
        runUpdate(player, title, data, type);
    }

    // 判断玩家当前是否开启对应 type 的榜单
    private boolean isScoreboardEnabledFor(Player player) {
        JSONObject playerData = getPlayerData(player);
        if (playerData == null) return false;
        // key stored after toggle
        for (String type : DataManager.SUPPORTED_TYPES) {
            Object value = playerData.get(type);
            if (value instanceof Number && ((Number) value).intValue() == 1) {
                return true;
            }
        }
        return false;
    }
    // 判断指定玩家数据对象 playerData 中 type 是否开启
    private boolean isScoreboardEnabled(JSONObject playerData, String type) {
        Object value = playerData.getOrDefault(type, 0L);
        return (value instanceof Number) && ((Number) value).intValue() == 1;
    }

    // 执行更新调用（考虑 Folia）
    private void runUpdate(Player player, String title, JSONObject data, String type) {
        if (Utils.isFolia()) {
            Bukkit.getRegionScheduler()
                    .run(plugin, player.getLocation(), task -> plugin.updateScoreboards(title, data, type));
        } else {
            plugin.updateScoreboards(title, data, type);
        }
    }

    // 更新数据库中玩家的开启状态 (Bug修复版)
    private void updateScoreboardStatus(Player player, String type) {
        JSONObject playersData = dataManager.getPlayersData();
        JSONObject playerData = (JSONObject) playersData.get(player.getUniqueId().toString());
        if (playerData == null) return;

        // 1. 先记录当前状态
        Object currentVal = playerData.getOrDefault(type, 0L);
        boolean wasEnabled = (currentVal instanceof Number && ((Number) currentVal).intValue() == 1);

        // 2. 将所有榜单状态重置为关闭
        for (String key : DataManager.SUPPORTED_TYPES) {
            playerData.put(key, 0L);
        }

        // 3. 如果之前是关闭的，才开启它；如果之前是开启的，上面循环已经把它关了，正好实现 toggle 效果
        if (!wasEnabled) {
            playerData.put(type, 1L);
        }

        dataManager.saveData("data", playersData);
    }

    private JSONObject getPlayerData(Player player) {
        return (JSONObject) dataManager.getPlayersData().get(player.getUniqueId().toString());
    }

}