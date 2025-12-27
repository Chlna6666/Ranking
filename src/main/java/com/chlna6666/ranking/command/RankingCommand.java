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

        // 关键修改：获取单例
        this.scoreboardManager = plugin.getScoreboardManager();

        this.rankingDisplay = new RankingDisplay(dataManager, i18n, adventure);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(i18n.translate("command.usage_ranking"));
            return true;
        }

        if (args[0].equalsIgnoreCase("reset")) {
            boolean hasPermission = !(sender instanceof Player) || sender.isOp() || sender.hasPermission("ranking.reset");

            if (!hasPermission) {
                sender.sendMessage(i18n.translate("command.no_permission"));
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage(i18n.translate("command.usage_reset"));
                return true;
            }

            String type = args[1].toLowerCase();

            if (type.equals("all")) {
                dataManager.resetAll();
                sender.sendMessage(i18n.translate("command.reset_all_success"));
                scoreboardManager.refreshAllScoreboards();
            } else if (DataManager.SUPPORTED_TYPES.contains(type)) {
                dataManager.resetLeaderboard(type);
                sender.sendMessage(i18n.translate("command.reset_one_success", type));
                scoreboardManager.refreshScoreboard(type);
            } else {
                sender.sendMessage(i18n.translate("command.unknown_ranking"));
            }

            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(i18n.translate("command.only_players"));
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
            case "all": rankingDisplay.displayAllRankings(player); break;
            case "my": rankingDisplay.displayPlayerRankings(player); break;
            case "list":
                if (args.length > 1) rankingDisplay.handleSingleRanking(player, args[1]);
                else player.sendMessage(i18n.translate("command.usage_list"));
                break;
            case "help": rankingDisplay.displayHelpMessage(player); break;
            case "dynamic": scoreboardManager.dynamicScoreboard(player); break;
            default: player.sendMessage(i18n.translate("command.unknown_command")); break;
        }
    }

    private void handleLeaderboardCommand(Player player, String[] args) {
        String type = args[0].toLowerCase();
        if (!DataManager.SUPPORTED_TYPES.contains(type) || !leaderboardSettings.isLeaderboardEnabled(type)) {
            player.sendMessage(i18n.translate("command.unknown_ranking"));
            return;
        }
        scoreboardManager.toggleScoreboard(player, type);
    }
}