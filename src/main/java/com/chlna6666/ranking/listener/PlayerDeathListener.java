package com.chlna6666.ranking.listener;

import com.chlna6666.ranking.Ranking;
import com.chlna6666.ranking.enums.LeaderboardType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {
    private final Ranking plugin;

    public PlayerDeathListener(Ranking plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (plugin.getLeaderboardSettings().isLeaderboardEnabled("deads")) {
            plugin.getRankingManager().handleEvent(event.getEntity(), LeaderboardType.DEADS);
        }
    }
}