package com.chlna6666.ranking;

import com.chlna6666.ranking.I18n.I18n;
import com.chlna6666.ranking.updatechecker.UpdateChecker;
import com.chlna6666.ranking.LeaderboardSettings;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
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
    private LeaderboardSettings leaderboardSettings;
    private UpdateChecker updateChecker;

    private final Map<UUID, BukkitRunnable> onlineTimers = new ConcurrentHashMap<>();
    private final Map<World, Map<Location, Player>> pistonCache = new HashMap<>();

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
        leaderboardSettings = LeaderboardSettings.getInstance();
        leaderboardSettings.loadSettings(configManager);
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

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new Papi(this, dataManager, i18n).register();
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
            CommandExecutor rankingExecutor = new RankingCommand(this, dataManager, i18n);
            rankingCommand.setExecutor(rankingExecutor);
            rankingCommand.setTabCompleter(new RankingTabCompleter());
        } else {
            getLogger().warning(i18n.translate("warning.cannot_get_main_command"));
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (leaderboardSettings.isLeaderboardEnabled("place")) {
            handleEvent(player, "place", dataManager.getPlaceData(), dataManager.getPlaceFile(), i18n.translate("sidebar.place"));
        }

        if (leaderboardSettings.isLeaderboardEnabled("break_bedrock")) {
            // 记录放置的活塞及其方向
            if (block.getType() == Material.PISTON || block.getType() == Material.STICKY_PISTON) {
                Directional directional = (Directional) block.getBlockData();
                BlockFace pistonFacing = directional.getFacing();
                Location bedrockPos = block.getRelative(pistonFacing).getLocation();
                if (block.getWorld().getBlockAt(bedrockPos).getType() == Material.BEDROCK) {
                    pistonCache.computeIfAbsent(block.getWorld(), k -> new HashMap<>()).put(bedrockPos, player);
                    //getLogger().info("活塞由 " + player.getName() + " 放置，朝向基岩，位置：" + bedrockPos);
                }
            }
        }
    }

    @EventHandler
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
        if (leaderboardSettings.isLeaderboardEnabled("break_bedrock")) {
            Block piston = event.getBlock();
            Directional directional = (Directional) piston.getBlockData();
            BlockFace pistonFacing = directional.getFacing();
            Location bedrockPos = piston.getRelative(pistonFacing).getLocation();

            // 检查基岩是否消失
            World world = piston.getWorld();
            Map<Location, Player> map = pistonCache.get(world);
            if (map != null) {
                if (piston.getWorld().getBlockAt(bedrockPos).getType() == Material.BEDROCK) {
                    Player player = map.remove(bedrockPos);
                    if (player != null) {
                        handleEvent(player, "break_bedrock", dataManager.getBreakBedrockData(), dataManager.getBreakBedrockFile(), i18n.translate("sidebar.break_bedrock"));
                        //getLogger().info("基岩被活塞收缩事件破坏，由 " + player.getName() + " 触发，位置：" + bedrockPos);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (leaderboardSettings.isLeaderboardEnabled("deads")) {
            handleEvent(event.getPlayer(), "destroys", dataManager.getDeadsData(), dataManager.getDeadsFile(), i18n.translate("sidebar.break"));
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (leaderboardSettings.isLeaderboardEnabled("deads")) {
            handleEvent(event.getEntity(), "deads", dataManager.getDeadsData(), dataManager.getDeadsFile(), i18n.translate("sidebar.death"));
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (leaderboardSettings.isLeaderboardEnabled("mobdie")) {
            if (event.getEntity().getKiller() != null) {
                handleEvent(event.getEntity().getKiller().getPlayer(), "mobdie", dataManager.getMobdieData(), dataManager.getMobdieFile(), i18n.translate("sidebar.kill"));
            }
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

        if (leaderboardSettings.isLeaderboardEnabled("onlinetime")) {
            BukkitRunnable timer = createOnlineTimeTimer(player, uuid);
            onlineTimers.put(uuid, timer);
            timer.runTaskTimer(this, 1200, 1200);
        }
    }

    private void updatePlayerScoreboards(Player player, UUID uuid) {
        JSONObject playerData = (JSONObject) dataManager.getPlayersData().getOrDefault(uuid.toString(), new JSONObject());
        checkAndUpdateScoreboard(player, playerData, "place", i18n.translate("sidebar.place"), dataManager.getPlaceData());
        checkAndUpdateScoreboard(player, playerData, "destroys", i18n.translate("sidebar.break"), dataManager.getDestroysData());
        checkAndUpdateScoreboard(player, playerData, "deads", i18n.translate("sidebar.death"), dataManager.getDeadsData());
        checkAndUpdateScoreboard(player, playerData, "mobdie", i18n.translate("sidebar.kill"), dataManager.getMobdieData());
        checkAndUpdateScoreboard(player, playerData, "onlinetime", i18n.translate("sidebar.online_time"), dataManager.getOnlinetimeData());
        checkAndUpdateScoreboard(player, playerData, "break_bedrock", i18n.translate("sidebar.break_bedrock"), dataManager.getBreakBedrockData());
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
                        updateScoreboards(player, i18n.translate("sidebar.online_time"), dataManager.getOnlinetimeData(), "onlinetime");
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

            Map<String, Integer> currentScores = new HashMap<>();
            for (String entry : scoreboard.getEntries()) {
                currentScores.put(entry, objective.getScore(entry).getScore());
            }

            // 更新每个玩家的数据
            for (Map.Entry<String, Long> entry : data.entrySet()) {
                String uuidString = entry.getKey();
                int rankingData = entry.getValue().intValue();
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuidString));
                if (offlinePlayer != null) {
                    String playerName = offlinePlayer.getName();
                    Integer currentScore = currentScores.get(playerName);
                    if (currentScore == null || currentScore != rankingData) {
                        Score score = objective.getScore(playerName);
                        score.setScore(rankingData);
                    }
                    currentScores.remove(playerName);
                }
            }

            // 移除不在数据中的玩家
            for (String entry : currentScores.keySet()) {
                scoreboard.resetScores(entry);
            }

            onlinePlayer.setScoreboard(scoreboard);
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