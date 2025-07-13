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
        if (args.length == 0) {
            sender.sendMessage(i18n.translate("command.usage_ranking"));
            return true;
        }

        // 管理员指令: /ranking reset <type|all>
        if (args[0].equalsIgnoreCase("reset")) {
            // 控制台 or 玩家是OP or 玩家有 ranking.reset 权限
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
                scoreboardManager.refreshAllScoreboards(); // 刷新全部榜单
            } else if (DataManager.SUPPORTED_TYPES.contains(type)) {
                dataManager.resetLeaderboard(type);
                sender.sendMessage(i18n.translate("command.reset_one_success", type));
                scoreboardManager.refreshScoreboard(type); // 刷新指定榜单
            } else {
                sender.sendMessage(i18n.translate("command.unknown_ranking"));
            }

            return true;
        }

        // 如果是控制台，必须是 reset，否则不允许操作
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
                scoreboardManager.dynamicScoreboard(player);
                break;
            default:
                player.sendMessage(i18n.translate("command.unknown_command"));
                break;
        }
    }

    // public void toggleScoreboard(Player player, String type)
    private void handleLeaderboardCommand(Player player, String[] args) {
        String type = args[0].toLowerCase();

        // 不是支持的排行榜类型
        if (!DataManager.SUPPORTED_TYPES.contains(type)
                || !leaderboardSettings.isLeaderboardEnabled(type)) {
            player.sendMessage(i18n.translate("command.unknown_ranking"));
            return;
        }

        // 直接调新方法
        scoreboardManager.toggleScoreboard(player, type);
    }

}
