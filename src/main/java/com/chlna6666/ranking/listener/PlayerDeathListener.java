package com.chlna6666.ranking.listener;

import com.chlna6666.ranking.Ranking;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;


import java.util.concurrent.CompletableFuture;

public class PlayerDeathListener implements Listener {

    private final Ranking plugin;

    public PlayerDeathListener(Ranking plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void  onPlayerDeath(PlayerDeathEvent event) {
        CompletableFuture.runAsync(() -> {
            if (plugin.getLeaderboardSettings().isLeaderboardEnabled("deads")) {
                    Player player = event.getEntity();

                    var deathData = plugin.getDataManager().getDeadsData();
                    var sidebarTitle = plugin.getI18n().translate("sidebar.death");

                    plugin.getServer().getScheduler().runTask(plugin, () ->
                            plugin.handleEvent(player, "deads", deathData, sidebarTitle));
                }
        });
    }
}
