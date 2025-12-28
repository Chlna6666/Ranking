package com.chlna6666.ranking.scoreboard;

import com.chlna6666.ranking.I18n.I18n;
import com.chlna6666.ranking.enums.LeaderboardType;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

public class ScoreboardUtils {

    /**
     * 将 JSON 数据格式化为排行榜文本行列表
     */
    public static List<String> formatLines(JSONObject data, JSONObject playersData, int topN) {
        List<String> lines = new ArrayList<>();
        if (data == null) {
            lines.add("§7No Data");
            return lines;
        }

        // 强转 Map 以便流处理
        Map<String, Object> mapData = (Map<String, Object>) data;

        // 1. 排序
        List<Map.Entry<String, Long>> topEntries = mapData.entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), parseLong(e.getValue())))
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(topN)
                .collect(Collectors.toList());

        // 2. 格式化
        int rank = 1;
        for (Map.Entry<String, Long> entry : topEntries) {
            String uuid = entry.getKey();
            long scoreVal = entry.getValue();

            String playerName = "Unknown";
            if (playersData != null && playersData.containsKey(uuid)) {
                JSONObject pdata = (JSONObject) playersData.get(uuid);
                playerName = (String) pdata.getOrDefault("name", "Unknown");
            }

            lines.add("§e" + rank + ". §f" + playerName + ": §a" + scoreVal);
            rank++;
        }

        if (lines.isEmpty()) {
            lines.add("§7No Data");
        }

        return lines;
    }

    private static long parseLong(Object obj) {
        if (obj instanceof Number) return ((Number) obj).longValue();
        try {
            return Long.parseLong(obj.toString());
        } catch (Exception e) {
            return 0L;
        }
    }

    public static String getTitle(I18n i18n, LeaderboardType type) {
        return i18n.translate(type.getLangKey());
    }
}