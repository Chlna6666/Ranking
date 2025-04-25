package com.chlna6666.ranking.scoreboard;

import com.chlna6666.ranking.datamanager.DataManager;
import com.chlna6666.ranking.I18n.I18n;
import com.chlna6666.ranking.Ranking;
import com.chlna6666.ranking.scoreboard.ScoreboardUtils;
import com.chlna6666.ranking.utils.Utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONObject;

import java.util.*;

/**
 * 独立的动态记分板管理器，支持自定义轮换间隔配置
 */
public class DynamicScoreboard {
    private final Ranking plugin;
    private final DataManager dataManager;
    private final I18n i18n;
    private final Map<UUID, BukkitRunnable> tasks = new HashMap<>();
    private final List<String> keys = Arrays.asList(
            "place", "destroys", "deads", "mobdie", "onlinetime", "break_bedrock"
    );
    /** 轮换间隔（tick），可通过配置项 dynamic.rotation_interval_minutes 设置，默认为5分钟 */
    private final long rotationIntervalTicks;

    public DynamicScoreboard(Ranking plugin, DataManager dataManager, I18n i18n) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.i18n = i18n;
        long minutes = plugin.getConfig().getLong("dynamic.rotation_interval_minutes", 5L);
        this.rotationIntervalTicks = Math.max(1L, minutes) * 1200L;
    }

    /**
     * 切换动态轮换记分板
     */
    public void toggle(Player player, JSONObject playersData) {
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

        if (next == 0) {
            stop(player, playersData);
        } else {
            start(player, playersData);
        }
    }

    /**
     * 启动动态轮换任务
     */
    private void start(Player player, JSONObject playersData) {
        UUID uuid = player.getUniqueId();
        // 取消旧任务
        if (tasks.containsKey(uuid)) {
            tasks.remove(uuid).cancel();
        }

        // 筛选可用 key
        List<String> enabled = new ArrayList<>();
        for (String k : keys) {
            if (plugin.getLeaderboardSettings().isLeaderboardEnabled(k)) {
                enabled.add(k);
            }
        }
        if (enabled.isEmpty()) {
            player.sendMessage(i18n.translate("sidebar.dynamic") + " " + i18n.translate("dynamic.none_enabled"));
            // 自动关闭
            pdataPut(playersData, uuid, "dynamic", 0L);
            return;
        }

        // 创建并调度定时任务：立即执行一次 + 自定义间隔
        BukkitRunnable task = new BukkitRunnable() {
            private int idx = 0;
            @Override
            public void run() {
                JSONObject pd = (JSONObject) playersData.get(uuid.toString());
                // 重置所有已启用 key
                for (String k : enabled) {
                    pd.put(k, 0L);
                }
                // 当前轮换 key
                String ckey = enabled.get(idx);
                pd.put(ckey, 1L);
                dataManager.saveData("data", playersData);

                // 更新记分板
                if (Utils.isFolia()) {
                    Bukkit.getRegionScheduler().run(plugin, player.getLocation(), __ ->
                            updateBoard(player, ckey)
                    );
                } else {
                    updateBoard(player, ckey);
                }
                idx = (idx + 1) % enabled.size();
            }
        };
        task.runTaskTimer(plugin, 0L, rotationIntervalTicks);
        tasks.put(uuid, task);
    }

    /**
     * 停止动态轮换，并重置所有 key
     */
    private void stop(Player player, JSONObject playersData) {
        UUID uuid = player.getUniqueId();
        if (tasks.containsKey(uuid)) {
            tasks.remove(uuid).cancel();
        }
        JSONObject pd = (JSONObject) playersData.get(uuid.toString());
        for (String k : keys) {
            pd.put(k, 0L);
        }
        dataManager.saveData("data", playersData);
        ScoreboardUtils.clearScoreboard(player);
    }

    /**
     * 更新记分板内容
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

    private void pdataPut(JSONObject playersData, UUID uuid, String key, Object val) {
        JSONObject pd = (JSONObject) playersData.get(uuid.toString());
        if (pd != null) pd.put(key, val);
    }
}
