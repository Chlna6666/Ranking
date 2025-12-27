package com.chlna6666.ranking.listener;

import com.chlna6666.ranking.Ranking;
import com.chlna6666.ranking.utils.Utils;
import org.bukkit.entity.Player;
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
        // 删除 CompletableFuture.runAsync
        if (plugin.getLeaderboardSettings().isLeaderboardEnabled("deads")) {
            Player player = event.getEntity();

            var deathData = plugin.getDataManager().getDeadsData();
            var sidebarTitle = plugin.getI18n().translate("sidebar.death");

            if (Utils.isFolia()) {
                plugin.getServer().getGlobalRegionScheduler().run(plugin, task ->
                        plugin.handleEvent(player, "deads", deathData, sidebarTitle));
            } else {
                plugin.handleEvent(player, "deads", deathData, sidebarTitle);
            }
        }
    }
}