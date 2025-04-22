package com.chlna6666.ranking.listener;

import com.chlna6666.ranking.Ranking;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class BlockPistonRetractListener implements Listener {

    private final Ranking plugin;
    private final BlockPlaceListener blockPlaceListener;

    public BlockPistonRetractListener(Ranking plugin, BlockPlaceListener blockPlaceListener) {
        this.plugin = plugin;
        this.blockPlaceListener = blockPlaceListener;
    }

    @EventHandler
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
        CompletableFuture.runAsync(() -> {
            if (plugin.getLeaderboardSettings().isLeaderboardEnabled("break_bedrock")) {
                Block piston = event.getBlock();
                Directional directional = (Directional) piston.getBlockData();
                BlockFace pistonFacing = directional.getFacing();
                Location bedrockPos = piston.getRelative(pistonFacing).getLocation();

                World world = piston.getWorld();
                Map<Location, Player> cache = blockPlaceListener.getPistonCache().get(world);
                if (cache != null) {
                    if (world.getBlockAt(bedrockPos).getType() == Material.BEDROCK) {
                        Player player = cache.remove(bedrockPos);
                        if (player != null) {
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                plugin.handleEvent(player, "break_bedrock", plugin.getDataManager().getBreakBedrockData(),
                                        plugin.getI18n().translate("sidebar.break_bedrock"));
                                //getLogger().info("基岩被活塞收缩事件破坏，由 " + player.getName() + " 触发，位置：" + bedrockPos);
                            });
                        }
                    }
                }
            }

        });
    }
}
