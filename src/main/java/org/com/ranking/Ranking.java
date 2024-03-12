package org.com.ranking;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
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
import org.com.ranking.metrics.Metrics;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static jdk.jfr.internal.SecuritySupport.getResourceAsStream;

public class Ranking extends JavaPlugin implements Listener {
//寄130分考你🐎

    private JSONObject playersData;
    private JSONObject placeData;
    private JSONObject destroysData;   //你说的是但是我就是分开写
    private JSONObject deadsData;

    private JSONObject  mobdieData;
    private JSONObject onlinetimeData;
    private File dataFile;
    private File placeFile;
    private File destroysFile;
    private File deadsFile;

    private File  mobdieFile;
    private File onlinetimeFile;

    // 获取 playersData
    public JSONObject getPlayersData() {
        return playersData;
    }
    // 获取 dataFile
    public File getDataFile() {
        return dataFile;
    }
    public JSONObject getplaceData() {
        return placeData;
    }
    public JSONObject getdestroysData() {
        return destroysData;
    }

    public JSONObject getdeadsData() {
        return deadsData;
    }
    public JSONObject getmobdieData() {
        return mobdieData;
    }
    public JSONObject getonlinetimeData() {
        return onlinetimeData;
    }

    private final Map<UUID, BukkitRunnable> onlineTimers = new ConcurrentHashMap<UUID, BukkitRunnable>();

    public static final String GREEN = "\u001B[0;33m";
    public static final String RESET = "\u001B[0m";


    @Override
    public void onEnable() {

        // 确保文件夹存在
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        saveDefaultConfig();
        //this.saveResource("config.yml",false);

        copyLangFiles();

        // 获取配置文件中的语言选项
        String languageOption = getConfig().getString("language");

       // 根据语言选项加载相应的语言文件
        File langFile = new File(getDataFolder(), "lang/" + languageOption + ".yml");
        YamlConfiguration langConfig = YamlConfiguration.loadConfiguration(langFile);



        Bukkit.getLogger().info("");
        Bukkit.getLogger().info(GREEN+"██████╗  █████╗ ███╗   ██╗██╗  ██╗██╗███╗   ██╗ ██████╗ "+RESET);
        Bukkit.getLogger().info(GREEN+"██╔══██╗██╔══██╗████╗  ██║██║ ██╔╝██║████╗  ██║██╔════╝ "+RESET);
        Bukkit.getLogger().info(GREEN+"██████╔╝███████║██╔██╗ ██║█████╔╝ ██║██╔██╗ ██║██║  ███╗"+RESET);
        Bukkit.getLogger().info(GREEN+"██╔══██╗██╔══██║██║╚██╗██║██╔═██╗ ██║██║╚██╗██║██║   ██║"+RESET);
        Bukkit.getLogger().info(GREEN+"██║  ██║██║  ██║██║ ╚████║██║  ██╗██║██║ ╚████║╚██████╔╝"+RESET);
        Bukkit.getLogger().info(GREEN+"╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═══╝╚═╝  ╚═╝╚═╝╚═╝  ╚═══╝ ╚═════╝"+RESET);
        Bukkit.getLogger().info("");




        String storageLocation = getConfig().getString("data_storage.location");
        String serverDirectory = System.getProperty("user.dir");
        File dataFolder = new File(serverDirectory, storageLocation);

        if (!dataFolder.exists()) {
            dataFolder.mkdirs(); // 创建文件夹及其父文件夹
        }


// 初始化数据文件
        if (getConfig().getString("data_storage.method").equalsIgnoreCase("json")) {
            dataFile = new File(dataFolder, "data.json");
            placeFile = new File(dataFolder, "place.json");
            destroysFile = new File(dataFolder, "destroys.json");
            deadsFile = new File(dataFolder, "deads.json");
            mobdieFile = new File(dataFolder,"mobdie.json");
            onlinetimeFile = new File(dataFolder, "onlinetime.json");

            // 检查并初始化 JSON 文件
            initializeAndSaveJSON(dataFile, playersData);
            initializeAndSaveJSON(placeFile, placeData);
            initializeAndSaveJSON(destroysFile, destroysData);
            initializeAndSaveJSON(deadsFile, deadsData);
            initializeAndSaveJSON(mobdieFile, mobdieData);
            initializeAndSaveJSON(onlinetimeFile, onlinetimeData);
        } else if (getConfig().getString("data_storage.method").equalsIgnoreCase("db")) {
            // 初始化数据库连接，加载数据
            initializeAndLoadDB();
        } else if (getConfig().getString("data_storage.method").equalsIgnoreCase("mysql")) {
            // 初始化 MySQL 连接，加载数据
            initializeAndLoadMySQL();
        }



        int pluginId = 21233;
        Metrics metrics = new Metrics(this, pluginId);



        // 注册事件监听器
        getServer().getPluginManager().registerEvents(this, this);
        // 注册命令
        //Objects.requireNonNull(getCommand("ranking")).setExecutor(new RankingCommand(this));
        PluginCommand rankingCommand = getCommand("ranking");

        if (rankingCommand != null) {
            CommandExecutor rankingExecutor = new RankingCommand(this);

            rankingCommand.setExecutor(rankingExecutor);
            rankingCommand.setTabCompleter(new RankingTabCompleter());
        } else {
            getLogger().warning("无法获取 /ranking 主命令！");
        }

    }


    private void copyLangFiles() {
        File langFolder = new File(getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        // 从资源中复制语言文件到插件数据文件夹
        File[] langFiles = new File(String.valueOf(getResource("lang"))).listFiles();
        if (langFiles != null) {
            for (File langFile : langFiles) {
                if (langFile.isFile()) {
                    try (InputStream inputStream = getResourceAsStream("lang/" + langFile.getName());
                         OutputStream outputStream = new FileOutputStream(new File(langFolder, langFile.getName()))) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = inputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, length);
                        }
                    } catch (IOException e) {
                        getLogger().warning("无法复制语言文件：" + langFile.getName());
                    }
                }
            }
        }
    }




    // 函数：初始化和保存 JSON 数据
// 函数：初始化和保存 JSON 数据
    private void initializeAndSaveJSON(File file, JSONObject data) {
        try {
            if (!file.exists() || file.length() == 0) {
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write("{}");
                }
                data = new JSONObject(); // 初始化数据对象
            } else {
                data = loadJSON(file); // 加载数据文件中的内容到数据对象
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 更新实例变量的值
        if (file.getName().equalsIgnoreCase("data.json")) {
            playersData = data;
        } else if (file.getName().equalsIgnoreCase("place.json")) {
            placeData = data;
        } else if (file.getName().equalsIgnoreCase("destroys.json")) {
            destroysData = data;
        } else if (file.getName().equalsIgnoreCase("deads.json")) {
            deadsData = data;
        } else if (file.getName().equalsIgnoreCase("mobdie.json")) {
            mobdieData = data;
        } else if (file.getName().equalsIgnoreCase("onlinetime.json")) {
            onlinetimeData = data;
        }
    }


    // 函数：初始化和加载数据库
    private void initializeAndLoadDB() {
        // 实现数据库连接初始化和加载数据的逻辑
    }

    // 函数：初始化和加载 MySQL 连接
    private void initializeAndLoadMySQL() {
        // 实现 MySQL 连接初始化和加载数据的逻辑
    }






    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getPlayer() == null) {
            return;
        }
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        //Bukkit.getLogger().warning("当前 placeData 的值：" + placeData.toJSONString());
        // 更新放置数量
        long placedBlocks = (long) placeData.getOrDefault(uuid.toString(), 0L);
        placeData.put(uuid.toString(), placedBlocks + 1);
        //Bukkit.getLogger().warning("修改后的 placeData 的值：" + placeData.toJSONString());
        // 异步保存 placeData
        saveJSONAsync(placeData, placeFile);
        // 刷新计分板
        updateScoreboards( player,"放置榜", placeData,"place");
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (event.getPlayer() == null) {
            return;
        }
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        //Bukkit.getLogger().warning("当前 destroysData 的值：" + destroysData.toJSONString());
        long destroysBlocks = (long) destroysData.getOrDefault(uuid.toString(), 0L);
        destroysData.put(uuid.toString(), destroysBlocks + 1);
        saveJSONAsync(destroysData, destroysFile);
        updateScoreboards( player,"挖掘榜", destroysData,"destroys");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();
        long deathCount = (long) deadsData.getOrDefault(uuid.toString(), 0L);
        deadsData.put(uuid.toString(), deathCount + 1);
        saveJSONAsync(deadsData, deadsFile);
        updateScoreboards(player, "死亡榜", deadsData,"deads");
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null || event.getEntity().getKiller().getPlayer() == null) {
            return;
        }
        Player player = event.getEntity().getKiller().getPlayer();
        UUID uuid = player.getUniqueId();
        long deathCount = (long) mobdieData.getOrDefault(uuid.toString(), 0L);
        mobdieData.put(uuid.toString(), deathCount + 1);
        saveJSONAsync(mobdieData, mobdieFile);
        updateScoreboards(player, "击杀榜", mobdieData,"mobdie");
    }




    public void updateScoreboards(Player player, String sidebarTitle, Map<String, Long> data, String dataType) {
        UUID uuid = player.getUniqueId();

        // 获取dataType为1的在线玩家
        List<Player> dataTypeOnePlayers = Bukkit.getOnlinePlayers().stream()
                .filter(p -> {
                    JSONObject pPlayerData = (JSONObject) playersData.getOrDefault(p.getUniqueId().toString(), new JSONObject());
                    Number pScoreboardConfig = (Number) pPlayerData.getOrDefault(dataType, 0);
                    return pScoreboardConfig.intValue() == 1;
                })
                .collect(Collectors.toList());

        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        Scoreboard playerScoreboard = scoreboardManager.getNewScoreboard();
        Objective objective = playerScoreboard.registerNewObjective("Ranking", "dummy", sidebarTitle);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        for (Map.Entry<String, Long> entry : data.entrySet()) {
            String uuidString = entry.getKey();
            long rankingdata = entry.getValue();

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuidString));
            if (offlinePlayer != null) {
                String playerName = offlinePlayer.getName();
                Score score = objective.getScore(playerName);
                score.setScore((int) rankingdata);
            }
        }

        // 设置指定玩家的 Scoreboard
        for (Player onlinePlayer : dataTypeOnePlayers) {
            onlinePlayer.setScoreboard(playerScoreboard);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String playerName = player.getName();



        // 如果玩家数据中没有这个 UUID 的记录，说明是第一次进入服务器
        if (!playersData.containsKey(uuid.toString())) {
            JSONObject playerInfo = new JSONObject();
            playerInfo.put("name", playerName);
            playersData.put(uuid.toString(), playerInfo);
            // 保存玩家数据到文件
            saveJSONAsync(playersData,dataFile);
        } else {
            // 玩家不是第一次进入服务器，检查玩家名称是否发生变化
            JSONObject storedPlayerInfo = (JSONObject) playersData.get(uuid.toString());
            String storedName = (String) storedPlayerInfo.get("name");

            // 如果存储的名称和当前名称不同，更新存储的名称
            if (!storedName.equals(playerName)) {
                storedPlayerInfo.put("name", playerName);
                // 保存玩家数据到文件
                saveJSONAsync(playersData,dataFile);
            }
        }

        JSONObject playerData = (JSONObject) playersData.getOrDefault(uuid.toString(), new JSONObject());
        Number placeValue = (Number) playerData.getOrDefault("place", 0);
        Number destroysValue = (Number) playerData.getOrDefault("destroys", 0);
        Number deadsValue = (Number) playerData.getOrDefault("deads", 0);
        Number mobdieValue = (Number) playerData.getOrDefault("mobdie", 0);
        Number onlinetimeValue = (Number) playerData.getOrDefault("onlinetime", 0);

        if (placeValue.intValue() == 1) {
            updateScoreboards(player, "放置榜", placeData,"place");
        }
        if (destroysValue.intValue() == 1) {
            updateScoreboards(player, "挖掘榜", destroysData,"destroys");
        }
        if (deadsValue.intValue() == 1) {
            updateScoreboards(player, "死亡榜", deadsData,"deads");
        }
        if (mobdieValue.intValue() == 1) {
            updateScoreboards(player, "击杀榜", deadsData,"mobdie");
        }
        if (onlinetimeValue.intValue() == 1) {
            updateScoreboards(player, "时长榜", onlinetimeData,"onlinetime");
        }

        // 创建并启动计时器
        BukkitRunnable timer = new BukkitRunnable() {
            @Override
            public void run() {
                long onlineTime = (long) onlinetimeData.getOrDefault(uuid.toString(), 0L);
                onlinetimeData.put(uuid.toString(), onlineTime + 1);

                // 异步保存在线时间数据到文件
                saveJSONAsync(onlinetimeData, onlinetimeFile);

                // 将更新计分板的任务发送到主线程
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // 更新计分板
                        updateScoreboards(player, "时长榜", onlinetimeData,"onlinetime");
                    }
                }.runTask(Ranking.this);
            }
        };

        // 将计时器加入Map
        onlineTimers.put(uuid, timer);

        // 启动计时器，以 ticks 为单位，表示一分钟后开始执行，每分钟执行一次
        timer.runTaskTimer(Ranking.this, 1200, 1200);

    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // 检查是否有计时器，如果有则取消
        BukkitRunnable timer = onlineTimers.get(uuid);
        if (timer != null) {
            timer.cancel();
            onlineTimers.remove(uuid);  // 在玩家退出时从Map中移除计时器
        }
    }


    private JSONObject loadJSON(File file) {
        JSONParser parser = new JSONParser();
        try (FileReader reader = new FileReader(file)) {
            Object parsedObject = parser.parse(reader);
            if (parsedObject instanceof JSONObject) {
                return (JSONObject) parsedObject;
            } else {
                Bukkit.getLogger().warning("Error loading JSON from file " + file.getName() + ": The root element is not a JSON object.");
                return new JSONObject();
            }
        } catch (IOException | ParseException e) {
            Bukkit.getLogger().warning("Error loading JSON from file " + file.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return new JSONObject();
        }
    }

    public void saveJSONAsync(JSONObject json, File file) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(json.toJSONString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void saveJSON(JSONObject json, File file) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(json.toJSONString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
