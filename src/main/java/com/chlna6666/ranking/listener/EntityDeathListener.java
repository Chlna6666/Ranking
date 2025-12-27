package com.chlna6666.ranking.listener;

import com.chlna6666.ranking.Ranking;
import com.chlna6666.ranking.utils.Utils;
import org.bukkit.entity.Player;
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
        // 删除 CompletableFuture.runAsync，直接在事件线程运行
        if (plugin.getLeaderboardSettings().isLeaderboardEnabled("mobdie")) {
            if (event.getEntity().getKiller() != null) {
                Player player = event.getEntity().getKiller();

                var mobdieData = plugin.getDataManager().getMobdieData();
                var sidebarTitle = plugin.getI18n().translate("sidebar.kill");

                if (Utils.isFolia()) {
                    plugin.getServer().getGlobalRegionScheduler().run(plugin, task ->
                            plugin.handleEvent(player, "mobdie", mobdieData, sidebarTitle));
                } else {
                    // 已经在主线程，直接调用即可，无需 runTask
                    plugin.handleEvent(player, "mobdie", mobdieData, sidebarTitle);
                }
            }
        }
    }
}