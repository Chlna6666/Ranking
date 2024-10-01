package com.chlna6666.ranking;

import com.chlna6666.ranking.I18n.I18n;
import com.chlna6666.ranking.updatechecker.UpdateChecker;

import com.fasterxml.jackson.databind.JsonNode;
import org.bukkit.Bukkit;
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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;


public class Ranking extends JavaPlugin implements Listener {

    private ConfigManager configManager;
    private I18n i18n;
    private DataManager dataManager;
    private LeaderboardSettings leaderboardSettings;
    private UpdateChecker updateChecker;

    // 使用 ConcurrentHashMap 来确保线程安全
    private final ConcurrentMap<UUID, BukkitRunnable> onlineTimers = new ConcurrentHashMap<>();
    // 使用 ConcurrentHashMap 作为内层 Map，以提高并发性能
    private final ConcurrentMap<World, ConcurrentMap<Location, Player>> pistonCache = new ConcurrentHashMap<>();

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
                    pistonCache.computeIfAbsent(block.getWorld(), k -> new ConcurrentHashMap<>()).put(bedrockPos, player);
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
            handleEvent(event.getPlayer(), "destroys", dataManager.getDestroysData(), dataManager.getDestroysFile(), i18n.translate("sidebar.break"));
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

    private void handleEvent(Player player, String dataType, ObjectNode data, File file, String sidebarTitle) {
        if (player == null) return;

        UUID uuid = player.getUniqueId();
        String uuidString = uuid.toString();
        long count = data.has(uuidString) ? data.get(uuidString).asLong() : 0L;
        data.put(uuidString, count + 1);

        updateScoreboards(player, sidebarTitle, data, dataType);
        dataManager.startSaveTask(new AtomicBoolean(), () -> dataManager.saveJSON(data, file));
    }
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String playerName = player.getName();

        if (player.hasPermission("ranking.update.notify") && getConfig().getBoolean("update_checker.notify_on_login")) {
            updateChecker.checkForUpdates(player);
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode playersData = dataManager.getPlayersData();

        if (!playersData.has(uuid.toString())) {
            ObjectNode playerInfo = mapper.createObjectNode();
            playerInfo.put("name", playerName);
            playersData.set(uuid.toString(), playerInfo);
            dataManager.saveJSONAsync(playersData, dataManager.getDataFile());
        } else {
            ObjectNode storedPlayerInfo = (ObjectNode) playersData.get(uuid.toString());
            String storedName = storedPlayerInfo.get("name").asText();

            if (!storedName.equals(playerName)) {
                storedPlayerInfo.put("name", playerName);
                dataManager.saveJSONAsync(playersData, dataManager.getDataFile());
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
        ObjectNode playerData = dataManager.getPlayersData().has(uuid.toString()) ?
                (ObjectNode) dataManager.getPlayersData().get(uuid.toString()) :
                new ObjectMapper().createObjectNode();

        checkAndUpdateScoreboard(player, playerData, "place", i18n.translate("sidebar.place"), dataManager.getPlaceData());
        checkAndUpdateScoreboard(player, playerData, "destroys", i18n.translate("sidebar.break"), dataManager.getDestroysData());
        checkAndUpdateScoreboard(player, playerData, "deads", i18n.translate("sidebar.death"), dataManager.getDeadsData());
        checkAndUpdateScoreboard(player, playerData, "mobdie", i18n.translate("sidebar.kill"), dataManager.getMobdieData());
        checkAndUpdateScoreboard(player, playerData, "onlinetime", i18n.translate("sidebar.online_time"), dataManager.getOnlinetimeData());
        checkAndUpdateScoreboard(player, playerData, "break_bedrock", i18n.translate("sidebar.break_bedrock"), dataManager.getBreakBedrockData());
    }
    private void checkAndUpdateScoreboard(Player player, ObjectNode playerData, String dataType, String sidebarTitle, ObjectNode data) {
        int value = playerData.has(dataType) ? playerData.get(dataType).asInt() : 0;
        if (value == 1) {
            updateScoreboards(player, sidebarTitle, data, dataType);
        }
    }

    private BukkitRunnable createOnlineTimeTimer(Player player, UUID uuid) {
        return new BukkitRunnable() {
            @Override
            public void run() {
                ObjectNode onlinetimeData = dataManager.getOnlinetimeData();
                String uuidString = uuid.toString();

                long onlineTime = onlinetimeData.has(uuidString) ? onlinetimeData.get(uuidString).asLong() : 0L;
                onlinetimeData.put(uuidString, onlineTime + 1);

                dataManager.saveJSONAsync(onlinetimeData, dataManager.getOnlinetimeFile());

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        updateScoreboards(player, i18n.translate("sidebar.online_time"), onlinetimeData, "onlinetime");
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

    public void updateScoreboards(Player player, String sidebarTitle, ObjectNode data, String dataType) {
        // 获取当前使用 dataType 为 1 的在线玩家
        List<Player> dataTypeOnePlayers = Bukkit.getOnlinePlayers().stream()
                .filter(p -> {
                    ObjectNode pPlayerData = (ObjectNode) dataManager.getPlayersData().get(p.getUniqueId().toString());
                    if (pPlayerData == null) {
                        pPlayerData = new ObjectMapper().createObjectNode();
                    }
                    return pPlayerData.has(dataType) && pPlayerData.get(dataType).asInt() == 1;
                })
                .collect(Collectors.toList());

        // 遍历所有需要更新记分板的玩家
        for (Player onlinePlayer : dataTypeOnePlayers) {
            Scoreboard scoreboard = onlinePlayer.getScoreboard();
            String objectiveName = "Ranking_" + dataType;
            Objective objective = scoreboard.getObjective(objectiveName);

            if (objective == null) {
                // 创建新的 Objective
                objective = scoreboard.registerNewObjective(objectiveName, "dummy", sidebarTitle);
                objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            } else if (!objective.getDisplayName().equals(sidebarTitle)) {
                // 更新 Objective 的显示名称
                objective.setDisplayName(sidebarTitle);
            }

            // 获取当前记分板上的所有分数
            Objective finalObjective = objective;
            Map<String, Integer> currentScores = scoreboard.getEntries().stream()
                    .collect(Collectors.toMap(
                            entry -> entry,
                            entry -> finalObjective.getScore(entry).getScore(),
                            (a, b) -> a, // 合并函数，防止冲突
                            HashMap::new
                    ));

            // 更新每个玩家的数据
            Iterator<Map.Entry<String, JsonNode>> fields = data.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String uuidString = entry.getKey();
                int rankingData = entry.getValue().asInt();
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuidString));
                String playerName = offlinePlayer.getName();

                // 检查当前分数是否需要更新
                Integer currentScore = currentScores.get(playerName);
                if (currentScore == null || currentScore != rankingData) {
                    Score score = finalObjective.getScore(playerName);
                    score.setScore(rankingData);
                }

                // 从当前分数中移除已更新的玩家
                currentScores.remove(playerName);
            }

            // 移除不在数据中的玩家
            currentScores.keySet().forEach(scoreboard::resetScores);

            // 重新设置记分板
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