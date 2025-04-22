package com.chlna6666.ranking.listener;

import com.chlna6666.ranking.Ranking;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.concurrent.CompletableFuture;

public class EntityDeathListener implements Listener {

    private final Ranking plugin;

    public EntityDeathListener(Ranking plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        CompletableFuture.runAsync(() -> {
            if (plugin.getLeaderboardSettings().isLeaderboardEnabled("mobdie")) {
                if (event.getEntity().getKiller() != null) {
                    Player player = event.getEntity().getKiller();

                    var mobdieData = plugin.getDataManager().getMobdieData();
                    var sidebarTitle = plugin.getI18n().translate("sidebar.kill");

                    plugin.getServer().getScheduler().runTask(plugin, () ->
                            plugin.handleEvent(player, "mobdie", mobdieData, sidebarTitle));
                }
            }
        });
    }
}
