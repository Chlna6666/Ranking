package com.chlna6666.ranking.scoreboard;

import com.chlna6666.ranking.I18n.I18n;
import com.chlna6666.ranking.datamanager.DataManager;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

public class ScoreboardUtils {
    private final I18n i18n;

    public ScoreboardUtils(I18n i18n) {
        this.i18n = i18n;
    }

    /**
     * 将 JSON 数据格式化为排行榜文本行列表
     * @param data 排行榜数据 (UUID -> Score)
     * @param playersData 玩家详细数据 (UUID -> {name: ...})
     * @param topN 显示前 N 名
     * @return 格式化后的文本行列表
     */
    public static List<String> formatLines(Map<String, Long> data, JSONObject playersData, int topN) {
        List<String> lines = new ArrayList<>();

        // 1. 排序
        List<Map.Entry<String, Long>> topEntries = data.entrySet().stream()
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

            // 格式: "1. PlayerName: 100"
            // 这里使用了黄色序号，白色名字，绿色分数，你可以根据需要修改颜色
            lines.add("§e" + rank + ". §f" + playerName + ": §a" + scoreVal);
            rank++;
        }

        if (lines.isEmpty()) {
            lines.add("§7No Data");
        }

        return lines;
    }

    // 获取侧边栏标题
    public static String getTitle(I18n i18n, String key) {
        return switch (key) {
            case "place" -> i18n.translate("sidebar.place");
            case "destroys" -> i18n.translate("sidebar.break");
            case "deads" -> i18n.translate("sidebar.death");
            case "mobdie" -> i18n.translate("sidebar.kill");
            case "onlinetime" -> i18n.translate("sidebar.online_time");
            case "break_bedrock" -> i18n.translate("sidebar.break_bedrock");
            default -> i18n.translate("sidebar.unknown");
        };
    }

    // 获取排名数据
    public static JSONObject getData(DataManager dm, String key) {
        return switch (key) {
            case "place" -> dm.getPlaceData();
            case "destroys" -> dm.getDestroysData();
            case "deads" -> dm.getDeadsData();
            case "mobdie" -> dm.getMobdieData();
            case "onlinetime" -> dm.getOnlinetimeData();
            case "break_bedrock" -> dm.getBreakBedrockData();
            default -> new JSONObject();
        };
    }
}