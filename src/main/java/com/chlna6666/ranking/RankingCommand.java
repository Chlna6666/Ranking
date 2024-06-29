package com.chlna6666.ranking;

import com.chlna6666.ranking.I18n.I18n;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

public class RankingCommand implements CommandExecutor {
    private final Ranking pluginInstance;
    private final DataManager dataManager;
    private final I18n i18n;

    public RankingCommand(Ranking pluginInstance, DataManager dataManager, I18n i18n) {
        this.pluginInstance = pluginInstance;
        this.dataManager = dataManager;
        this.i18n = i18n;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(i18n.translate("command.only_players"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sender.sendMessage(i18n.translate("command.usage_ranking"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "place":
                handleScoreboardToggle(player, "place", i18n.translate("command.place_board"), dataManager.getPlaceData());
                break;
            case "destroys":
                handleScoreboardToggle(player, "destroys", i18n.translate("command.destroys_board"), dataManager.getDestroysData());
                break;
            case "deads":
                handleScoreboardToggle(player, "deads", i18n.translate("command.deads_board"), dataManager.getDeadsData());
                break;
            case "mobdie":
                handleScoreboardToggle(player, "mobdie", i18n.translate("command.mobdie_board"), dataManager.getMobdieData());
                break;
            case "onlinetime":
                handleScoreboardToggle(player, "onlinetime", i18n.translate("command.onlinetime_board"), dataManager.getOnlinetimeData());
                break;
            case "all":
                displayAllRankings(player);
                break;
            case "my":
                displayPlayerRankings(player);
                break;
            case "list":
                if (args.length > 1) {
                    handleSingleRanking(player, args[1]);
                } else {
                    player.sendMessage(i18n.translate("command.usage_list"));
                }
                break;
            case "help":
                displayHelpMessage(player);
                break;
            default:
                player.sendMessage(i18n.translate("command.unknown_command"));
                break;
        }

        return true;
    }

    private void handleScoreboardToggle(Player player, String rankingName, String displayName, Map<String, Long> rankingData) {
        updateScoreboardStatus(player, rankingName);
        int scoreboardStatus = getPlayerScoreboardStatus(player, rankingName);

        if (scoreboardStatus == 1) {
            clearScoreboard(player);
            player.sendMessage(displayName + i18n.translate("command.enabled"));
            pluginInstance.updateScoreboards(player, displayName, rankingData, rankingName);
        } else {
            player.sendMessage(displayName + i18n.translate("command.disabled"));
            clearScoreboard(player);
        }
    }

    private void displayAllRankings(Player player) {
        player.sendMessage(ChatColor.GOLD + i18n.translate("command.all_rankings"));
        displayRankingData(player, i18n.translate("command.place_board"), dataManager.getPlaceData());
        displayRankingData(player, i18n.translate("command.destroys_board"), dataManager.getDestroysData());
        displayRankingData(player, i18n.translate("command.deads_board"), dataManager.getDeadsData());
        displayRankingData(player, i18n.translate("command.mobdie_board"), dataManager.getMobdieData());
        displayRankingData(player, i18n.translate("command.onlinetime_board"), dataManager.getOnlinetimeData());
    }

    private void displayPlayerRankings(Player player) {
        UUID uuid = player.getUniqueId();
        player.sendMessage(ChatColor.GOLD + i18n.translate("command.your_rankings"));
        displayPlayerData(player, i18n.translate("command.place_board"), dataManager.getPlaceData(), uuid);
        displayPlayerData(player, i18n.translate("command.destroys_board"), dataManager.getDestroysData(), uuid);
        displayPlayerData(player, i18n.translate("command.deads_board"), dataManager.getDeadsData(), uuid);
        displayPlayerData(player, i18n.translate("command.mobdie_board"), dataManager.getMobdieData(), uuid);
        displayPlayerData(player, i18n.translate("command.onlinetime_board"), dataManager.getOnlinetimeData(), uuid);
    }

    private void handleSingleRanking(Player player, String rankingName) {
        switch (rankingName.toLowerCase()) {
            case "place":
                displayRankingData(player, i18n.translate("command.place_board"), dataManager.getPlaceData());
                break;
            case "destroys":
                displayRankingData(player, i18n.translate("command.destroys_board"), dataManager.getDestroysData());
                break;
            case "deads":
                displayRankingData(player, i18n.translate("command.deads_board"), dataManager.getDeadsData());
                break;
            case "mobdie":
                displayRankingData(player, i18n.translate("command.mobdie_board"), dataManager.getMobdieData());
                break;
            case "onlinetime":
                displayRankingData(player, i18n.translate("command.onlinetime_board"), dataManager.getOnlinetimeData());
                break;
            default:
                player.sendMessage(i18n.translate("command.unknown_ranking"));
                break;
        }
    }

    private void displayRankingData(Player player, String title, Map<String, Long> data) {
        player.sendMessage(ChatColor.GOLD + title + i18n.translate("command.colon"));
        data.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> {
                    UUID uuid = UUID.fromString(entry.getKey());
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                    String playerName = offlinePlayer.getName();
                    String message = (uuid.equals(player.getUniqueId()) ? ChatColor.GREEN : ChatColor.WHITE) +
                            playerName + ": " + entry.getValue();
                    player.sendMessage(message);
                });
    }

    private void displayPlayerData(Player player, String title, Map<String, Long> data, UUID uuid) {
        Long value = data.get(uuid.toString());
        if (value != null) {
            player.sendMessage(ChatColor.GOLD + title + i18n.translate("command.colon") + ChatColor.GREEN + value);
        } else {
            player.sendMessage(ChatColor.GOLD + title + i18n.translate("command.colon") + i18n.translate("command.no_data"));
        }
    }

    private void updateScoreboardStatus(Player player, String rankingValue) {
        List<String> specificKeys = Arrays.asList("place", "destroys", "deads", "mobdie", "onlinetime");

        UUID uuid = player.getUniqueId();
        JSONObject playersData = dataManager.getPlayersData();
        JSONObject playerData = (JSONObject) playersData.get(uuid.toString());

        if (playerData != null) {
            for (String key : specificKeys) {
                if (!key.equals(rankingValue)) {
                    playerData.put(key, 0L);
                }
            }

            if (specificKeys.contains(rankingValue)) {
                long currentValue = playerData.containsKey(rankingValue) ? (long) playerData.get(rankingValue) : 0;
                long newStatus = (currentValue == 0) ? 1 : 0;
                playerData.put(rankingValue, newStatus);
            }

            dataManager.saveJSONAsync(playersData, dataManager.getDataFile());
        }
    }

    private int getPlayerScoreboardStatus(Player player, String rankingValue) {
        JSONObject playerData = getPlayerData(player);
        if (playerData != null && playerData.containsKey(rankingValue)) {
            Object value = playerData.get(rankingValue);
            if (value instanceof Long) {
                return ((Long) value).intValue();
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

    private JSONObject getPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        JSONObject playersData = dataManager.getPlayersData();
        return (JSONObject) playersData.get(uuid.toString());
    }

    private void clearScoreboard(Player player) {
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

    private void displayHelpMessage(Player player) {
        TextComponent message = new TextComponent("§9§l=== §b§l");
        TextComponent rankingLink = new TextComponent("[Ranking]");
        rankingLink.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/Chlna6666/Ranking"));
        TextComponent helpMessage = new TextComponent(" §9§l" + i18n.translate("command.help") + " §f§lby Chlna6666 §9§l===\n");

        message.addExtra(rankingLink);
        message.addExtra(helpMessage);

        TextComponent place = new TextComponent("§b/ranking place §f- §7" + i18n.translate("command.view_place_board") + "\n");
        place.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ranking place"));
        TextComponent destroys = new TextComponent("§b/ranking destroys §f- §7" + i18n.translate("command.view_destroys_board") + "\n");
        destroys.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ranking destroys"));
        TextComponent deads = new TextComponent("§b/ranking deads §f- §7" + i18n.translate("command.view_deads_board") + "\n");
        deads.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ranking deads"));
        TextComponent mobdie = new TextComponent("§b/ranking mobdie §f- §7" + i18n.translate("command.view_mobdie_board") + "\n");
        mobdie.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ranking mobdie"));
        TextComponent onlinetime = new TextComponent("§b/ranking onlinetime §f- §7" + i18n.translate("command.view_onlinetime_board") + "\n");
        onlinetime.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ranking onlinetime"));
        TextComponent all = new TextComponent("§b/ranking all §f- §7" + i18n.translate("command.view_all_boards") + "\n");
        all.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ranking all"));
        TextComponent my = new TextComponent("§b/ranking my §f- §7" + i18n.translate("command.view_my_boards") + "\n");
        my.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ranking my"));
        TextComponent list = new TextComponent("§b/ranking list <ranking_name> §f- §7" + i18n.translate("command.view_specific_board") + "\n");
        list.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ranking list <ranking_name>"));

        player.spigot().sendMessage(message, place, destroys, deads, mobdie, onlinetime, all, my, list);
    }
}
