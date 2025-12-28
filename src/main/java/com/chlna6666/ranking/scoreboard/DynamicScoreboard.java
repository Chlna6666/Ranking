package com.chlna6666.ranking.scoreboard;

import com.chlna6666.ranking.Ranking;
import com.chlna6666.ranking.datamanager.DataManager;
import com.chlna6666.ranking.enums.LeaderboardType;
import com.chlna6666.ranking.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class DynamicScoreboard {
    private final Ranking plugin;
    private final DataManager dataManager;
    private final ScoreboardManager scoreboardManager;
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Map<UUID, Integer> playerIndexes = new ConcurrentHashMap<>();

    public DynamicScoreboard(Ranking plugin, DataManager dataManager, ScoreboardManager scoreboardManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.scoreboardManager = scoreboardManager;

        long interval = plugin.getConfig().getLong("dynamic.rotation_interval_minutes", 5L);
        scheduler.scheduleAtFixedRate(() -> {
            if (Utils.isFolia()) Bukkit.getGlobalRegionScheduler().run(plugin, t -> tick());
            else Bukkit.getScheduler().runTask(plugin, this::tick);
        }, 0L, interval, TimeUnit.MINUTES);
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }

    public void toggle(Player player) {
        JSONObject pdata = (JSONObject) dataManager.getPlayersData().get(player.getUniqueId().toString());
        if (pdata == null) return;

        boolean enabled = !isFlagSet(pdata, "dynamic");

        // 重置所有静态榜单
        for (LeaderboardType type : LeaderboardType.values()) {
            pdata.put(type.getId(), 0L);
        }
        pdata.put("dynamic", enabled ? 1L : 0L);
        dataManager.saveData("data", dataManager.getPlayersData());

        player.sendMessage(plugin.getI18n().translate("sidebar.dynamic") + " " +
                plugin.getI18n().translate(enabled ? "command.enabled" : "command.disabled"));

        if (enabled) {
            playerIndexes.put(player.getUniqueId(), 0);
            updateFor(player);
        } else {
            playerIndexes.remove(player.getUniqueId());
            scoreboardManager.removeBoard(player);
        }
    }

    private void tick() {
        List<LeaderboardType> enabledTypes = getEnabledTypes();
        if (enabledTypes.isEmpty()) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            JSONObject pdata = (JSONObject) dataManager.getPlayersData().get(player.getUniqueId().toString());
            if (!isFlagSet(pdata, "dynamic")) continue;

            int idx = playerIndexes.getOrDefault(player.getUniqueId(), 0);

            // 更新显示
            LeaderboardType type = enabledTypes.get(idx % enabledTypes.size());
            render(player, type);

            // 移动索引
            playerIndexes.put(player.getUniqueId(), (idx + 1) % enabledTypes.size());
        }
    }

    private void updateFor(Player player) {
        List<LeaderboardType> enabled = getEnabledTypes();
        if (!enabled.isEmpty()) render(player, enabled.get(0));
    }

    private void render(Player player, LeaderboardType type) {
        String title = ScoreboardUtils.getTitle(plugin.getI18n(), type);
        List<String> lines = ScoreboardUtils.formatLines(dataManager.getData(type), dataManager.getPlayersData(), 10);
        scoreboardManager.updateBoard(player, title, lines);
    }

    private List<LeaderboardType> getEnabledTypes() {
        return Arrays.stream(LeaderboardType.values())
                .filter(t -> plugin.getLeaderboardSettings().isLeaderboardEnabled(t.getId()))
                .collect(Collectors.toList());
    }

    private boolean isFlagSet(JSONObject json, String key) {
        if (json == null) return false;
        Object val = json.getOrDefault(key, 0L);
        return val instanceof Number && ((Number) val).intValue() == 1;
    }
}