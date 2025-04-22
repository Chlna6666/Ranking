package com.chlna6666.ranking.listener;

import com.chlna6666.ranking.Ranking;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class BlockPlaceListener implements Listener {

    private final Ranking plugin;
    // 用于记录活塞放置时的缓存数据（仅对 break_bedrock 有效）
    private final Map<World, Map<Location, Player>> pistonCache = new ConcurrentHashMap<>();

    public BlockPlaceListener(Ranking plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        CompletableFuture.runAsync(() -> {
            Player player = event.getPlayer();
            Block block = event.getBlock();

            if (plugin.getLeaderboardSettings().isLeaderboardEnabled("place")) {
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        plugin.handleEvent(player, "place", plugin.getDataManager().getPlaceData(),
                                plugin.getI18n().translate("sidebar.place")));
            }

            if (plugin.getLeaderboardSettings().isLeaderboardEnabled("break_bedrock")) {
                // 检查放置的是活塞（普通活塞或黏性活塞）
                if (block.getType() == Material.PISTON || block.getType() == Material.STICKY_PISTON) {
                    Directional directional = (Directional) block.getBlockData();
                    BlockFace pistonFacing = directional.getFacing();
                    Location bedrockPos = block.getRelative(pistonFacing).getLocation();
                    if (block.getWorld().getBlockAt(bedrockPos).getType() == Material.BEDROCK) {
                        pistonCache.computeIfAbsent(block.getWorld(), k -> new ConcurrentHashMap<>()).put(bedrockPos, player);
                    }
                }
            }
         });
    }

    // 为 BlockPistonRetractListener 提供活塞缓存的访问方法
    public Map<World, Map<Location, Player>> getPistonCache() {
        return pistonCache;
    }
}
