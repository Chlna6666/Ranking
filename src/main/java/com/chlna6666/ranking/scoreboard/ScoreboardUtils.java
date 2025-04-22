package com.chlna6666.ranking.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

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

}
