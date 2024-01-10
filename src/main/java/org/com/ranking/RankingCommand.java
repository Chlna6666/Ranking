package org.com.ranking;


import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.json.simple.JSONObject;

import java.util.UUID;

public class RankingCommand implements CommandExecutor {
    private Ranking pluginInstance;

    public RankingCommand(Ranking pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (args.length == 0) {
                sender.sendMessage("使用方法: /ranking <子命令>");
                return true;
            }

            if (args.length > 0 && args[0].equalsIgnoreCase("place")) {
                // 处理 /ranking place 命令
                int scoreboardStatus = 0;

                UUID uuid = player.getUniqueId();
                JSONObject playersData = pluginInstance.getPlayersData();
                JSONObject playerData = (JSONObject) playersData.get(uuid.toString());
                if (playerData != null && playerData.containsKey("place")) {
                    Object placeValue = playerData.get("place");
                    if (placeValue instanceof Integer) {
                        scoreboardStatus = (int) placeValue;
                    } else if (placeValue instanceof Long) {
                        scoreboardStatus = Math.toIntExact((long) placeValue);
                    }
                }
                // 切换计分板状态
                scoreboardStatus = (scoreboardStatus == 0) ? 1 : 0;

                // 更新玩家数据中的 "place" 值
                playerData.put("place", scoreboardStatus);
                Bukkit.getLogger().warning("加载了playerData  " + playerData.toJSONString());

                // 保存玩家数据到文件
                pluginInstance.saveJSONAsync(playersData, pluginInstance.getDataFile());

                // 根据计分板状态发送消息给玩家
                if (scoreboardStatus == 1) {
                    player.sendMessage("计分板已开启！");
                    // 开启计分板的逻辑
                    pluginInstance.updateScoreboards(player,"放置榜", pluginInstance.getplaceData());

                } else {
                    player.sendMessage("计分板已关闭！");
                    clearScoreboard(player); // 清空计分板
                }

                return true;
            } else if (args.length > 0 && args[0].equalsIgnoreCase("destroys")) {


                return true;
            }  else if (args.length > 0 && args[0].equalsIgnoreCase("deads")) {


            return true;
           }else if (args.length > 0 && args[0].equalsIgnoreCase("onlinetime")) {


            return true;
        }
        }
        else {
            sender.sendMessage("只有玩家可以执行此命令！");
            return true;
        }
        return true;
    }



    private void clearScoreboard(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        if (scoreboard != null) {
            Objective objective = scoreboard.getObjective(DisplaySlot.SIDEBAR);
            if (objective != null) {
                objective.unregister();
            }
        }
    }

}



