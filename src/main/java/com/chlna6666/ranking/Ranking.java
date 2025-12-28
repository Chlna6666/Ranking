package com.chlna6666.ranking;

import com.chlna6666.ranking.command.RankingCommand;
import com.chlna6666.ranking.command.RankingTabCompleter;
import com.chlna6666.ranking.config.ConfigManager;
import com.chlna6666.ranking.datamanager.DataManager;
import com.chlna6666.ranking.datamanager.JsonDataManager;
import com.chlna6666.ranking.datamanager.MySQLDataManager;
import com.chlna6666.ranking.I18n.I18n;
import com.chlna6666.ranking.leaderboard.LeaderboardSettings;
import com.chlna6666.ranking.listener.*;
import com.chlna6666.ranking.manager.RankingManager;
import com.chlna6666.ranking.metrics.Metrics;
import com.chlna6666.ranking.papi.PlaceholderAPI;
import com.chlna6666.ranking.scoreboard.ScoreboardManager;
import com.chlna6666.ranking.statistics.OnlineTime;
import com.chlna6666.ranking.updatechecker.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.TimeUnit;

import static com.chlna6666.ranking.utils.Utils.isFolia;

public class Ranking extends JavaPlugin {

    private I18n i18n;
    private ConfigManager configManager;
    private DataManager dataManager;
    private LeaderboardSettings leaderboardSettings;
    private UpdateChecker updateChecker;
    private ScoreboardManager scoreboardManager;
    private RankingManager rankingManager;

    private String currentVersion;
    public static final String BROWN = "\u001B[0;33m";
    public static final String RESET = "\u001B[0m";

    @Override
    public void onEnable() {
        i18n = new I18n(this);

        if (!getDataFolder().exists()) getDataFolder().mkdirs();

        configManager = new ConfigManager(this);
        String storageMethod = getConfig().getString("data_storage.method", "json");
        if ("mysql".equalsIgnoreCase(storageMethod)) {
            dataManager = new MySQLDataManager(this);
        } else {
            dataManager = new JsonDataManager(this);
        }

        leaderboardSettings = LeaderboardSettings.getInstance();
        leaderboardSettings.loadSettings(configManager);

        // 初始化 Manager
        scoreboardManager = new ScoreboardManager(this);
        rankingManager = new RankingManager(this, dataManager, scoreboardManager);

        logPluginInfo();

        if (getConfig().getBoolean("bstats.enabled")) {
            new Metrics(this, 21233);
        }

        registerListeners();
        registerCommands();

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderAPI(this, dataManager, i18n).register();
        }

        startRegularSaveTask();

        currentVersion = getDescription().getVersion();
        updateChecker = new UpdateChecker(this);
        updateChecker.checkForUpdates(null);
    }

    @Override
    public void onDisable() {
        if (rankingManager != null) rankingManager.shutdown();
        OnlineTime.shutdownScheduler();
        if (dataManager != null) {
            dataManager.saveAllData();
            dataManager.shutdownDataManager();
        }
    }

    private void registerListeners() {
        var pm = getServer().getPluginManager();
        OnlineTime onlineTime = new OnlineTime(this, rankingManager);
        BlockPlaceListener blockPlaceListener = new BlockPlaceListener(this);

        pm.registerEvents(new BlockBreakListener(this), this);
        pm.registerEvents(blockPlaceListener, this);
        pm.registerEvents(new PlayerDeathListener(this), this);
        pm.registerEvents(new EntityDeathListener(this), this);
        pm.registerEvents(new BlockPistonRetractListener(this, blockPlaceListener), this);
        pm.registerEvents(new PlayerJoinListener(this, onlineTime), this);
        pm.registerEvents(new PlayerQuitListener(this, onlineTime), this);
    }

    private void registerCommands() {
        PluginCommand cmd = getCommand("ranking");
        if (cmd != null) {
            cmd.setExecutor(new RankingCommand(this, dataManager, i18n, rankingManager));
            cmd.setTabCompleter(new RankingTabCompleter());
        }
    }

    private void startRegularSaveTask() {
        long interval = configManager.getConfig().getLong("data_storage.regular_save_interval", 1200);
        if (isFolia()) {
            getServer().getAsyncScheduler().runAtFixedRate(this, t -> dataManager.saveAllData(),
                    interval * 50L, interval * 50L, TimeUnit.MILLISECONDS);
        } else {
            new BukkitRunnable() {
                @Override public void run() { dataManager.saveAllData(); }
            }.runTaskTimerAsynchronously(this, interval, interval);
        }
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

    public DataManager getDataManager() { return dataManager; }
    public I18n getI18n() { return i18n; }
    public LeaderboardSettings getLeaderboardSettings() { return leaderboardSettings; }
    public String getCurrentVersion() { return currentVersion; }
    public UpdateChecker getUpdateChecker() { return updateChecker; }
    public RankingManager getRankingManager() { return rankingManager; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
}