package com.chlna6666.ranking.listener;

import com.chlna6666.ranking.Ranking;
import com.chlna6666.ranking.utils.Utils;
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
import java.util.concurrent.ConcurrentHashMap;

public class BlockPlaceListener implements Listener {

    private final Ranking plugin;
    private final Map<World, Map<Location, Player>> pistonCache = new ConcurrentHashMap<>();

    public BlockPlaceListener(Ranking plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        // 删除 CompletableFuture.runAsync
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (plugin.getLeaderboardSettings().isLeaderboardEnabled("place")) {
            if (Utils.isFolia()) {
                plugin.getServer().getGlobalRegionScheduler().run(plugin, task ->
                        plugin.handleEvent(player, "place", plugin.getDataManager().getPlaceData(),
                                plugin.getI18n().translate("sidebar.place")));
            } else {
                plugin.handleEvent(player, "place", plugin.getDataManager().getPlaceData(),
                        plugin.getI18n().translate("sidebar.place"));
            }
        }

        if (plugin.getLeaderboardSettings().isLeaderboardEnabled("break_bedrock")) {
            if (block.getType() == Material.PISTON || block.getType() == Material.STICKY_PISTON) {
                if (block.getBlockData() instanceof Directional directional) {
                    BlockFace pistonFacing = directional.getFacing();
                    Location bedrockPos = block.getRelative(pistonFacing).getLocation();
                    if (block.getWorld().getBlockAt(bedrockPos).getType() == Material.BEDROCK) {
                        pistonCache.computeIfAbsent(block.getWorld(), k -> new ConcurrentHashMap<>()).put(bedrockPos, player);
                    }
                }
            }
        }
    }

    public Map<World, Map<Location, Player>> getPistonCache() {
        return pistonCache;
    }
}