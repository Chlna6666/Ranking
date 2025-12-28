package com.chlna6666.ranking.listener;

import com.chlna6666.ranking.Ranking;
import com.chlna6666.ranking.enums.LeaderboardType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockBreakListener implements Listener {
    private final Ranking plugin;

    public BlockBreakListener(Ranking plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (plugin.getLeaderboardSettings().isLeaderboardEnabled("destroys")) {
            plugin.getRankingManager().handleEvent(event.getPlayer(), LeaderboardType.DESTROYS);
        }
    }
}