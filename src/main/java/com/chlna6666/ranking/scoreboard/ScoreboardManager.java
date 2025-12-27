package com.chlna6666.ranking.scoreboard;

import com.chlna6666.ranking.datamanager.DataManager;
import com.chlna6666.ranking.I18n.I18n;
import com.chlna6666.ranking.Ranking;
import fr.mrmicky.fastboard.FastBoard;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ScoreboardManager {
    private final Ranking plugin;
    private final DataManager dataManager;
    private final I18n i18n;
    private final DynamicScoreboard dynamicHandler;

    // 存储玩家的 FastBoard 实例 (UUID -> FastBoard)
    private final Map<UUID, FastBoard> boards = new ConcurrentHashMap<>();

    public ScoreboardManager(Ranking plugin, DataManager dataManager, I18n i18n) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.i18n = i18n;
        this.dynamicHandler = new DynamicScoreboard(plugin, dataManager, i18n);
    }

    /**
     * 更新指定玩家的计分板内容
     * @param player 目标玩家
     * @param title 标题
     * @param lines 内容行
     */
    public void updateBoard(Player player, String title, List<String> lines) {
        FastBoard board = boards.computeIfAbsent(player.getUniqueId(), uuid -> new FastBoard(player));
        board.updateTitle(title);
        board.updateLines(lines);
    }

    /**
     * 移除玩家的计分板
     */
    public void removeBoard(Player player) {
        FastBoard board = boards.remove(player.getUniqueId());
        if (board != null) {
            board.delete();
        }
    }

    /**
     * 广播更新：给所有开启了对应榜单的玩家更新内容
     */
    public void broadcastUpdate(String type, String title, List<String> lines) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isScoreboardEnabledFor(player, type)) {
                updateBoard(player, title, lines);
            }
        }
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
        // 1. 更新数据库状态
        updateScoreboardStatus(player, type);

        String title = ScoreboardUtils.getTitle(i18n, type);

        // 2. 检查更新后的状态
        if (isScoreboardEnabledFor(player, type)) {
            player.sendMessage(title + " " + i18n.translate("command.enabled"));
            // 立即触发一次全服更新逻辑（虽然只针对该玩家生效，但会重新计算数据）
            plugin.updateScoreboards(title, ScoreboardUtils.getData(dataManager, type), type);
        } else {
            player.sendMessage(title + " " + i18n.translate("command.disabled"));
            removeBoard(player);
        }
    }

    public void refreshAllScoreboards() {
        for (String type : DataManager.SUPPORTED_TYPES) {
            String title = ScoreboardUtils.getTitle(i18n, type);
            JSONObject data = ScoreboardUtils.getData(dataManager, type);
            plugin.updateScoreboards(title, data, type);
        }
    }

    public void refreshScoreboard(String type) {
        String title = ScoreboardUtils.getTitle(i18n, type);
        JSONObject data = ScoreboardUtils.getData(dataManager, type);
        plugin.updateScoreboards(title, data, type);
    }

    private boolean isScoreboardEnabledFor(Player player, String targetType) {
        JSONObject playerData = getPlayerData(player);
        if (playerData == null) return false;

        Object value = playerData.get(targetType);
        return value instanceof Number && ((Number) value).intValue() == 1;
    }

    private void updateScoreboardStatus(Player player, String type) {
        JSONObject playersData = dataManager.getPlayersData();
        JSONObject playerData = (JSONObject) playersData.get(player.getUniqueId().toString());
        if (playerData == null) return;

        Object currentVal = playerData.getOrDefault(type, 0L);
        boolean wasEnabled = (currentVal instanceof Number && ((Number) currentVal).intValue() == 1);

        // 重置所有状态 (互斥显示)
        for (String key : DataManager.SUPPORTED_TYPES) {
            playerData.put(key, 0L);
        }
        playerData.put("dynamic", 0L); // 也要关闭动态

        // 切换
        if (!wasEnabled) {
            playerData.put(type, 1L);
        }

        dataManager.saveData("data", playersData);
    }

    private JSONObject getPlayerData(Player player) {
        return (JSONObject) dataManager.getPlayersData().get(player.getUniqueId().toString());
    }
}