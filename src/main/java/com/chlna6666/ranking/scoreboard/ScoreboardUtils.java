package com.chlna6666.ranking.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ScoreboardUtils {

    // 存储各个 dataType 对应的计分板
    private static final Map<String, Scoreboard> scoreboardMap = new HashMap<>();
    // 存储各个 dataType 对应的 UUID -> 玩家名称映射
    private static final Map<String, Map<String, String>> uuidToNameMap = new HashMap<>();

    /**
     * 获取或创建指定 dataType 的计分板
     *
     * @param dataType 数据类型标识
     * @return 对应的计分板实例
     */
    public static Scoreboard getOrCreateScoreboard(String dataType) {
        if (!scoreboardMap.containsKey(dataType)) {
            ScoreboardManager manager = Bukkit.getScoreboardManager();
            Scoreboard scoreboard = manager.getNewScoreboard();
            scoreboardMap.put(dataType, scoreboard);
        }
        return scoreboardMap.get(dataType);
    }

    /**
     * 获取或创建指定 dataType 的 UUID 到玩家名称映射
     *
     * @param dataType 数据类型标识
     * @return 对应的 UUID 到名称映射
     */
    public static Map<String, String> getOrCreateUUIDMap(String dataType) {
        if (!uuidToNameMap.containsKey(dataType)) {
            uuidToNameMap.put(dataType, new HashMap<>());
        }
        return uuidToNameMap.get(dataType);
    }

    public static void clearScoreboard(Player player) {
        // 1. 获取 Bukkit 提供的 ScoreboardManager
        ScoreboardManager mgr = Bukkit.getScoreboardManager();
        if (mgr == null) return;

        // 2. 创建一份全新的、绝对空白的 Scoreboard
        Scoreboard emptyBoard = mgr.getNewScoreboard();

        // 3. 先给玩家切上这份空白板子
        player.setScoreboard(emptyBoard);

        // 4. 然后再遍历注销一遍（通常不会有任何 Objective，但以防万一）
        for (Objective obj : new ArrayList<>(emptyBoard.getObjectives())) {
            try {
                obj.unregister();
            } catch (Exception e) {
                Bukkit.getLogger().warning("无法注销 Objective: "
                        + obj.getName() + "，原因: " + e.getMessage());
            }
        }
    }

}
