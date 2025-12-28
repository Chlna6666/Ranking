package com.chlna6666.ranking.command;

import com.chlna6666.ranking.datamanager.DataManager;
import com.chlna6666.ranking.enums.LeaderboardType;
import com.chlna6666.ranking.I18n.I18n;
import com.chlna6666.ranking.leaderboard.LeaderboardSettings;
import com.chlna6666.ranking.Ranking;
import com.chlna6666.ranking.display.RankingDisplay;
import com.chlna6666.ranking.manager.RankingManager;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RankingCommand implements CommandExecutor {
    private final DataManager dataManager;
    private final I18n i18n;
    private final LeaderboardSettings settings;
    private final RankingManager rankingManager;
    private final RankingDisplay rankingDisplay;

    public RankingCommand(Ranking plugin, DataManager dataManager, I18n i18n, RankingManager rankingManager) {
        this.dataManager = dataManager;
        this.i18n = i18n;
        this.settings = LeaderboardSettings.getInstance();
        this.rankingManager = rankingManager;
        this.rankingDisplay = new RankingDisplay(dataManager, i18n, BukkitAudiences.create(plugin));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(i18n.translate("command.usage_ranking"));
            return true;
        }

        String sub = args[0].toLowerCase();

        // 1. 处理 Reset
        if (sub.equals("reset")) {
            if (!checkPerm(sender)) return true;
            if (args.length < 2) {
                sender.sendMessage(i18n.translate("command.usage_reset"));
                return true;
            }
            if (args[1].equalsIgnoreCase("all")) {
                dataManager.resetAll();
                sender.sendMessage(i18n.translate("command.reset_all_success"));
                rankingManager.refreshAll();
            } else {
                LeaderboardType type = LeaderboardType.fromString(args[1]);
                if (type != null) {
                    dataManager.resetLeaderboard(type.getId());
                    sender.sendMessage(i18n.translate("command.reset_one_success", type.getId()));
                    rankingManager.updateScoreboards(type); // 假设Manager有此方法
                } else {
                    sender.sendMessage(i18n.translate("command.unknown_ranking"));
                }
            }
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(i18n.translate("command.only_players"));
            return true;
        }

        // 2. 处理通用命令
        switch (sub) {
            case "all" -> { rankingDisplay.displayAllRankings(player); return true; }
            case "my" -> { rankingDisplay.displayPlayerRankings(player); return true; }
            case "help" -> { rankingDisplay.displayHelpMessage(player); return true; }
            case "dynamic" -> { rankingManager.toggleDynamic(player); return true; }
            case "list" -> {
                if (args.length > 1) rankingDisplay.handleSingleRanking(player, args[1]);
                else player.sendMessage(i18n.translate("command.usage_list"));
                return true;
            }
        }

        // 3. 处理排行榜开关
        LeaderboardType type = LeaderboardType.fromString(sub);
        if (type != null && settings.isLeaderboardEnabled(type.getId())) {
            rankingManager.toggleScoreboard(player, type);
        } else {
            player.sendMessage(i18n.translate("command.unknown_command"));
        }

        return true;
    }

    private boolean checkPerm(CommandSender sender) {
        if (!(sender instanceof Player) || sender.isOp() || sender.hasPermission("ranking.reset")) return true;
        sender.sendMessage(i18n.translate("command.no_permission"));
        return false;
    }
}