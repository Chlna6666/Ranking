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
import com.chlna6666.ranking.scoreboard.ScoreboardUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

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

    private String currentVersion;


    private long SAVE_DELAY_TICKS;
    private long REGULAR_SAVE_INTERVAL_TICKS;

    public static final String BROWN = "\u001B[0;33m";
    public static final String RESET = "\u001B[0m";

    @Override
    public void onEnable() {
        i18n = new I18n(this);
        i18n.copyDefaultLanguageFiles();

        if (!getDataFolder().exists()) {
            boolean created = getDataFolder().mkdirs();  // 创建目录及其父目录
            if (created) {
                getLogger().info("Data folder and any necessary parent directories created successfully.");
            } else {
                getLogger().warning("Failed to create data folder or directories.");
            }
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
        logPluginInfo();


        if (getConfig().getBoolean("bstats.enabled")) {
            int pluginId = 21233;
            new Metrics(this, pluginId);
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
        dataManager.saveAllData();
        dataManager.shutdownDataManager();
    }

    private void registerListeners() {
        var pluginManager = getServer().getPluginManager();
        // 初始化 OnlineTime 模块
        OnlineTime onlineTime = new OnlineTime(this, dataManager);
        // 用于后续传递给 BlockPistonRetractListener
        BlockPlaceListener blockPlaceListener = new BlockPlaceListener(this);

        pluginManager.registerEvents(new BlockBreakListener(this), this);
        pluginManager.registerEvents(blockPlaceListener, this);
        pluginManager.registerEvents(new EntityDeathListener(this), this);
        pluginManager.registerEvents(new BlockPistonRetractListener(this, blockPlaceListener), this);
        pluginManager.registerEvents(new PlayerJoinListener(this, onlineTime), this);
        pluginManager.registerEvents(new PlayerQuitListener(this, onlineTime), this);
    }


    private void loadConfigValues() {
        FileConfiguration config = configManager.getConfig();
        SAVE_DELAY_TICKS = config.getLong("data_storage.save_delay");
        REGULAR_SAVE_INTERVAL_TICKS = config.getLong("data_storage.regular_save_interval");
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
            CommandExecutor rankingExecutor = new RankingCommand(this, dataManager, i18n);
            rankingCommand.setExecutor(rankingExecutor);
            rankingCommand.setTabCompleter(new RankingTabCompleter());
        } else {
            getLogger().warning(i18n.translate("warning.cannot_get_main_command"));
        }
    }

    private void startRegularSaveTask() {
        if (isFolia()) {
            getServer().getAsyncScheduler().runAtFixedRate(
                    this,
                    task -> dataManager.saveAllData(),
                    REGULAR_SAVE_INTERVAL_TICKS * 50L,
                    REGULAR_SAVE_INTERVAL_TICKS * 50L,
                    TimeUnit.MILLISECONDS
            );
        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    dataManager.saveAllData();
                }
            }.runTaskTimerAsynchronously(this, REGULAR_SAVE_INTERVAL_TICKS, REGULAR_SAVE_INTERVAL_TICKS);
        }
    }


    /**
     * 处理事件（更新计数、计分板，并异步保存数据）
     */
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
            Bukkit.getGlobalRegionScheduler().run(
                    this,
                    scheduledTask -> task.run()
            );
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(this, task);
        }
    }

    public void updatePlayerScoreboards(UUID uuid) {
        JSONObject playerData = dataManager.getPlayersData().containsKey(uuid.toString()) ?
                (JSONObject) dataManager.getPlayersData().get(uuid.toString()) :
                new JSONObject();
        checkAndUpdateScoreboard(playerData, "place", i18n.translate("sidebar.place"), dataManager.getPlaceData());
        checkAndUpdateScoreboard(playerData, "destroys", i18n.translate("sidebar.break"), dataManager.getDestroysData());
        checkAndUpdateScoreboard(playerData, "deads", i18n.translate("sidebar.death"), dataManager.getDeadsData());
        checkAndUpdateScoreboard(playerData, "mobdie", i18n.translate("sidebar.kill"), dataManager.getMobdieData());
        checkAndUpdateScoreboard(playerData, "onlinetime", i18n.translate("sidebar.online_time"), dataManager.getOnlinetimeData());
        checkAndUpdateScoreboard(playerData, "break_bedrock", i18n.translate("sidebar.break_bedrock"), dataManager.getBreakBedrockData());
    }
    private void checkAndUpdateScoreboard(JSONObject playerData, String dataType, String sidebarTitle, JSONObject data) {
        int value = playerData.containsKey(dataType) ? ((Number) playerData.get(dataType)).intValue() : 0;
        if (value == 1) {
            updateScoreboards(sidebarTitle, data, dataType);
        }
    }

    /**
     * 更新计分板数据
     */
    public void updateScoreboards(String sidebarTitle, Map<String, Long> data, String dataType) {
        Scoreboard scoreboard = ScoreboardUtils.getOrCreateScoreboard(dataType);
        Map<String, String> uuidToNameMap = ScoreboardUtils.getOrCreateUUIDMap(dataType);

        // 创建/更新计分项
        Component title = LegacyComponentSerializer.legacyAmpersand().deserialize(sidebarTitle);
        Objective objective = scoreboard.getObjective("Ranking_" + dataType);
        if (objective == null) {
            objective = scoreboard.registerNewObjective(
                    "Ranking_" + dataType,
                    Criteria.DUMMY,
                    title
            );
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else if (!objective.displayName().equals(title)) {
            objective.displayName(title);
        }

        Set<String> validNames = new HashSet<>();
        JSONObject playersData = dataManager.getPlayersData();

        // 更新各个玩家的分数
        for (Map.Entry<String, Long> entry : data.entrySet()) {
            String uuid = entry.getKey();
            long scoreValue = entry.getValue();
            String currentName = "Unknown";
            if (playersData.containsKey(uuid)) {
                JSONObject playerData = (JSONObject) playersData.get(uuid);
                currentName = (String) playerData.getOrDefault("name", "Unknown");
            }
            validNames.add(currentName);

            String storedName = uuidToNameMap.get(uuid);
            if (storedName != null && !storedName.equals(currentName)) {
                scoreboard.resetScores(storedName);
            }
            uuidToNameMap.put(uuid, currentName);
            Score score = objective.getScore(currentName);
            if (score.getScore() != (int) scoreValue) {
                score.setScore((int) scoreValue);
            }
        }

        // 清理无效的名称
        uuidToNameMap.entrySet().removeIf(entry -> {
            if (!data.containsKey(entry.getKey())) {
                scoreboard.resetScores(entry.getValue());
                return true;
            }
            return false;
        });

        // 清理其他残留条目
        for (String entry : scoreboard.getEntries()) {
            if (objective.getScore(entry).isScoreSet() && !validNames.contains(entry)) {
                scoreboard.resetScores(entry);
            }
        }

        // 更新在线玩家的显示计分板
        Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Player p : Bukkit.getOnlinePlayers()) {
            JSONObject playerData = (JSONObject) playersData.getOrDefault(p.getUniqueId().toString(), new JSONObject());
            boolean shouldShow = ((Number) playerData.getOrDefault(dataType, 0)).intValue() == 1;
            if (shouldShow && !p.getScoreboard().equals(scoreboard)) {
                p.setScoreboard(scoreboard);
            } else if (!shouldShow && p.getScoreboard().equals(scoreboard)) {
                p.setScoreboard(mainScoreboard);
            }
        }
    }


    public DataManager getDataManager() {
        return dataManager;
    }

    public I18n getI18n() {
        return i18n;
    }

    public LeaderboardSettings getLeaderboardSettings() {
        return leaderboardSettings;
    }

    public String getCurrentVersion() {return currentVersion;}

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    public long getSAVE_DELAY_TICKS() {
        return SAVE_DELAY_TICKS;
    }
}
