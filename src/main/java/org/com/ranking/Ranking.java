package org.com.ranking;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Ranking extends JavaPlugin implements Listener {
////////////////////////////////////////////////////////////////////
//                          _ooOoo_                               //
//                         o8888888o                              //
//                         88" . "88                              //
//                         (| ^_^ |)                              //
//                         O\  =  /O                              //
//                      ____/`---'\____                           //
//                    .'  \\|     |//  `.                         //
//                   /  \\|||  :  |||//  \                        //
//                  /  _||||| -:- |||||-  \                       //
//                  |   | \\\  -  /// |   |                       //
//                  | \_|  ''\---/''  |   |                       //
//                  \  .-\__  `-`  ___/-. /                       //
//                ___`. .'  /--.--\  `. . ___                     //
//              ."" '<  `.___\_<|>_/___.'  >'"".                  //
//            | | :  `- \`.;`\ _ /`;.`/ - ` : | |                 //
//            \  \ `-.   \_ __\ /__ _/   .-` /  /                 //
//      ========`-.____`-.___\_____/___.-`____.-'========         //
//                           `=---='                              //
//      ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^        //
//                  佛祖保佑   3+证书考试必过200分                    //
////////////////////////////////////////////////////////////////////

    private JSONObject playersData;
    private JSONObject placeData;
    private JSONObject destroysData;   //你说的是但是我就是分开写
    private JSONObject deadsData;
    private JSONObject onlinetimeData;
    private File dataFile;
    private File placeFile;
    private File destroysFile;
    private File deadsFile;
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
    public JSONObject getonlinetimeData() {
        return onlinetimeData;
    }

    private final Map<UUID, BukkitRunnable> onlineTimers = new ConcurrentHashMap<UUID, BukkitRunnable>();

    public static final String GREEN = "\u001B[0;33m";
    public static final String RESET = "\u001B[0m";


    @Override
    public void onEnable() {
        Bukkit.getLogger().info("");
        Bukkit.getLogger().info(GREEN+" ______   ______   __   __   __  __   __   __   __   ______    "+RESET);
        Bukkit.getLogger().info(GREEN+"/\\  == \\ /\\  __ \\ /\\ \"-.\\ \\ /\\ \\/ /  /\\ \\ /\\ \"-.\\ \\ /\\  ___\\   "+RESET);
        Bukkit.getLogger().info(GREEN+"\\ \\  __< \\ \\  __ \\\\ \\ \\-.  \\\\ \\  _\"-.\\ \\ \\\\ \\ \\-.  \\\\ \\ \\__ \\  "+RESET);
        Bukkit.getLogger().info(GREEN+" \\ \\_\\ \\_\\\\ \\_\\ \\_\\\\ \\_\\\\\"\\_\\\\ \\_\\ \\_\\\\ \\_\\\\ \\_\\\\\"\\_\\\\ \\_____\\ "+RESET);
        Bukkit.getLogger().info(GREEN+"  \\/_/ /_/ \\/_/\\/_/ \\/_/ \\/_/ \\/_/\\/_/ \\/_/ \\/_/ \\/_/ \\/_____/ "+RESET);
        Bukkit.getLogger().info("");


        Bukkit.getLogger().info("");
        Bukkit.getLogger().info(GREEN+"██████╗  █████╗ ███╗   ██╗██╗  ██╗██╗███╗   ██╗ ██████╗ "+RESET);
        Bukkit.getLogger().info(GREEN+"██╔══██╗██╔══██╗████╗  ██║██║ ██╔╝██║████╗  ██║██╔════╝ "+RESET);
        Bukkit.getLogger().info(GREEN+"██████╔╝███████║██╔██╗ ██║█████╔╝ ██║██╔██╗ ██║██║  ███╗"+RESET);
        Bukkit.getLogger().info(GREEN+"██╔══██╗██╔══██║██║╚██╗██║██╔═██╗ ██║██║╚██╗██║██║   ██║"+RESET);
        Bukkit.getLogger().info(GREEN+"██║  ██║██║  ██║██║ ╚████║██║  ██╗██║██║ ╚████║╚██████╔╝"+RESET);
        Bukkit.getLogger().info(GREEN+"╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═══╝╚═╝  ╚═╝╚═╝╚═╝  ╚═══╝ ╚═════╝"+RESET);
        Bukkit.getLogger().info("");



        // 确保文件夹存在
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }


        // 初始化数据文件
        dataFile = new File(getDataFolder(), "data.json");
        placeFile = new File(getDataFolder(), "place.json");
        destroysFile = new File(getDataFolder(), "destroys.json");
        deadsFile = new File(getDataFolder(), "deads.json");
        onlinetimeFile = new File(getDataFolder(), "onlinetime.json");

        if (!dataFile.exists() || dataFile.length() == 0) {
            try (FileWriter writer = new FileWriter(dataFile)) {
                writer.write("{}");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (!placeFile.exists() || placeFile.length() == 0) {
            try (FileWriter writer = new FileWriter(placeFile)) {
                writer.write("{}");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!destroysFile.exists() || destroysFile.length() == 0) {
            try (FileWriter writer = new FileWriter(destroysFile)) {
                writer.write("{}");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!deadsFile.exists() || deadsFile.length() == 0) {
            try (FileWriter writer = new FileWriter(deadsFile)) {
                writer.write("{}");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!onlinetimeFile.exists() || onlinetimeFile.length() == 0) {
            try (FileWriter writer = new FileWriter(onlinetimeFile)) {
                writer.write("{}");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

            // 保存数据
        if (!dataFile.exists()) {
             playersData = new JSONObject();
             saveJSONAsync(playersData, dataFile);
        } else {
             playersData = loadJSON(dataFile);
        }
        if (!placeFile.exists()) {
            placeData = new JSONObject();
            saveJSONAsync(placeData, placeFile);
        } else {
            placeData = loadJSON(placeFile);
        }
        if (!destroysFile.exists()) {
            destroysData = new JSONObject();
            saveJSONAsync(destroysData, destroysFile);
        } else {
            destroysData = loadJSON(destroysFile);
        }
        if (!deadsFile.exists()) {
            deadsData = new JSONObject();
            saveJSONAsync(deadsData, deadsFile);
        } else {
            deadsData = loadJSON(deadsFile);
        }
        if (!onlinetimeFile.exists()) {
            onlinetimeData = new JSONObject();
            saveJSONAsync(onlinetimeData, onlinetimeFile);
        } else {
            onlinetimeData = loadJSON(onlinetimeFile);
        }


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







    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
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




    public void updateScoreboards(Player player, String sidebarTitle, Map<String, Long> data, String dataType) {
        UUID uuid = player.getUniqueId();

        JSONObject playerData = (JSONObject) playersData.getOrDefault(uuid.toString(), new JSONObject());

        // 检查该积分板是否在配置中开启
        Number scoreboardConfig = (Number) playerData.getOrDefault(dataType, 0);
        if (scoreboardConfig.intValue() != 1) {
            return;  // 积分板未开启，直接返回
        }

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



    /*public void updateScoreboards(Player player,String sidebarTitle, Map<String, Long> data) {
        UUID uuid = player.getUniqueId();
        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        Scoreboard globalScoreboard = scoreboardManager.getNewScoreboard();
        Objective objective = globalScoreboard.registerNewObjective("Ranking", "dummy", sidebarTitle);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        for (Map.Entry<String, Long> entry : data.entrySet()) {
            String uuidString = entry.getKey();
            long placedBlocks = entry.getValue();

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuidString));
            if (offlinePlayer != null) {
                String playerName = offlinePlayer.getName();

                Score score = objective.getScore(playerName);
                score.setScore((int) placedBlocks);
            }
        }

        JSONObject playerData = (JSONObject) playersData.getOrDefault(uuid.toString(), new JSONObject());
        Number placeValue = (Number) playerData.getOrDefault("place", 0);


        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.setScoreboard(globalScoreboard);
        }


    }

     */


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
