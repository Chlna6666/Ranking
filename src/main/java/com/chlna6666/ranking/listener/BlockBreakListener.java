package com.chlna6666.ranking.listener;

import com.chlna6666.ranking.Ranking;
import com.chlna6666.ranking.utils.Utils;
import org.bukkit.entity.Player;
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
        // 删除 CompletableFuture.runAsync
        if (plugin.getLeaderboardSettings().isLeaderboardEnabled("destroys")) {
            Player player = event.getPlayer();
            var destroysData = plugin.getDataManager().getDestroysData();
            var sidebarTitle = plugin.getI18n().translate("sidebar.break");

            if (Utils.isFolia()) {
                plugin.getServer().getGlobalRegionScheduler().run(plugin, task ->
                        plugin.handleEvent(player, "destroys", destroysData, sidebarTitle));
            } else {
                plugin.handleEvent(player, "destroys", destroysData, sidebarTitle);
            }
        }
    }
}