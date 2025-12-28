package com.chlna6666.ranking.statistics;

import com.chlna6666.ranking.Ranking;
import com.chlna6666.ranking.enums.LeaderboardType;
import com.chlna6666.ranking.manager.RankingManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

public class OnlineTime {
    private final RankingManager rankingManager;
    private final Map<UUID, ScheduledFuture<?>> onlineTimers = new ConcurrentHashMap<>();
    private final Map<UUID, Player> playerCache = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(8);

    public OnlineTime(Ranking plugin, RankingManager rankingManager) {
        this.rankingManager = rankingManager;
    }

    public void scheduleOnlineTimeTask(UUID uuid, long delay, long period) {
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(
                createTask(uuid), delay, period, TimeUnit.SECONDS);
        onlineTimers.put(uuid, task);
    }

    public void cancelOnlineTimeTask(UUID uuid) {
        ScheduledFuture<?> task = onlineTimers.remove(uuid);
        if (task != null) task.cancel(true);
        playerCache.remove(uuid);
    }

    private Runnable createTask(UUID uuid) {
        return () -> {
            Player player = playerCache.computeIfAbsent(uuid, Bukkit::getPlayer);
            if (player != null && player.isOnline()) {
                rankingManager.handleEvent(player, LeaderboardType.ONLINE_TIME);
            }
        };
    }

    public static void shutdownScheduler() {
        scheduler.shutdownNow();
    }
}