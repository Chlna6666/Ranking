package com.chlna6666.ranking.listener;

import com.chlna6666.ranking.Ranking;
import com.chlna6666.ranking.statistics.OnlineTime;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerQuitListener implements Listener {

    private final Ranking plugin;
    private final OnlineTime onlineTime;

    public PlayerQuitListener(Ranking plugin, OnlineTime onlineTime) {
        this.plugin = plugin;
        this.onlineTime = onlineTime;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        onlineTime.cancelOnlineTimeTask(uuid);

        // 移除 FastBoard 实例
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().removeBoard(player);
        }

        plugin.getDataManager().saveAllData();
    }
}