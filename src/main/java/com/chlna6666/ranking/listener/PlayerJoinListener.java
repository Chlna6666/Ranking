package com.chlna6666.ranking.listener;

import com.chlna6666.ranking.Ranking;
import com.chlna6666.ranking.statistics.OnlineTime;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.json.simple.JSONObject;

import java.util.UUID;

public class PlayerJoinListener implements Listener {

    private final Ranking plugin;
    private final OnlineTime onlineTime;

    public PlayerJoinListener(Ranking plugin, OnlineTime onlineTime) {
        this.plugin = plugin;
        this.onlineTime = onlineTime;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String playerName = player.getName();

        // 1. 检查更新通知
        if (player.hasPermission("ranking.update.notify") && plugin.getConfig().getBoolean("update_checker.notify_on_login")) {
            plugin.getUpdateChecker().checkForUpdates(player);
        }

        // 2. 初始化或更新玩家数据（名字变更等）
        JSONObject playersData = plugin.getDataManager().getPlayersData();

        if (!playersData.containsKey(uuid.toString())) {
            JSONObject playerInfo = new JSONObject();
            playerInfo.put("name", playerName);
            playersData.put(uuid.toString(), playerInfo);
            plugin.getDataManager().saveData("data", playersData);
        } else {
            JSONObject storedPlayerInfo = (JSONObject) playersData.get(uuid.toString());
            String storedName = (String) storedPlayerInfo.get("name");
            if (!storedName.equals(playerName)) {
                storedPlayerInfo.put("name", playerName);
                plugin.getDataManager().saveData("data", playersData);
            }
        }

        // 3. 计分板处理 (FastBoard 适配)
        // 确保加入时移除旧的板子（如果有残留）
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().removeBoard(player);
        }

        // 恢复玩家之前的计分板状态 (如果玩家退出前是开启状态)
        plugin.updatePlayerScoreboards(uuid);

        // 4. 在线时间统计处理
        onlineTime.cancelOnlineTimeTask(uuid);

        // 如果启用在线时间计时，则创建并调度任务
        if (plugin.getLeaderboardSettings().isLeaderboardEnabled("onlinetime")) {
            onlineTime.scheduleOnlineTimeTask(uuid, 60, 60); // 调度在线时间任务（延迟和周期单位为秒）
        }
    }
}