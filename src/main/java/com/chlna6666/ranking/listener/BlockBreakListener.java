package com.chlna6666.ranking.listener;

import com.chlna6666.ranking.Ranking;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.concurrent.CompletableFuture;

public class BlockBreakListener implements Listener {

    private final Ranking plugin;

    public BlockBreakListener(Ranking plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
            CompletableFuture.runAsync(() -> {
                if (plugin.getLeaderboardSettings().isLeaderboardEnabled("destroys")) {
                    Player player = event.getPlayer();
                    // 获取需要的数据
                    var destroysData = plugin.getDataManager().getDestroysData();
                    var sidebarTitle = plugin.getI18n().translate("sidebar.break");

                    // 主线程更新计分板
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                            plugin.handleEvent(player, "destroys", destroysData, sidebarTitle));
                }
            });
        }
    }
