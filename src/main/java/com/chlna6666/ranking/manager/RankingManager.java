package com.chlna6666.ranking.manager;

import com.chlna6666.ranking.Ranking;
import com.chlna6666.ranking.datamanager.DataManager;
import com.chlna6666.ranking.enums.LeaderboardType;
import com.chlna6666.ranking.scoreboard.DynamicScoreboard;
import com.chlna6666.ranking.scoreboard.ScoreboardManager;
import com.chlna6666.ranking.scoreboard.ScoreboardUtils;
import com.chlna6666.ranking.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class RankingManager {
    private final Ranking plugin;
    private final DataManager dataManager;
    private final ScoreboardManager scoreboardManager;
    private final DynamicScoreboard dynamicScoreboard;

    // 优化：使用 Set 记录哪些榜单数据发生了变化（变脏了），需要刷新显示
    private final Set<LeaderboardType> dirtyTypes = ConcurrentHashMap.newKeySet();

    public RankingManager(Ranking plugin, DataManager dataManager, ScoreboardManager scoreboardManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.scoreboardManager = scoreboardManager;
        this.dynamicScoreboard = new DynamicScoreboard(plugin, dataManager, scoreboardManager);

        // 启动定时刷新任务
        startUpdateTask();
    }

    /**
     * 启动定时刷新任务 (缓冲机制)
     * 每 20 tick (1秒) 检查一次是否有榜单需要更新
     * 避免了每次挖掘方块都重排数据和发包的性能浪费
     */
    private void startUpdateTask() {
        Consumer<Object> task = (t) -> {
            if (dirtyTypes.isEmpty()) return;

            // 遍历所有标记为"脏"的榜单进行刷新
            for (LeaderboardType type : dirtyTypes) {
                updateScoreboards(type);
            }
            dirtyTypes.clear();
        };

        if (Utils.isFolia()) {
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, t -> task.accept(t), 20L, 20L);
        } else {
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> task.accept(null), 20L, 20L);
        }
    }

    /**
     * 统一事件处理入口
     * 优化后：仅更新内存数据，不再进行 IO 操作和立即刷新
     */
    public void handleEvent(Player player, LeaderboardType type) {
        if (player == null) return;

        runAsync(() -> {
            JSONObject data = dataManager.getData(type);

            // 优化：添加同步锁，防止多线程并发修改导致数据异常
            synchronized (data) {
                String uuid = player.getUniqueId().toString();
                long count = data.containsKey(uuid) ? parseLong(data.get(uuid)) : 0L;
                data.put(uuid, count + 1);
            }

            dirtyTypes.add(type);
        });
    }

    /**
     * 玩家开关排行榜
     */
    public void toggleScoreboard(Player player, LeaderboardType type) {
        updatePlayerStatus(player, type.getId());

        if (isScoreboardEnabledFor(player, type.getId())) {
            player.sendMessage(ScoreboardUtils.getTitle(plugin.getI18n(), type) + " " + plugin.getI18n().translate("command.enabled"));
            // 玩家主动开启时，立即计算一次给他看
            updateScoreboards(type);
        } else {
            player.sendMessage(ScoreboardUtils.getTitle(plugin.getI18n(), type) + " " + plugin.getI18n().translate("command.disabled"));
            scoreboardManager.removeBoard(player);
        }
    }

    /**
     * 开关动态排行榜
     */
    public void toggleDynamic(Player player) {
        dynamicScoreboard.toggle(player);
    }

    /**
     * 刷新所有榜单显示
     */
    public void refreshAll() {
        for (LeaderboardType type : LeaderboardType.values()) {
            updateScoreboards(type);
        }
    }

    public void shutdown() {
        dynamicScoreboard.shutdown();
        scoreboardManager.removeAll();
    }

    // --- 核心逻辑 ---

    /**
     * 计算并广播指定类型的排行榜更新
     */
    public void updateScoreboards(LeaderboardType type) {
        runAsync(() -> {
            // 计算数据 (耗时操作)
            JSONObject data = dataManager.getData(type);
            JSONObject playerData = dataManager.getPlayersData();
            int topN = plugin.getLeaderboardSettings().getTopN();
            String title = ScoreboardUtils.getTitle(plugin.getI18n(), type);

            // 注意：formatLines 内部进行了排序，这是最消耗 CPU 的步骤
            // 现在它只会被定时任务调用，大大降低了频率
            List<String> lines = ScoreboardUtils.formatLines(data, playerData, topN);

            // 广播 (回到主线程/全局线程发送数据包)
            runGlobal(() -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (isScoreboardEnabledFor(player, type.getId())) {
                        scoreboardManager.updateBoard(player, title, lines);
                    }
                }
            });
        });
    }

    // 玩家加入时恢复显示
    public void restorePlayerScoreboard(Player player) {
        scoreboardManager.removeBoard(player); // 清理旧的

        JSONObject pData = getPlayerData(player);
        if (pData == null) return;

        // 检查普通榜单
        for (LeaderboardType type : LeaderboardType.values()) {
            if (isFlagSet(pData, type.getId())) {
                updateScoreboards(type);
                return;
            }
        }
    }

    private void updatePlayerStatus(Player player, String targetKey) {
        JSONObject pData = getPlayerData(player);
        if (pData == null) return;

        boolean wasEnabled = isFlagSet(pData, targetKey);

        // 重置所有
        for (LeaderboardType type : LeaderboardType.values()) {
            pData.put(type.getId(), 0L);
        }
        pData.put("dynamic", 0L);

        if (!wasEnabled) {
            pData.put(targetKey, 1L);
        }
        // 这里是配置更改，频率低，可以立即保存
        dataManager.saveData("data", dataManager.getPlayersData());
    }

    private boolean isScoreboardEnabledFor(Player player, String key) {
        return isFlagSet(getPlayerData(player), key);
    }

    private JSONObject getPlayerData(Player player) {
        JSONObject all = dataManager.getPlayersData();
        return (JSONObject) all.get(player.getUniqueId().toString());
    }

    private boolean isFlagSet(JSONObject json, String key) {
        if (json == null) return false;
        Object val = json.getOrDefault(key, 0L);
        return val instanceof Number && ((Number) val).intValue() == 1;
    }

    private long parseLong(Object obj) {
        if (obj instanceof Number) return ((Number) obj).longValue();
        return 0L;
    }

    // --- 调度工具 ---
    private void runAsync(Runnable task) {
        if (Utils.isFolia()) Bukkit.getAsyncScheduler().runNow(plugin, t -> task.run());
        else Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    private void runGlobal(Runnable task) {
        if (Utils.isFolia()) Bukkit.getGlobalRegionScheduler().run(plugin, t -> task.run());
        else Bukkit.getScheduler().runTask(plugin, task);
    }
}