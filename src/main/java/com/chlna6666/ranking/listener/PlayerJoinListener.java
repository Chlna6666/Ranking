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

        if (player.hasPermission("ranking.update.notify") && plugin.getConfig().getBoolean("update_checker.notify_on_login")) {
            plugin.getUpdateChecker().checkForUpdates(player);
        }
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

        plugin.clearScoreboard(player);
        plugin.updatePlayerScoreboards(uuid);

        onlineTime.cancelOnlineTimeTask(uuid);

        // 如果启用在线时间计时，则创建并调度任务
        if (plugin.getLeaderboardSettings().isLeaderboardEnabled("onlinetime")) {
            onlineTime.scheduleOnlineTimeTask(uuid, 60, 60); // 调度在线时间任务（延迟和周期单位为秒）
        }
    }
}