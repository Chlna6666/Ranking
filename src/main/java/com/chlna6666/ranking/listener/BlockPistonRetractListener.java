package com.chlna6666.ranking.listener;

import com.chlna6666.ranking.Ranking;
import com.chlna6666.ranking.enums.LeaderboardType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonRetractEvent;
import java.util.Map;

public class BlockPistonRetractListener implements Listener {
    private final Ranking plugin;
    private final BlockPlaceListener blockPlaceListener;

    public BlockPistonRetractListener(Ranking plugin, BlockPlaceListener blockPlaceListener) {
        this.plugin = plugin;
        this.blockPlaceListener = blockPlaceListener;
    }

    @EventHandler
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
        if (!plugin.getLeaderboardSettings().isLeaderboardEnabled("break_bedrock")) return;

        Block piston = event.getBlock();
        if (piston.getBlockData() instanceof Directional directional) {
            BlockFace facing = directional.getFacing();
            Location target = piston.getRelative(facing).getLocation();

            Map<Location, Player> cache = blockPlaceListener.getPistonCache().get(piston.getWorld());
            if (cache != null && piston.getWorld().getBlockAt(target).getType() == Material.BEDROCK) {
                Player player = cache.remove(target);
                if (player != null) {
                    plugin.getRankingManager().handleEvent(player, LeaderboardType.BREAK_BEDROCK);
                }
            }
        }
    }
}