package com.chlna6666.ranking.listener;

import com.chlna6666.ranking.Ranking;
import com.chlna6666.ranking.enums.LeaderboardType;
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
        Player player = event.getPlayer();

        if (plugin.getLeaderboardSettings().isLeaderboardEnabled("place")) {
            plugin.getRankingManager().handleEvent(player, LeaderboardType.PLACE);
        }

        if (plugin.getLeaderboardSettings().isLeaderboardEnabled("break_bedrock")) {
            Block block = event.getBlock();
            if (block.getType() == Material.PISTON || block.getType() == Material.STICKY_PISTON) {
                if (block.getBlockData() instanceof Directional directional) {
                    BlockFace facing = directional.getFacing();
                    Location target = block.getRelative(facing).getLocation();
                    if (block.getWorld().getBlockAt(target).getType() == Material.BEDROCK) {
                        pistonCache.computeIfAbsent(block.getWorld(), k -> new ConcurrentHashMap<>()).put(target, player);
                    }
                }
            }
        }
    }

    public Map<World, Map<Location, Player>> getPistonCache() { return pistonCache; }
}