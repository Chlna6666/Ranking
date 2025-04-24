package com.chlna6666.ranking.scoreboard;

import com.chlna6666.ranking.datamanager.DataManager;
import com.chlna6666.ranking.I18n.I18n;
import com.chlna6666.ranking.Ranking;
import com.chlna6666.ranking.utils.Utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONObject;

import java.util.*;


public class ScoreboardManager {
    private final Ranking plugin;
    private final DataManager dataManager;
    private final I18n i18n;
    private final Map<UUID, BukkitRunnable> dynamicTasks = new HashMap<>();

    private final List<String> dynamicKeys = Arrays.asList(
            "place", "destroys", "deads", "mobdie", "onlinetime", "break_bedrock"
    );

    public ScoreboardManager(Ranking plugin, DataManager dataManager, I18n i18n) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.i18n = i18n;
    }

    /**
     * 根据 key 选择对应的 sidebarTitle 和 数据，然后调用 plugin.updateScoreboards(...)
     */
    private void updateDynamicScoreboard(Player player, String key) {
        String title;
        JSONObject data;

        switch (key) {
            case "place":
                title = i18n.translate("sidebar.place");
                data = dataManager.getPlaceData();
                break;
            case "destroys":
                title = i18n.translate("sidebar.break");
                data = dataManager.getDestroysData();
                break;
            case "deads":
                title = i18n.translate("sidebar.death");
                data = dataManager.getDeadsData();
                break;
            case "mobdie":
                title = i18n.translate("sidebar.kill");
                data = dataManager.getMobdieData();
                break;
            case "onlinetime":
                title = i18n.translate("sidebar.online_time");
                data = dataManager.getOnlinetimeData();
                break;
            case "break_bedrock":
                title = i18n.translate("sidebar.break_bedrock");
                data = dataManager.getBreakBedrockData();
                break;
            default:
                // 不认识的 key，直接返回
                return;
        }

        plugin.updateScoreboards(title, data, key);
    }

    /**
     * 动态记分板：开启时立即执行一次并每隔 5 分钟轮换一次；关闭时停止任务并重置所有 key。
     */
    public void dynamicScoreboard(Player player, JSONObject playersData) {
        UUID uuid = player.getUniqueId();
        JSONObject playerData = (JSONObject) playersData.get(uuid.toString());
        if (playerData == null) return;

        // 1. 切换 dynamic 标记（0 ↔ 1）
        updateScoreboardStatus(player, "dynamic");
        int status = getPlayerScoreboardStatus(player, "dynamic");

        // 准备提示文字（可以根据你的 i18n key 调整）
        String prefix = i18n.translate("sidebar.dynamic") + " ";  // 例如 "[动态] "
        String msgEnabled  = prefix + i18n.translate("command.enabled");
        String msgDisabled = prefix + i18n.translate("command.disabled");

        // 2. 根据状态发消息
        if (status == 1) {
            player.sendMessage(msgEnabled);
        } else {
            player.sendMessage(msgDisabled);
        }

        // 3. 如果关闭：取消任务 + 重置所有 key + 清空记分板
        if (status == 0) {
            if (dynamicTasks.containsKey(uuid)) {
                dynamicTasks.get(uuid).cancel();
                dynamicTasks.remove(uuid);
            }
            for (String key : dynamicKeys) {
                playerData.put(key, 0L);
            }
            dataManager.saveData("data", playersData);
            ScoreboardUtils.clearScoreboard(player);
            return;
        }

        // 4. 如果开启：先取消可能存在的旧任务，再启动新任务
        if (dynamicTasks.containsKey(uuid)) {
            dynamicTasks.get(uuid).cancel();
        }

        BukkitRunnable task = new BukkitRunnable() {
            private int index = 0;

            @Override
            public void run() {
                // 重置所有动态 key
                for (String key : dynamicKeys) {
                    playerData.put(key, 0L);
                }
                // 当前要展示的 key
                String currentKey = dynamicKeys.get(index);
                playerData.put(currentKey, 1L);
                dataManager.saveData("data", playersData);

                // 根据环境调度真正的记分板更新
                if (Utils.isFolia()) {
                    Bukkit.getRegionScheduler().run(plugin, player.getLocation(), sched ->
                            updateDynamicScoreboard(player, currentKey)
                    );
                } else {
                    updateDynamicScoreboard(player, currentKey);
                }

                index = (index + 1) % dynamicKeys.size();
            }
        };

        // “立即执行一次 + 每 6000 tick（5 分钟）执行一次”
        task.runTaskTimer(plugin, 0L, 6000L);
        dynamicTasks.put(uuid, task);
    }





    public void toggleScoreboard(Player player, String rankingName, String displayName, JSONObject rankingData) {
        updateScoreboardStatus(player, rankingName);
        int scoreboardStatus = getPlayerScoreboardStatus(player, rankingName);
        if (scoreboardStatus == 1) {
            ScoreboardUtils.clearScoreboard(player);
            player.sendMessage(displayName + i18n.translate("command.enabled"));
            if (Utils.isFolia()) {
                Bukkit.getRegionScheduler().run(plugin, player.getLocation(), scheduledTask ->
                        plugin.updateScoreboards(displayName, rankingData, rankingName));
            } else {
                plugin.updateScoreboards(displayName, rankingData, rankingName);
            }
        } else {
            player.sendMessage(displayName + i18n.translate("command.disabled"));
            ScoreboardUtils.clearScoreboard(player);
        }
    }

    private void updateScoreboardStatus(Player player, String rankingValue) {
        List<String> specificKeys = Arrays.asList("place", "destroys", "deads", "mobdie", "onlinetime", "break_bedrock", "dynamic");
        UUID uuid = player.getUniqueId();
        JSONObject playersData = dataManager.getPlayersData();
        JSONObject playerData = (JSONObject) playersData.get(uuid.toString());
        if (playerData != null) {
            for (String key : specificKeys) {
                if (!key.equals(rankingValue)) {
                    playerData.put(key, 0L);
                }
            }
            if (specificKeys.contains(rankingValue)) {
                long currentValue = playerData.containsKey(rankingValue) ? (Long) playerData.get(rankingValue) : 0;
                long newStatus = (currentValue == 0) ? 1 : 0;
                playerData.put(rankingValue, newStatus);
            }
            dataManager.saveData("data", playersData);
        }
    }

    private int getPlayerScoreboardStatus(Player player, String rankingValue) {
        JSONObject playerData = getPlayerData(player);
        if (playerData != null && playerData.containsKey(rankingValue)) {
            Object value = playerData.get(rankingValue);
            if (value instanceof Long) {
                return ((Long) value).intValue();
            } else if (value instanceof Integer) {
                return (Integer) value;
            }
        }
        return 0;
    }

    private JSONObject getPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        JSONObject playersData = dataManager.getPlayersData();
        return (JSONObject) playersData.get(uuid.toString());
    }
}
