package com.chlna6666.ranking.command;

import com.chlna6666.ranking.datamanager.DataManager;
import com.chlna6666.ranking.I18n.I18n;
import com.chlna6666.ranking.leaderboard.LeaderboardSettings;
import com.chlna6666.ranking.Ranking;
import com.chlna6666.ranking.display.RankingDisplay;
import com.chlna6666.ranking.scoreboard.ScoreboardManager;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;

public class RankingCommand implements CommandExecutor {
    private final DataManager dataManager;
    private final I18n i18n;
    private final LeaderboardSettings leaderboardSettings;
    private final ScoreboardManager scoreboardManager;
    private final RankingDisplay rankingDisplay;

    public RankingCommand(Ranking plugin, DataManager dataManager, I18n i18n) {
        this.dataManager = dataManager;
        this.i18n = i18n;
        BukkitAudiences adventure = BukkitAudiences.create(plugin);
        this.leaderboardSettings = LeaderboardSettings.getInstance();
        this.scoreboardManager = new ScoreboardManager(plugin, dataManager, i18n);
        this.rankingDisplay = new RankingDisplay(dataManager, i18n, adventure);
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

        // 判断是否为排行榜命令
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
                rankingDisplay.displayAllRankings(player);
                break;
            case "my":
                rankingDisplay.displayPlayerRankings(player);
                break;
            case "list":
                if (args.length > 1) {
                    rankingDisplay.handleSingleRanking(player, args[1]);
                } else {
                    player.sendMessage(i18n.translate("command.usage_list"));
                }
                break;
            case "help":
                rankingDisplay.displayHelpMessage(player);
                break;
            case "dynamic":
                scoreboardManager.dynamicScoreboard(player, "dynamic", i18n.translate("sidebar.dynamic"), dataManager.getPlayersData());
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
                scoreboardManager.toggleScoreboard(player, rankingName, i18n.translate("sidebar.place"), dataManager.getPlaceData());
                break;
            case "destroys":
                scoreboardManager.toggleScoreboard(player, rankingName, i18n.translate("sidebar.break"), dataManager.getDestroysData());
                break;
            case "deads":
                scoreboardManager.toggleScoreboard(player, rankingName, i18n.translate("sidebar.death"), dataManager.getDeadsData());
                break;
            case "mobdie":
                scoreboardManager.toggleScoreboard(player, rankingName, i18n.translate("sidebar.kill"), dataManager.getMobdieData());
                break;
            case "onlinetime":
                scoreboardManager.toggleScoreboard(player, rankingName, i18n.translate("sidebar.online_time"), dataManager.getOnlinetimeData());
                break;
            case "break_bedrock":
                scoreboardManager.toggleScoreboard(player, rankingName, i18n.translate("sidebar.break_bedrock"), dataManager.getBreakBedrockData());
                break;
            default:
                player.sendMessage(i18n.translate("command.unknown_ranking"));
                break;
        }
    }


}
