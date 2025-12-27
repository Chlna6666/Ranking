package com.chlna6666.ranking.scoreboard;

import com.chlna6666.ranking.datamanager.DataManager;
import com.chlna6666.ranking.I18n.I18n;
import com.chlna6666.ranking.Ranking;
import com.chlna6666.ranking.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DynamicScoreboard {
    private final Ranking plugin;
    private final DataManager dataManager;
    private final I18n i18n;
    private final List<String> allKeys = DataManager.SUPPORTED_TYPES;
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Map<UUID, Integer> playerIndexes = new HashMap<>();

    public DynamicScoreboard(Ranking plugin, DataManager dataManager, I18n i18n) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.i18n = i18n;
        long interval = plugin.getConfig().getLong("dynamic.rotation_interval_minutes", 5L);

        // 调度轮询
        scheduler.scheduleAtFixedRate(() -> {
            if (Utils.isFolia()) {
                Bukkit.getGlobalRegionScheduler().run(plugin, t -> tickAll());
            } else {
                Bukkit.getScheduler().runTask(plugin, this::tickAll);
            }
        }, 0L, interval, TimeUnit.MINUTES);
    }

    public static void shutdown() {
        scheduler.shutdownNow();
    }

    public void toggle(Player player) {
        JSONObject pdata = getPlayerData(player);
        if (pdata == null) return;

        boolean enabled = toggleFlag(pdata, "dynamic");
        dataManager.saveData("data", dataManager.getPlayersData());

        String prefix = i18n.translate("sidebar.dynamic") + " ";
        player.sendMessage(prefix + i18n.translate(enabled ? "command.enabled" : "command.disabled"));

        if (enabled) {
            playerIndexes.put(player.getUniqueId(), 0);
            resetFlags(pdata, allKeys); // 关闭其他静态

            List<String> enabledKeys = getEnabledKeys();
            if (!enabledKeys.isEmpty()) {
                updateFor(player, enabledKeys.get(0));
            }
        } else {
            playerIndexes.remove(player.getUniqueId());
            resetFlags(pdata, allKeys);
            // 移除计分板
            plugin.getScoreboardManager().removeBoard(player);
        }
    }

    private void tickAll() {
        JSONObject allData = dataManager.getPlayersData();
        List<String> enabledKeys = getEnabledKeys();
        if (enabledKeys.isEmpty()) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            JSONObject pdata = (JSONObject) allData.get(uuid.toString());
            // 检查 dynamic 开关
            if (!isFlagSet(pdata, "dynamic")) continue;

            int idx = playerIndexes.getOrDefault(uuid, 0);
            String key = enabledKeys.get(idx);

            updateFor(player, key);

            playerIndexes.put(uuid, (idx + 1) % enabledKeys.size());
        }
    }

    private List<String> getEnabledKeys() {
        return allKeys.stream()
                .filter(k -> plugin.getLeaderboardSettings().isLeaderboardEnabled(k))
                .collect(Collectors.toList());
    }

    private JSONObject getPlayerData(Player player) {
        return (JSONObject) dataManager.getPlayersData().get(player.getUniqueId().toString());
    }

    private boolean toggleFlag(JSONObject pdata, String key) {
        long cur = pdata.getOrDefault(key, 0L) instanceof Number
                ? ((Number) pdata.get(key)).longValue() : 0L;
        pdata.put(key, cur == 0 ? 1L : 0L);
        return cur == 0;
    }

    private boolean isFlagSet(JSONObject pdata, String key) {
        Object value = pdata != null ? pdata.getOrDefault(key, 0L) : 0L;
        return value instanceof Number && ((Number) value).intValue() == 1;
    }

    private void resetFlags(JSONObject pdata, List<String> keys) {
        for (String k : keys) pdata.put(k, 0L);
    }

    private void updateFor(Player player, String key) {
        String title = ScoreboardUtils.getTitle(i18n, key);
        JSONObject data = ScoreboardUtils.getData(dataManager, key);
        // 注意：这里默认取前10名，你也可以从config读取 top_n
        List<String> lines = ScoreboardUtils.formatLines(data, dataManager.getPlayersData(), 10);

        // 更新
        plugin.getScoreboardManager().updateBoard(player, title, lines);
    }
}