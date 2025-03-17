package com.chlna6666.ranking;

import com.chlna6666.ranking.I18n.I18n;
import com.chlna6666.ranking.updatechecker.UpdateChecker;


import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
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

import org.json.simple.JSONObject;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


import static com.chlna6666.ranking.utils.Utils.isFolia;


public class Ranking extends JavaPlugin implements Listener {

    private ConfigManager configManager;
    private I18n i18n;
    private DataManager dataManager;
    private LeaderboardSettings leaderboardSettings;
    private UpdateChecker updateChecker;

    private final Map<UUID, BukkitRunnable> onlineTimers = new HashMap<>();
    private final Map<World, Map<Location, Player>> pistonCache = new HashMap<>();

    private long SAVE_DELAY_TICKS;
    private long REGULAR_SAVE_INTERVAL_TICKS;

    public static final String BROWN = "\u001B[0;33m";
    public static final String RESET = "\u001B[0m";



    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            boolean created = getDataFolder().mkdirs();  // 使用 mkdirs() 创建目录及其父目录
            if (created) {
                getLogger().info("Data folder and any necessary parent directories created successfully.");
            } else {
                getLogger().warning("Failed to create data folder or directories.");
            }
        }

        configManager = new ConfigManager(this);
        if (Objects.requireNonNull(getConfig().getString("data_storage.method")).equalsIgnoreCase("mysql")) {
            dataManager = new MySQLDataManager(this);
        } else {
            dataManager = new DataManager(this);
        }
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


    private void handleEvent(Player player, String dataType, JSONObject data, File file, String sidebarTitle) {
        if (player == null) return;

        UUID uuid = player.getUniqueId();
        String uuidString = uuid.toString();
        long count = data.containsKey(uuidString) ? (Long) data.get(uuidString) : 0L;
        data.put(uuidString, count + 1);
        updateScoreboards(sidebarTitle, data, dataType);
        runSaveTaskAsync(() -> dataManager.saveJSON(data, file));
    }


    // 负责在 Bukkit 或 Folia 中异步执行保存任务
    private void runSaveTaskAsync(Runnable task) {
        if (isFolia()) {
            Bukkit.getGlobalRegionScheduler().run(
                    this, // 插件实例
                    scheduledTask -> task.run()
            );
        } else {
            // 在 Bukkit 中异步执行任务
            Bukkit.getScheduler().runTaskAsynchronously(this, task);
        }
    }


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String playerName = player.getName();

        if (player.hasPermission("ranking.update.notify") && getConfig().getBoolean("update_checker.notify_on_login")) {
            updateChecker.checkForUpdates(player);
        }
        JSONObject playersData = dataManager.getPlayersData();

        if (!playersData.containsKey(uuid.toString())) {
            JSONObject playerInfo = new JSONObject();
            playerInfo.put("name", playerName);
            playersData.put(uuid.toString(), playerInfo);
            dataManager.saveJSONAsync(playersData, dataManager.getDataFile());
        } else {
            JSONObject storedPlayerInfo = (JSONObject) playersData.get(uuid.toString());
            String storedName = (String) storedPlayerInfo.get("name");
            if (!storedName.equals(playerName)) {
                storedPlayerInfo.put("name", playerName);
                dataManager.saveJSONAsync(playersData, dataManager.getDataFile());
            }
        }


        clearPlayerRankingObjective(player);
        updatePlayerScoreboards(uuid);

        if (leaderboardSettings.isLeaderboardEnabled("onlinetime")) {
            BukkitRunnable timer = createOnlineTimeTimer(uuid);
            if (isFolia()) {
                // 使用Folia的调度器进行定时任务
                Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                        this,
                        scheduledTask -> {
                            if (player.isOnline()) {
                                timer.run();
                            }
                        },
                        1200L, // 延迟（ticks，1秒=20 ticks）
                        1200L  // 间隔（ticks）
                );
            } else {
                onlineTimers.put(uuid, timer);
                timer.runTaskTimer(this, 1200L, 1200L); // 延迟和间隔单位为tick（20 ticks = 1秒）
            }

        }
    }

    private void updatePlayerScoreboards(UUID uuid) {
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


    // 创建在线时间的定时任务
    private BukkitRunnable createOnlineTimeTimer( UUID uuid) {
        return new BukkitRunnable() {
            @Override
            public void run() {
                JSONObject onlinetimeData = dataManager.getOnlinetimeData();
                String uuidString = uuid.toString();

                // 获取玩家在线时间并更新
                long onlineTime = onlinetimeData.containsKey(uuidString) ? ((Number) onlinetimeData.get(uuidString)).longValue() : 0L;
                onlinetimeData.put(uuidString, onlineTime + 1);


                // 异步保存在线时间数据
                dataManager.saveJSONAsync(onlinetimeData, dataManager.getOnlinetimeFile());

                // 更新计分板，需要在主线程上运行
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        updateScoreboards(i18n.translate("sidebar.online_time"), onlinetimeData, "onlinetime");
                    }
                }.runTask(Ranking.this);  // 主线程更新计分板
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
        if (isFolia()) {
            getServer().getAsyncScheduler().runAtFixedRate(
                    this,
                    task -> dataManager.saveAllData(),
                    REGULAR_SAVE_INTERVAL_TICKS * 50L, // 初始延迟 (毫秒)
                    REGULAR_SAVE_INTERVAL_TICKS * 50L, // 执行周期 (毫秒)
                    TimeUnit.MILLISECONDS // 时间单位
            );

        } else {
            // 使用 Bukkit 的调度方式
            new BukkitRunnable() {
                @Override
                public void run() {
                    dataManager.saveAllData();
                }
            }.runTaskTimerAsynchronously(this, REGULAR_SAVE_INTERVAL_TICKS, REGULAR_SAVE_INTERVAL_TICKS);
        }
    }

    // 存储 dataType 对应的积分板和 UUID-名称映射
    private final Map<String, Scoreboard> dataTypeScoreboards = new HashMap<>();
    private final Map<String, Map<String, String>> dataTypeUUIDToName = new HashMap<>(); // dataType -> (UUID -> PlayerName)

    public void updateScoreboards(String sidebarTitle, Map<String, Long> data, String dataType) {
        // 获取或创建积分板及名称映射
        Scoreboard scoreboard = dataTypeScoreboards.computeIfAbsent(dataType,
                key -> Bukkit.getScoreboardManager().getNewScoreboard());
        Map<String, String> uuidToNameMap = dataTypeUUIDToName.computeIfAbsent(dataType,
                key -> new HashMap<>());

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

        // 临时存储有效玩家名称
        Set<String> validNames = new HashSet<>();

        // 从缓存中获取玩家数据，避免每次调用 getOfflinePlayer
        JSONObject playersData = dataManager.getPlayersData();

        // 更新玩家分数
        for (Map.Entry<String, Long> entry : data.entrySet()) {
            String uuid = entry.getKey();
            long scoreValue = entry.getValue();

            String currentName = "Unknown";
            if (playersData.containsKey(uuid)) {
                JSONObject playerData = (JSONObject) playersData.get(uuid);
                currentName = (String) playerData.getOrDefault("name", "Unknown");
            }
            validNames.add(currentName);

            // 处理玩家改名
            String storedName = uuidToNameMap.get(uuid);
            if (storedName != null && !storedName.equals(currentName)) {
                scoreboard.resetScores(storedName); // 清除旧名称
            }
            uuidToNameMap.put(uuid, currentName);

            // 更新分数
            Score score = objective.getScore(currentName);
            if (score.getScore() != (int) scoreValue) {
                score.setScore((int) scoreValue);
            }
        }

        // 清理已移除的玩家数据
        Iterator<Map.Entry<String, String>> iterator = uuidToNameMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            String uuid = entry.getKey();
            String name = entry.getValue();

            if (!data.containsKey(uuid)) {
                scoreboard.resetScores(name);
                iterator.remove();
            }
        }

        // 二次清理残留条目（处理其他插件添加的条目）
        for (String entry : scoreboard.getEntries()) {
            if (objective.getScore(entry).isScoreSet() && !validNames.contains(entry)) {
                scoreboard.resetScores(entry);
            }
        }

        // 更新玩家显示状态
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



    private void clearPlayerRankingObjective(Player player) {
        Scoreboard scoreboard = player.getScoreboard();

        // 清除玩家记分板上的所有目标
        for (Objective objective : new ArrayList<>(scoreboard.getObjectives())) {
            try {
                objective.unregister();
            } catch (Exception e) {
                getLogger().warning("无法注销目标: " + objective.getName() + ". 错误: " + e.getMessage());
            }
        }
    }



    public long getSaveDelayTicks() {
        return SAVE_DELAY_TICKS;
    }

    public I18n getI18n() {
        return i18n;
    }
}