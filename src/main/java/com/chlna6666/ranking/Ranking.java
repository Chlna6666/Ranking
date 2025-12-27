package com.chlna6666.ranking;

import com.chlna6666.ranking.config.ConfigManager;
import com.chlna6666.ranking.datamanager.DataManager;
import com.chlna6666.ranking.datamanager.JsonDataManager;
import com.chlna6666.ranking.datamanager.MySQLDataManager;
import com.chlna6666.ranking.I18n.I18n;
import com.chlna6666.ranking.listener.*;
import com.chlna6666.ranking.papi.PlaceholderAPI;
import com.chlna6666.ranking.command.RankingCommand;
import com.chlna6666.ranking.command.RankingTabCompleter;
import com.chlna6666.ranking.leaderboard.LeaderboardSettings;
import com.chlna6666.ranking.statistics.OnlineTime;
import com.chlna6666.ranking.updatechecker.UpdateChecker;
import com.chlna6666.ranking.metrics.Metrics;
import com.chlna6666.ranking.scoreboard.ScoreboardManager;
import com.chlna6666.ranking.scoreboard.DynamicScoreboard;
import com.chlna6666.ranking.scoreboard.ScoreboardUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import org.json.simple.JSONObject;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.chlna6666.ranking.utils.Utils.isFolia;

public class Ranking extends JavaPlugin {

    private I18n i18n;
    private ConfigManager configManager;
    private DataManager dataManager;
    private LeaderboardSettings leaderboardSettings;
    private UpdateChecker updateChecker;
    private ScoreboardManager scoreboardManager; // 新增单例

    private String currentVersion;
    private int leaderboardTopN;

    private long SAVE_DELAY_TICKS;
    private long REGULAR_SAVE_INTERVAL_TICKS;

    public static final String BROWN = "\u001B[0;33m";
    public static final String RESET = "\u001B[0m";

    @Override
    public void onEnable() {
        i18n = new I18n(this);
        i18n.copyDefaultLanguageFiles();

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        configManager = new ConfigManager(this);
        String storageMethod = getConfig().getString("data_storage.method", "json");
        if ("mysql".equalsIgnoreCase(storageMethod)) {
            dataManager = new MySQLDataManager(this);
        } else {
            dataManager = new JsonDataManager(this);
        }

        leaderboardSettings = LeaderboardSettings.getInstance();
        leaderboardSettings.loadSettings(configManager);

        // 初始化 ScoreboardManager
        scoreboardManager = new ScoreboardManager(this, dataManager, i18n);

        logPluginInfo();

        if (getConfig().getBoolean("bstats.enabled")) {
            new Metrics(this, 21233);
        }

        registerListeners();
        registerCommands();

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderAPI(this, dataManager, i18n).register();
        }

        loadConfigValues();
        startRegularSaveTask();

        currentVersion = getDescription().getVersion();
        updateChecker = new UpdateChecker(this);
        updateChecker.checkForUpdates(null);
    }

    @Override
    public void onDisable() {
        OnlineTime.shutdownScheduler();
        DynamicScoreboard.shutdown();
        dataManager.saveAllData();
        dataManager.shutdownDataManager();

        // 插件关闭时清理所有 FastBoard
        if (scoreboardManager != null) {
            for(Player p : Bukkit.getOnlinePlayers()) {
                scoreboardManager.removeBoard(p);
            }
        }
    }

    private void registerListeners() {
        var pluginManager = getServer().getPluginManager();
        OnlineTime onlineTime = new OnlineTime(this, dataManager);
        BlockPlaceListener blockPlaceListener = new BlockPlaceListener(this);

        pluginManager.registerEvents(new BlockBreakListener(this), this);
        pluginManager.registerEvents(blockPlaceListener, this);
        pluginManager.registerEvents(new PlayerDeathListener(this), this);
        pluginManager.registerEvents(new EntityDeathListener(this), this);
        pluginManager.registerEvents(new BlockPistonRetractListener(this, blockPlaceListener), this);
        pluginManager.registerEvents(new PlayerJoinListener(this, onlineTime), this);
        pluginManager.registerEvents(new PlayerQuitListener(this, onlineTime), this);
    }

    private void loadConfigValues() {
        FileConfiguration config = configManager.getConfig();
        SAVE_DELAY_TICKS = config.getLong("data_storage.save_delay");
        REGULAR_SAVE_INTERVAL_TICKS = config.getLong("data_storage.regular_save_interval");
        leaderboardTopN = config.getInt("leaderboards.top_n", 10);
    }

    private void logPluginInfo() {
        Bukkit.getLogger().info("");
        Bukkit.getLogger().info(BROWN + "██████╗  █████╗ ███╗   ██╗██╗  ██╗██╗███╗   ██╗ ██████╗ " + RESET);
        Bukkit.getLogger().info(BROWN + "██╔══██╗██╔══██╗████╗  ██║██║ ██╔╝██║████╗  ██║██╔════╝ " + RESET);
        Bukkit.getLogger().info(BROWN + "██████╔╝███████║██╔██╗ ██║█████╔╝ ██║██╔██╗ ██║██║  ███╗" + RESET);
        Bukkit.getLogger().info(BROWN + "██╔══██╗██╔══██║██║╚██╗██║██╔═██╗ ██║██║╚██╗██║██║   ██║" + RESET);
        Bukkit.getLogger().info(BROWN + "██║  ██║██║  ██║██║ ╚████║██║  ██╗██║██║ ╚████║╚██████╔╝" + RESET);
        Bukkit.getLogger().info(BROWN + "╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═══╝╚═╝  ╚═╝╚═╝╚═╝  ╚═══╝ ╚═════╝" + RESET);
        Bukkit.getLogger().info("");
    }

    private void registerCommands() {
        PluginCommand rankingCommand = getCommand("ranking");
        if (rankingCommand != null) {
            // 使用 plugin 实例中的 manager
            CommandExecutor rankingExecutor = new RankingCommand(this, dataManager, i18n);
            rankingCommand.setExecutor(rankingExecutor);
            rankingCommand.setTabCompleter(new RankingTabCompleter());
        } else {
            getLogger().warning(i18n.translate("warning.cannot_get_main_command"));
        }
    }

    private void startRegularSaveTask() {
        if (isFolia()) {
            getServer().getAsyncScheduler().runAtFixedRate(this, task -> dataManager.saveAllData(),
                    REGULAR_SAVE_INTERVAL_TICKS * 50L, REGULAR_SAVE_INTERVAL_TICKS * 50L, TimeUnit.MILLISECONDS);
        } else {
            new BukkitRunnable() {
                @Override public void run() { dataManager.saveAllData(); }
            }.runTaskTimerAsynchronously(this, REGULAR_SAVE_INTERVAL_TICKS, REGULAR_SAVE_INTERVAL_TICKS);
        }
    }

    public void handleEvent(Player player, String dataType, JSONObject data, String sidebarTitle) {
        if (player == null) return;
        String uuidString = player.getUniqueId().toString();
        long count = data.containsKey(uuidString) ? (Long) data.get(uuidString) : 0L;
        data.put(uuidString, count + 1);
        updateScoreboards(sidebarTitle, data, dataType);
        runSaveTaskAsync(() -> dataManager.saveData(dataType, data));
    }

    public void runSaveTaskAsync(Runnable task) {
        if (isFolia()) {
            Bukkit.getGlobalRegionScheduler().run(this, scheduledTask -> task.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(this, task);
        }
    }

    public void updatePlayerScoreboards(UUID uuid) {
        // 玩家加入时，检查是否有开启的榜单，触发更新
        JSONObject playerData = (JSONObject) dataManager.getPlayersData().get(uuid.toString());
        if (playerData == null) return;

        for (String type : DataManager.SUPPORTED_TYPES) {
            Object value = playerData.get(type);
            if (value instanceof Number && ((Number) value).intValue() == 1) {
                updateScoreboards(ScoreboardUtils.getTitle(i18n, type), ScoreboardUtils.getData(dataManager, type), type);
            }
        }
    }

    /**
     * 核心更新方法：计算排行数据 -> 调用 Manager 广播
     */
    public void updateScoreboards(String sidebarTitle, Map<String, Long> data, String dataType) {
        // 1. 异步计算排名行
        Runnable calculateTask = () -> {
            JSONObject playersData = dataManager.getPlayersData();
            List<String> lines = ScoreboardUtils.formatLines(data, playersData, leaderboardTopN);

            // 2. 调度到主线程/全局线程应用更新
            Runnable applyTask = () -> {
                scoreboardManager.broadcastUpdate(dataType, sidebarTitle, lines);
            };

            if (isFolia()) {
                Bukkit.getGlobalRegionScheduler().run(this, task -> applyTask.run());
            } else {
                Bukkit.getScheduler().runTask(this, applyTask);
            }
        };

        if (isFolia()) {
            Bukkit.getAsyncScheduler().runNow(this, task -> calculateTask.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(this, calculateTask);
        }
    }

    public DataManager getDataManager() { return dataManager; }
    public I18n getI18n() { return i18n; }
    public LeaderboardSettings getLeaderboardSettings() { return leaderboardSettings; }
    public String getCurrentVersion() { return currentVersion; }
    public UpdateChecker getUpdateChecker() { return updateChecker; }
    // 新增 Getter
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
}