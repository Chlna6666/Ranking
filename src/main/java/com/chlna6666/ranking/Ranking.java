package com.chlna6666.ranking;

import com.chlna6666.ranking.I18n.I18n;
import com.chlna6666.ranking.updatechecker.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import com.chlna6666.ranking.metrics.Metrics;
import org.json.simple.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class Ranking extends JavaPlugin implements Listener {

    private ConfigManager configManager;
    private I18n i18n;
    private DataManager dataManager;
    private UpdateChecker updateChecker;

    private ScoreboardManager scoreboardManager;
    private Scoreboard sharedScoreboard;

    private final Map<UUID, BukkitRunnable> onlineTimers = new ConcurrentHashMap<>();

    private long SAVE_DELAY_TICKS;
    private long REGULAR_SAVE_INTERVAL_TICKS;

    public static final String BROWN = "\u001B[0;33m";
    public static final String RESET = "\u001B[0m";

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        configManager = new ConfigManager(this);
        dataManager = new DataManager(this);
        logPluginInfo();

        i18n = new I18n(this);
        i18n.copyDefaultLanguageFiles();
        //getLogger().info(i18n.translate("plugin.enabled"));

        if (getConfig().getBoolean("bstats.enabled")) {
            int pluginId = 21233;
            new Metrics(this, pluginId);
        }


        getServer().getPluginManager().registerEvents(this, this);
        registerCommands();

        scoreboardManager = Bukkit.getScoreboardManager();
        sharedScoreboard = scoreboardManager.getNewScoreboard();

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new Papi(this, dataManager).register();
        }

        loadConfigValues();
        startRegularSaveTask();

        updateChecker = new UpdateChecker(this);
        updateChecker.checkForUpdates(null);
    }

    @Override
    public void onDisable() {
        dataManager.saveAllData();
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
            CommandExecutor rankingExecutor = new RankingCommand(this, dataManager);
            rankingCommand.setExecutor(rankingExecutor);
            rankingCommand.setTabCompleter(new RankingTabCompleter());
        } else {
            getLogger().warning("无法获取 /ranking 主命令！");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        handleEvent(event.getPlayer(), "place", dataManager.getPlaceData(), dataManager.getPlaceFile(), "放置榜");
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        handleEvent(event.getPlayer(), "destroys", dataManager.getDestroysData(), dataManager.getDestroysFile(), "挖掘榜");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        handleEvent(event.getEntity(), "deads", dataManager.getDeadsData(), dataManager.getDeadsFile(), "死亡榜");
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            handleEvent(event.getEntity().getKiller().getPlayer(), "mobdie", dataManager.getMobdieData(), dataManager.getMobdieFile(), "击杀榜");
        }
    }

    private void handleEvent(Player player, String dataType, Map<String, Long> data, File file, String sidebarTitle) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        long count = data.getOrDefault(uuid.toString(), 0L);
        data.put(uuid.toString(), count + 1);
        updateScoreboards(player, sidebarTitle, data, dataType);

        dataManager.startSaveTask(new AtomicBoolean(), () -> dataManager.saveJSON((JSONObject) data, file));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String playerName = player.getName();

        if (player.hasPermission("ranking.update.notify") && getConfig().getBoolean("update_checker.notify_on_login")) {
            updateChecker.checkForUpdates(player);
        }

        if (!dataManager.getPlayersData().containsKey(uuid.toString())) {
            JSONObject playerInfo = new JSONObject();
            playerInfo.put("name", playerName);
            dataManager.getPlayersData().put(uuid.toString(), playerInfo);
            dataManager.saveJSONAsync(dataManager.getPlayersData(), dataManager.getDataFile());
        } else {
            JSONObject storedPlayerInfo = (JSONObject) dataManager.getPlayersData().get(uuid.toString());
            String storedName = (String) storedPlayerInfo.get("name");

            if (!storedName.equals(playerName)) {
                storedPlayerInfo.put("name", playerName);
                dataManager.saveJSONAsync(dataManager.getPlayersData(), dataManager.getDataFile());
            }
        }
        clearPlayerRankingObjective(player);
        updatePlayerScoreboards(player, uuid);

        BukkitRunnable timer = createOnlineTimeTimer(player, uuid);
        onlineTimers.put(uuid, timer);
        timer.runTaskTimer(this, 1200, 1200);
    }

    private void updatePlayerScoreboards(Player player, UUID uuid) {
        JSONObject playerData = (JSONObject) dataManager.getPlayersData().getOrDefault(uuid.toString(), new JSONObject());
        checkAndUpdateScoreboard(player, playerData, "place", "放置榜", dataManager.getPlaceData());
        checkAndUpdateScoreboard(player, playerData, "destroys", "挖掘榜", dataManager.getDestroysData());
        checkAndUpdateScoreboard(player, playerData, "deads", "死亡榜", dataManager.getDeadsData());
        checkAndUpdateScoreboard(player, playerData, "mobdie", "击杀榜", dataManager.getMobdieData());
        checkAndUpdateScoreboard(player, playerData, "onlinetime", "时长榜", dataManager.getOnlinetimeData());
    }

    private void checkAndUpdateScoreboard(Player player, JSONObject playerData, String dataType, String sidebarTitle, Map<String, Long> data) {
        Number value = (Number) playerData.getOrDefault(dataType, 0);
        if (value.intValue() == 1) {
            updateScoreboards(player, sidebarTitle, data, dataType);
        }
    }

    private BukkitRunnable createOnlineTimeTimer(Player player, UUID uuid) {
        return new BukkitRunnable() {
            @Override
            public void run() {
                long onlineTime = (long) dataManager.getOnlinetimeData().getOrDefault(uuid.toString(), 0L);
                dataManager.getOnlinetimeData().put(uuid.toString(), onlineTime + 1);
                dataManager.saveJSONAsync(dataManager.getOnlinetimeData(), dataManager.getOnlinetimeFile());

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        updateScoreboards(player, "时长榜", dataManager.getOnlinetimeData(), "onlinetime");
                    }
                }.runTask(Ranking.this);
            }
        };
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        BukkitRunnable timer = onlineTimers.get(uuid);
        if (timer != null) {
            timer.cancel();
            onlineTimers.remove(uuid);
        }
        //clearPlayerRankingObjective(player);
    }

    private void startRegularSaveTask() {
        BukkitRunnable regularSaveTask = new BukkitRunnable() {
            @Override
            public void run() {
                dataManager.saveAllData();
            }
        };
        regularSaveTask.runTaskTimer(this, REGULAR_SAVE_INTERVAL_TICKS, REGULAR_SAVE_INTERVAL_TICKS);
    }

    public void updateScoreboards(Player player, String sidebarTitle, Map<String, Long> data, String dataType) {
        // 获取当前使用 dataType 为 1 的在线玩家
        List<Player> dataTypeOnePlayers = Bukkit.getOnlinePlayers().stream()
                .filter(p -> {
                    JSONObject pPlayerData = (JSONObject) dataManager.getPlayersData().getOrDefault(p.getUniqueId().toString(), new JSONObject());
                    Number pScoreboardConfig = (Number) pPlayerData.getOrDefault(dataType, 0);
                    return pScoreboardConfig.intValue() == 1;
                })
                .collect(Collectors.toList());

        // 遍历所有需要更新记分板的玩家
        for (Player onlinePlayer : dataTypeOnePlayers) {
            Scoreboard scoreboard = onlinePlayer.getScoreboard();
            Objective objective = scoreboard.getObjective("Ranking_" + dataType);

            if (objective == null) {
                objective = scoreboard.registerNewObjective("Ranking_" + dataType, "dummy", sidebarTitle);
                objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            } else if (!objective.getDisplayName().equals(sidebarTitle)) {
                // 如果 objective 已存在但显示名称不正确，则更新显示名称
                objective.setDisplayName(sidebarTitle);
            }

            // 更新每个玩家的数据
            for (Map.Entry<String, Long> entry : data.entrySet()) {
                String uuidString = entry.getKey();
                long rankingData = entry.getValue();
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuidString));
                if (offlinePlayer != null) {
                    String playerName = offlinePlayer.getName();
                    Score score = objective.getScore(playerName);
                    if (score.getScore() != (int) rankingData) {
                        score.setScore((int) rankingData);
                    }
                }
            }

            // 移除不在数据中的玩家
            for (String entry : scoreboard.getEntries()) {
                boolean exists = data.entrySet().stream().anyMatch(e -> {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(e.getKey()));
                    return offlinePlayer != null && offlinePlayer.getName().equals(entry);
                });
                if (!exists) {
                    scoreboard.resetScores(entry);
                }
            }
        }
    }


    private void clearPlayerRankingObjective(Player player) {
        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        Scoreboard newScoreboard = scoreboardManager.getNewScoreboard();  // 创建新的空白记分板
        player.setScoreboard(newScoreboard);  // 将新的空白记分板设置给玩家
    }


    public long getSaveDelayTicks() {
        return SAVE_DELAY_TICKS;
    }

    public I18n getI18n() {
        return i18n;
    }
}
