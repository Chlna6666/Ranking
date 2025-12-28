package com.chlna6666.ranking.scoreboard;

import com.chlna6666.ranking.Ranking;
import com.chlna6666.ranking.utils.Utils;
import fr.mrmicky.fastboard.FastBoard;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ScoreboardManager {
    private final Ranking plugin;
    private final Map<UUID, FastBoard> boards = new ConcurrentHashMap<>();

    public ScoreboardManager(Ranking plugin) {
        this.plugin = plugin;
    }

    /**
     * 更新或创建玩家的计分板
     */
    public void updateBoard(Player player, String title, List<String> lines) {
        if (!player.isOnline()) return;
        FastBoard board = boards.computeIfAbsent(player.getUniqueId(), uuid -> new FastBoard(player));
        board.updateTitle(title);
        board.updateLines(lines);
    }

    /**
     * 移除玩家的计分板
     */
    public void removeBoard(Player player) {
        FastBoard board = boards.remove(player.getUniqueId());
        if (board != null) {
            board.delete();
        }
        tryResetVanillaScoreboard(player);
    }

    public void removeAll() {
        for (FastBoard board : boards.values()) {
            board.delete();
        }
        boards.clear();
    }

    // 尝试重置服务端状态
    private void tryResetVanillaScoreboard(Player player) {
        if (!player.isOnline()) return;
        try {
            if (!Utils.isFolia()) {
                org.bukkit.scoreboard.ScoreboardManager mgr = Bukkit.getScoreboardManager();
                if (mgr != null) {
                    player.setScoreboard(mgr.getNewScoreboard());
                    player.setScoreboard(mgr.getMainScoreboard());
                }
            } else {
                try {
                    player.getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }
}