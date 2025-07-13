package com.chlna6666.ranking.command;

import com.chlna6666.ranking.datamanager.DataManager;
import com.chlna6666.ranking.leaderboard.LeaderboardSettings;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RankingTabCompleter implements TabCompleter {

    private final LeaderboardSettings leaderboardSettings;

    public RankingTabCompleter() {
        this.leaderboardSettings = LeaderboardSettings.getInstance();
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> subCommands = new ArrayList<>();
        List<String> leaderboardCommands = DataManager.SUPPORTED_TYPES;

        boolean isAdmin = !(sender instanceof org.bukkit.entity.Player) || sender.isOp() || sender.hasPermission("ranking.reset");

        if (args.length == 1) {
            for (String cmd : leaderboardCommands) {
                if (leaderboardSettings.isLeaderboardEnabled(cmd)) {
                    subCommands.add(cmd);
                }
            }
            subCommands.addAll(Arrays.asList("all", "my", "list", "help", "dynamic"));
            if (isAdmin) {
                subCommands.add("reset");
            }
        } else if (args.length == 2) {
            if ((args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("reset"))) {
                for (String cmd : leaderboardCommands) {
                    if (leaderboardSettings.isLeaderboardEnabled(cmd)) {
                        subCommands.add(cmd);
                    }
                }
                if (args[0].equalsIgnoreCase("reset") && isAdmin) {
                    subCommands.add("all");
                }
            }
        }

        return subCommands;
    }


}
