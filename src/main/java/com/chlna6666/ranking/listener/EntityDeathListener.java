package com.chlna6666.ranking.listener;

import com.chlna6666.ranking.Ranking;
import com.chlna6666.ranking.enums.LeaderboardType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class EntityDeathListener implements Listener {
    private final Ranking plugin;

    public EntityDeathListener(Ranking plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (plugin.getLeaderboardSettings().isLeaderboardEnabled("mobdie")) {
            if (event.getEntity().getKiller() != null) {
                plugin.getRankingManager().handleEvent(event.getEntity().getKiller(), LeaderboardType.MOB_DIE);
            }
        }
    }
}