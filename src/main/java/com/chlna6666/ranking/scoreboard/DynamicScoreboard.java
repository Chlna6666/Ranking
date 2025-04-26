package com.chlna6666.ranking.scoreboard;

import com.chlna6666.ranking.datamanager.DataManager;
import com.chlna6666.ranking.I18n.I18n;
import com.chlna6666.ranking.Ranking;
import com.chlna6666.ranking.scoreboard.ScoreboardUtils;
import com.chlna6666.ranking.utils.Utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 独立的动态记分板管理器——使用 Java ScheduledExecutorService 全局定时轮询
 */
public class DynamicScoreboard {
    private final Ranking plugin;
    private final DataManager dataManager;
    private final I18n i18n;
    private final List<String> allKeys = Arrays.asList(
            "place", "destroys", "deads", "mobdie", "onlinetime", "break_bedrock"
    );
    private static ScheduledExecutorService scheduler;

    /**
     * 记录每个玩家当前轮换索引
     */
    private final Map<UUID, Integer> playerIndexes = new HashMap<>();

    public DynamicScoreboard(Ranking plugin, DataManager dataManager, I18n i18n) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.i18n = i18n;
        // 从配置读取轮换间隔（分钟）
        long rotationIntervalMinutes = plugin.getConfig().getLong("dynamic.rotation_interval_minutes", 5L);
        // 创建单线程调度器
        scheduler = Executors.newSingleThreadScheduledExecutor();
        // 调度全局轮询任务：在主线程执行 tickAll
        scheduler.scheduleAtFixedRate(() ->
                        Bukkit.getScheduler().runTask(plugin, this::tickAll),
                0L, rotationIntervalMinutes, TimeUnit.MINUTES
        );
    }

    /**
     * 停用时需调用以释放线程池资源
     */
    public static void shutdown() {
        if (scheduler != null) scheduler.shutdownNow();
    }

    /**
     * 玩家执行 /rk dynamic 时调用，切换状态、初始化索引，并立即更新一次以避免空白
     */
    public void toggle(Player player) {
        JSONObject playersData = dataManager.getPlayersData();
        UUID uuid = player.getUniqueId();
        JSONObject pdata = (JSONObject) playersData.get(uuid.toString());
        if (pdata == null) return;

        // 切换 dynamic 标记
        long cur = pdata.containsKey("dynamic") && pdata.get("dynamic") instanceof Number
                ? ((Number) pdata.get("dynamic")).longValue() : 0L;
        long next = cur == 0 ? 1 : 0;
        pdata.put("dynamic", next);
        dataManager.saveData("data", playersData);

        String prefix = i18n.translate("sidebar.dynamic") + " ";
        player.sendMessage(next == 1
                ? prefix + i18n.translate("command.enabled")
                : prefix + i18n.translate("command.disabled")
        );

        if (next == 1) {
            // 开启：初始化索引
            playerIndexes.put(uuid, 0);
            // 立即执行一次轮换更新，避免玩家面板空白
            // 筛选可用 key
            List<String> enabled = new ArrayList<>();
            for (String k : allKeys) {
                if (plugin.getLeaderboardSettings().isLeaderboardEnabled(k)) {
                    enabled.add(k);
                }
            }
            if (!enabled.isEmpty()) {
                // 重置状态并更新第一个 key
                resetPlayerKeys(pdata, enabled);
                String firstKey = enabled.get(0);
                pdata.put(firstKey, 1L);
                dataManager.saveData("data", playersData);
                if (Utils.isFolia()) {
                    Bukkit.getRegionScheduler().run(plugin, player.getLocation(), __ -> updateBoard(player, firstKey));
                } else {
                    updateBoard(player, firstKey);
                }
            }
        } else {
            // 关闭：移除索引，重置所有 key 并清空板子
            playerIndexes.remove(uuid);
            resetPlayerKeys(pdata, allKeys);
            ScoreboardUtils.clearScoreboard(player);
        }
    }

    /**
     * 全局轮询：遍历所有在线玩家并更新动态记分板
     */
    private void tickAll() {
        JSONObject playersData = dataManager.getPlayersData();
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            JSONObject pdata = (JSONObject) playersData.get(uuid.toString());
            if (pdata == null) continue;
            Object dyn = pdata.get("dynamic");
            if (!(dyn instanceof Number) || ((Number) dyn).longValue() != 1) continue;

            // 筛选可用 key
            List<String> enabled = new ArrayList<>();
            for (String k : allKeys) {
                if (plugin.getLeaderboardSettings().isLeaderboardEnabled(k)) {
                    enabled.add(k);
                }
            }
            if (enabled.isEmpty()) continue;

            // 轮换索引
            int idx = playerIndexes.getOrDefault(uuid, 0);
            // 重置状态
            resetPlayerKeys(pdata, enabled);

            String key = enabled.get(idx);
            pdata.put(key, 1L);
            dataManager.saveData("data", playersData);

            // 更新记分板
            if (Utils.isFolia()) {
                Bukkit.getRegionScheduler().run(plugin, player.getLocation(), __ -> updateBoard(player, key));
            } else {
                updateBoard(player, key);
            }

            playerIndexes.put(uuid, (idx + 1) % enabled.size());
        }
    }

    /**
     * 重置玩家指定 keys 的状态
     */
    private void resetPlayerKeys(JSONObject pdata, List<String> keys) {
        for (String k : keys) pdata.put(k, 0L);
    }

    /**
     * 更新玩家记分板内容
     */
    private void updateBoard(Player player, String key) {
        String title;
        JSONObject data;
        switch (key) {
            case "place":        title = i18n.translate("sidebar.place");        data = dataManager.getPlaceData(); break;
            case "destroys":     title = i18n.translate("sidebar.break");        data = dataManager.getDestroysData(); break;
            case "deads":        title = i18n.translate("sidebar.death");        data = dataManager.getDeadsData(); break;
            case "mobdie":       title = i18n.translate("sidebar.kill");         data = dataManager.getMobdieData(); break;
            case "onlinetime":   title = i18n.translate("sidebar.online_time");  data = dataManager.getOnlinetimeData(); break;
            case "break_bedrock":title = i18n.translate("sidebar.break_bedrock"); data = dataManager.getBreakBedrockData(); break;
            default: return;
        }
        plugin.updateScoreboards(title, data, key);
    }
}
