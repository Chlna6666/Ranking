package com.chlna6666.ranking.statistics;

import com.chlna6666.ranking.Ranking;
import com.chlna6666.ranking.datamanager.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

public class OnlineTime {
    private final Ranking plugin;
    private final Map<UUID, ScheduledFuture<?>> onlineTimers = new ConcurrentHashMap<>();
    private final Map<UUID, Player> playerCache = new ConcurrentHashMap<>(); // 玩家缓存
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(8);

    public OnlineTime(Ranking plugin, DataManager dataManager) {
        this.plugin = plugin;
    }

    /**
     * 统一调度在线时间任务（高性能异步模式）
     *
     * @param uuid   玩家 UUID
     * @param delay  延迟（秒）
     * @param period 周期（秒）
     */
    public void scheduleOnlineTimeTask(UUID uuid, long delay, long period) {
        ScheduledFuture<?> scheduledTask = scheduler.scheduleAtFixedRate(
                createOnlineTimeTask(uuid), delay, period, TimeUnit.SECONDS);
        onlineTimers.put(uuid, scheduledTask);
    }

    /**
     * 取消指定玩家的在线时间任务
     *
     * @param uuid 玩家 UUID
     */
    public void cancelOnlineTimeTask(UUID uuid) {
        ScheduledFuture<?> task = onlineTimers.remove(uuid);
        if (task != null) {
            task.cancel(true);
        }
        playerCache.remove(uuid); // 移除缓存
    }

    /**
     * 创建在线时间的定时任务
     *
     * @param uuid 玩家 UUID
     * @return 一个 Runnable，每次运行时更新在线时间数据和计分板
     */
    private Runnable createOnlineTimeTask(UUID uuid) {
        return () -> {
            // 异步更新计分板
            CompletableFuture.runAsync(() -> {
                // 获取玩家对象
                Player player = playerCache.computeIfAbsent(uuid, Bukkit::getPlayer);
                if (player != null && player.isOnline()) {
                    // 主线程更新计分板
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            plugin.handleEvent(player, "onlinetime", plugin.getDataManager().getOnlinetimeData(),
                                    plugin.getI18n().translate("sidebar.online_time"));
                        }
                    }.runTask(plugin);
                }
            });
        };
    }

    /**
     * 关闭调度器并取消所有在线时间任务
     */
    public static void shutdownScheduler() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}