package com.chlna6666.ranking.listener;

import com.chlna6666.ranking.Ranking;
import com.chlna6666.ranking.statistics.OnlineTime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {
    private final Ranking plugin;
    private final OnlineTime onlineTime;

    public PlayerQuitListener(Ranking plugin, OnlineTime onlineTime) {
        this.plugin = plugin;
        this.onlineTime = onlineTime;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        onlineTime.cancelOnlineTimeTask(event.getPlayer().getUniqueId());
        plugin.getScoreboardManager().removeBoard(event.getPlayer());
        plugin.getDataManager().saveAllData();
    }
}