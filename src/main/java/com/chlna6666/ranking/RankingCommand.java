package com.chlna6666.ranking;

import com.chlna6666.ranking.I18n.I18n;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.json.simple.JSONObject;


import org.jetbrains.annotations.NotNull;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.chlna6666.ranking.utils.Utils.isFolia;
import static org.bukkit.Bukkit.getLogger;

public class RankingCommand implements CommandExecutor {
    private final Ranking pluginInstance;
    private final DataManager dataManager;
    private final I18n i18n;
    private final LeaderboardSettings leaderboardSettings;
    private final BukkitAudiences adventure;

    public RankingCommand(Ranking pluginInstance, DataManager dataManager, I18n i18n) {
        this.pluginInstance = pluginInstance;
        this.dataManager = dataManager;
        this.i18n = i18n;
        this.adventure = BukkitAudiences.create(pluginInstance);
        this.leaderboardSettings = LeaderboardSettings.getInstance();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(i18n.translate("command.only_players"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(i18n.translate("command.usage_ranking"));
            return true;
        }

        if (leaderboardSettings.isLeaderboardEnabled(args[0].toLowerCase())) {
            handleLeaderboardCommand(player, args);
        } else {
            handleGeneralCommand(player, args);
        }

        return true;
    }

    private void handleGeneralCommand(Player player, String[] args) {
        switch (args[0].toLowerCase()) {
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
    }

    private void handleLeaderboardCommand(Player player, String[] args) {
        String rankingName = args[0].toLowerCase();
        switch (rankingName) {
            case "place":
                toggleScoreboard(player, rankingName, i18n.translate("sidebar.place"), dataManager.getPlaceData());
                break;
            case "destroys":
                toggleScoreboard(player, rankingName, i18n.translate("sidebar.break"), dataManager.getDestroysData());
                break;
            case "deads":
                toggleScoreboard(player, rankingName, i18n.translate("sidebar.death"), dataManager.getDeadsData());
                break;
            case "mobdie":
                toggleScoreboard(player, rankingName, i18n.translate("sidebar.kill"), dataManager.getMobdieData());
                break;
            case "onlinetime":
                toggleScoreboard(player, rankingName, i18n.translate("sidebar.online_time"), dataManager.getOnlinetimeData());
                break;
            case "break_bedrock":
                toggleScoreboard(player, rankingName, i18n.translate("sidebar.break_bedrock"), dataManager.getBreakBedrockData());
                break;
            default:
                player.sendMessage(i18n.translate("command.unknown_ranking"));
                break;
        }
    }

    private void toggleScoreboard(Player player, String rankingName, String displayName, JSONObject rankingData) {
        updateScoreboardStatus(player, rankingName);
        int scoreboardStatus = getPlayerScoreboardStatus(player, rankingName);

        if (scoreboardStatus == 1) {
            clearScoreboard(player);
            player.sendMessage(displayName + i18n.translate("command.enabled"));
            if (isFolia()) {
                Bukkit.getRegionScheduler().run(pluginInstance, player.getLocation(), scheduledTask -> {
                    pluginInstance.updateScoreboards( displayName, rankingData, rankingName);
                });
            } else {
                pluginInstance.updateScoreboards(displayName, rankingData, rankingName);
            }

        } else {
            player.sendMessage(displayName + i18n.translate("command.disabled"));
            clearScoreboard(player);
        }
    }

    private void updateScoreboardStatus(Player player, String rankingValue) {
        List<String> specificKeys = Arrays.asList("place", "destroys", "deads", "mobdie", "onlinetime", "break_bedrock");

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
                long currentValue = playerData.containsKey(rankingValue) ? (Long) playerData.get(rankingValue) : 0;
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
                return (Integer) value;
            }
        }
        return 0;
    }

    private JSONObject getPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        JSONObject playersData = dataManager.getPlayersData();
        return (JSONObject) playersData.get(uuid.toString());
    }

    private void displayAllRankings(Player player) {
        player.sendMessage(Component.text(i18n.translate("command.all_rankings"), NamedTextColor.GOLD));
        displayRankingData(player, i18n.translate("sidebar.place"), dataManager.getPlaceData());
        displayRankingData(player, i18n.translate("sidebar.break"), dataManager.getDestroysData());
        displayRankingData(player, i18n.translate("sidebar.death"), dataManager.getDeadsData());
        displayRankingData(player, i18n.translate("sidebar.kill"), dataManager.getMobdieData());
        displayRankingData(player, i18n.translate("sidebar.online_time"), dataManager.getOnlinetimeData());
        displayRankingData(player, i18n.translate("sidebar.break_bedrock"), dataManager.getBreakBedrockData());
    }

    private void displayPlayerRankings(Player player) {
        UUID uuid = player.getUniqueId();
        player.sendMessage(Component.text(i18n.translate("command.your_rankings"), NamedTextColor.GOLD));
        displayPlayerData(player, i18n.translate("sidebar.place"), dataManager.getPlaceData(), uuid);
        displayPlayerData(player, i18n.translate("sidebar.break"), dataManager.getDestroysData(), uuid);
        displayPlayerData(player, i18n.translate("sidebar.death"), dataManager.getDeadsData(), uuid);
        displayPlayerData(player, i18n.translate("sidebar.kill"), dataManager.getMobdieData(), uuid);
        displayPlayerData(player, i18n.translate("sidebar.online_time"), dataManager.getOnlinetimeData(), uuid);
        displayPlayerData(player, i18n.translate("sidebar.break_bedrock"), dataManager.getBreakBedrockData(), uuid);
    }

    private void handleSingleRanking(Player player, String rankingName) {
        if (leaderboardSettings.isLeaderboardEnabled(rankingName.toLowerCase())) {
            switch (rankingName.toLowerCase()) {
                case "place":
                    displayRankingData(player, i18n.translate("sidebar.place"), dataManager.getPlaceData());
                    break;
                case "destroys":
                    displayRankingData(player, i18n.translate("sidebar.break"), dataManager.getDestroysData());
                    break;
                case "deads":
                    displayRankingData(player, i18n.translate("sidebar.death"), dataManager.getDeadsData());
                    break;
                case "mobdie":
                    displayRankingData(player, i18n.translate("sidebar.kill"), dataManager.getMobdieData());
                    break;
                case "onlinetime":
                    displayRankingData(player, i18n.translate("sidebar.online_time"), dataManager.getOnlinetimeData());
                    break;
                case "break_bedrock":
                    displayRankingData(player, i18n.translate("sidebar.break_bedrock"), dataManager.getBreakBedrockData());
                    break;
                default:
                    player.sendMessage(i18n.translate("command.unknown_ranking"));
                    break;
            }
        } else {
            player.sendMessage(i18n.translate("command.unknown_ranking"));
        }
    }

    private void displayRankingData(Player player, String title, JSONObject data) {
        player.sendMessage(Component.text(title + i18n.translate("command.colon"), NamedTextColor.GOLD));
        data.forEach((key, value) -> {
            try {
                UUID uuid = UUID.fromString((String) key);
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown Player";

                Component message = Component.text(playerName + ": " + value)
                        .color(uuid.equals(player.getUniqueId()) ? NamedTextColor.GREEN : NamedTextColor.WHITE);

                adventure.player(player).sendMessage(message);
            } catch (IllegalArgumentException e) {
                adventure.player(player).sendMessage(Component.text("Error parsing UUID: " + key, NamedTextColor.RED));
            }
        });
    }

    private void displayPlayerData(Player player, String title, JSONObject data, UUID uuid) {
        Object value = data.get(uuid.toString());
        if (value != null) {
            adventure.player(player).sendMessage(
                    Component.text(title + i18n.translate("command.colon"), NamedTextColor.GOLD)
                            .append(Component.text(value.toString(), NamedTextColor.GREEN))
            );
        } else {
            adventure.player(player).sendMessage(
                    Component.text(title + i18n.translate("command.colon") + i18n.translate("command.no_data"), NamedTextColor.GOLD)
            );
        }
    }

    private void clearScoreboard(Player player) {
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

    private void displayHelpMessage(Player player) {
        player.sendMessage("§3§l" + i18n.translate("command.help.header") + " §7by Chlna6666");
        player.sendMessage("§b/ranking place §f- §7" + i18n.translate("command.help.place"));
        player.sendMessage("§b/ranking destroys §f- §7" + i18n.translate("command.help.destroys"));
        player.sendMessage("§b/ranking deads §f- §7" + i18n.translate("command.help.deads"));
        player.sendMessage("§b/ranking mobdie §f- §7" + i18n.translate("command.help.mobdie"));
        player.sendMessage("§b/ranking onlinetime §f- §7" + i18n.translate("command.help.onlinetime"));
        player.sendMessage("§b/ranking break_bedrock §f- §7" + i18n.translate("command.help.break_bedrock"));
        player.sendMessage("§b/ranking all §f- §7" + i18n.translate("command.help.all"));
        player.sendMessage("§b/ranking my §f- §7" + i18n.translate("command.help.my"));
        player.sendMessage("§b/ranking list <ranking_name> §f- §7" + i18n.translate("command.help.list"));
        player.sendMessage("§b/ranking help §f- §7" + i18n.translate("command.help.help"));
    }
}
