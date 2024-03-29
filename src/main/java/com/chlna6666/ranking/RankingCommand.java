package com.chlna6666.ranking;


import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.json.simple.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class RankingCommand implements CommandExecutor {
    private Ranking pluginInstance;

    public RankingCommand(Ranking pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;

        if (!(sender instanceof Player)) {
            sender.sendMessage("只有玩家可以执行此命令！");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("使用方法: /ranking <子命令>");
            return true;
        }


        if (args.length > 0 && args[0].equalsIgnoreCase("place")) {
            // 获取玩家的计分板状态

            updateScoreboardStatus(player, pluginInstance, "place");

            int scoreboardStatus = getPlayerScoreboardStatus(player, "place");

            Bukkit.getLogger().info("scoreboardStatus : " + scoreboardStatus);

            // 根据计分板状态发送消息给玩家
            if (scoreboardStatus == 1) {
                clearScoreboard(player);
                player.sendMessage("计分板已开启！");

                // 开启计分板的逻辑
                pluginInstance.updateScoreboards(player,"放置榜", pluginInstance.getplaceData(),"place");
                Bukkit.getLogger().info("pluginInstance.getplaceData(): " + pluginInstance.getplaceData());
            } else {
                player.sendMessage("计分板已关闭！");
                clearScoreboard(player); // 清空计分板
            }

            return true;
        }
        else if (args.length > 0 && args[0].equalsIgnoreCase("destroys")) {
            updateScoreboardStatus(player, pluginInstance, "destroys");
            int scoreboardStatus = getPlayerScoreboardStatus(player, "destroys");


            // 根据计分板状态发送消息给玩家
            if (scoreboardStatus == 1) {
                clearScoreboard(player);
                player.sendMessage("计分板已开启！");
                // 开启计分板的逻辑
                pluginInstance.updateScoreboards(player,"挖掘榜", pluginInstance.getdestroysData(),"destroys");
            } else {
                player.sendMessage("计分板已关闭！");
                clearScoreboard(player); // 清空计分板
            }


            return true;
        }else if (args.length > 0 && args[0].equalsIgnoreCase("deads")) {
            updateScoreboardStatus(player, pluginInstance, "deads");
            int scoreboardStatus = getPlayerScoreboardStatus(player, "deads");


            // 根据计分板状态发送消息给玩家
            if (scoreboardStatus == 1) {
                clearScoreboard(player);
                player.sendMessage("计分板已开启！");
                // 开启计分板的逻辑
                pluginInstance.updateScoreboards(player,"死亡榜", pluginInstance.getdeadsData(),"deads");
            } else {
                player.sendMessage("计分板已关闭！");
                clearScoreboard(player); // 清空计分板
            }


            return true;
        }else if (args.length > 0 && args[0].equalsIgnoreCase("mobdie")) {
            updateScoreboardStatus(player, pluginInstance, "mobdie");
            int scoreboardStatus = getPlayerScoreboardStatus(player, "mobdie");


            // 根据计分板状态发送消息给玩家
            if (scoreboardStatus == 1) {
                clearScoreboard(player);
                player.sendMessage("计分板已开启！");
                // 开启计分板的逻辑
                pluginInstance.updateScoreboards(player,"击杀榜", pluginInstance.getmobdieData(),"mobdie");
            } else {
                player.sendMessage("计分板已关闭！");
                clearScoreboard(player); // 清空计分板
            }


            return true;
        }else if (args.length > 0 && args[0].equalsIgnoreCase("onlinetime")) {
            updateScoreboardStatus(player, pluginInstance, "onlinetime");
            int scoreboardStatus = getPlayerScoreboardStatus(player, "onlinetime");


            // 根据计分板状态发送消息给玩家
            if (scoreboardStatus == 1) {
                clearScoreboard(player);
                player.sendMessage("计分板已开启！");
                // 开启计分板的逻辑
                pluginInstance.updateScoreboards(player,"时长榜", pluginInstance.getonlinetimeData(),"onlinetime");
            } else {
                player.sendMessage("计分板已关闭！");
                clearScoreboard(player); // 清空计分板
            }


            return true;
        }else if (args.length > 0 && args[0].equalsIgnoreCase("help")) {
            TextComponent message = new TextComponent("§9§l=== §b§l");
            TextComponent rankingLink = new TextComponent("[Ranking]");
            rankingLink.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/Chlna6666/Ranking"));
            TextComponent helpMessage = new TextComponent(" §9§l帮助 §f§lby Chlna6666 §9§l===\n");

            message.addExtra(rankingLink);
            message.addExtra(helpMessage);

            TextComponent place = new TextComponent("§b/ranking place §f- §7查看放置榜\n");
            place.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ranking place"));
            TextComponent destroys = new TextComponent("§b/ranking destroys §f- §7查看挖掘榜\n");
            destroys.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ranking destroys"));
            TextComponent deads = new TextComponent("§b/ranking deads §f- §7查看死亡榜\n");
            deads.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ranking deads"));
            TextComponent mobdie = new TextComponent("§b/ranking mobdie §f- §7查看击杀榜\n");
            mobdie.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ranking mobdie"));
            TextComponent onlinetime = new TextComponent("§b/ranking onlinetime §f- §7查看在线时长榜\n");
            onlinetime.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ranking onlinetime"));

            player.spigot().sendMessage(ChatMessageType.CHAT, message, place, destroys, deads, mobdie, onlinetime);

        }


        return true;
    }

    private int getPlayerScoreboardStatus(Player player, String rankingValue) {
        JSONObject playerData = getPlayerData(player, pluginInstance);
        if (playerData != null && playerData.containsKey(rankingValue)) {
            Object value = playerData.get(rankingValue);
            if (value instanceof Long) {
                long longValue = (Long) value;
                int intValue = (int) longValue;
                return intValue;
            } else if (value instanceof Integer) {
                return (int) value;
            } else {
                Bukkit.getLogger().warning("Unexpected value type for " + rankingValue + ": " + value.getClass().getSimpleName());
            }
        } else {
            Bukkit.getLogger().warning("Player data is null or doesn't contain key: " + rankingValue);
        }
        return 0;
    }





    private JSONObject getPlayerData(Player player, Ranking pluginInstance) {
        UUID uuid = player.getUniqueId();
        JSONObject playersData = pluginInstance.getPlayersData();
        return (JSONObject) playersData.get(uuid.toString());
    }

    public void updateScoreboardStatus(Player player, Ranking pluginInstance, String rankingValue) {
        List<String> specificKeys = Arrays.asList("place", "destroys", "deads","mobdie", "onlinetime");

        UUID uuid = player.getUniqueId();
        JSONObject playersData = pluginInstance.getPlayersData();
        JSONObject playerData = (JSONObject) playersData.get(uuid.toString());

        if (playerData != null) {
            // 只将特定键值设为 0
            for (String key : specificKeys) {
                if (!key.equals(rankingValue)) {
                    playerData.put(key, 0L); // 设置为0L，即Long类型的0
                }
            }

            // 切换当前键值的状态
            if (specificKeys.contains(rankingValue)) {
                long currentValue = playerData.containsKey(rankingValue) ? (long) playerData.get(rankingValue) : 0;
                long newStatus = (currentValue == 0) ? 1 : 0; // 将newStatus设为long类型
                playerData.put(rankingValue, newStatus);
            }

            //Bukkit.getLogger().warning("加载了playerData  " + playerData.toJSONString());
            // 保存玩家数据到文件
            pluginInstance.saveJSONAsync(playersData, pluginInstance.getDataFile());
        }
    }







    private void clearScoreboard(Player player) {
        Bukkit.getLogger().warning("Player player  " + player);
        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        Scoreboard newScoreboard = scoreboardManager.getNewScoreboard();  // 创建新的空白记分板
        player.setScoreboard(newScoreboard);  // 将新的空白记分板设置给玩家
        Scoreboard scoreboard = player.getScoreboard();
        if (scoreboard != null) {
            Objective objective = scoreboard.getObjective(DisplaySlot.SIDEBAR);
            if (objective != null) {
                objective.unregister();
            }
        }
    }






}