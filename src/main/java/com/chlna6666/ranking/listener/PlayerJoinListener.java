package com.chlna6666.ranking.listener;

import com.chlna6666.ranking.Ranking;
import com.chlna6666.ranking.statistics.OnlineTime;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.json.simple.JSONObject;

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
        String uuid = player.getUniqueId().toString();

        if (player.hasPermission("ranking.update.notify") && plugin.getConfig().getBoolean("update_checker.notify_on_login")) {
            plugin.getUpdateChecker().checkForUpdates(player);
        }

        JSONObject playersData = plugin.getDataManager().getPlayersData();
        if (!playersData.containsKey(uuid)) {
            JSONObject info = new JSONObject();
            info.put("name", player.getName());
            playersData.put(uuid, info);
            plugin.getDataManager().saveData("data", playersData);
        } else {
            JSONObject info = (JSONObject) playersData.get(uuid);
            if (!player.getName().equals(info.get("name"))) {
                info.put("name", player.getName());
                plugin.getDataManager().saveData("data", playersData);
            }
        }

        // 恢复计分板
        plugin.getRankingManager().restorePlayerScoreboard(player);

        onlineTime.cancelOnlineTimeTask(player.getUniqueId());
        if (plugin.getLeaderboardSettings().isLeaderboardEnabled("onlinetime")) {
            onlineTime.scheduleOnlineTimeTask(player.getUniqueId(), 60, 60);
        }
    }
}