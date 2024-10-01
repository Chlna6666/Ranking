package com.chlna6666.ranking;

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
        List<String> leaderboardCommands = Arrays.asList("place", "destroys", "deads", "mobdie", "onlinetime", "break_bedrock");

        if (args.length == 1) {
            for (String cmd : leaderboardCommands) {
                if (leaderboardSettings.isLeaderboardEnabled(cmd)) {
                    subCommands.add(cmd);
                }
            }
            subCommands.addAll(Arrays.asList("all", "my", "list", "help"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("list")) {
            for (String cmd : leaderboardCommands) {
                if (leaderboardSettings.isLeaderboardEnabled(cmd)) {
                    subCommands.add(cmd);
                }
            }
        }

        return subCommands;
    }
}
