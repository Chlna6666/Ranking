package com.chlna6666.ranking;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class RankingTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> subCommands = new ArrayList<>();

        if (args.length == 1) {
            subCommands.add("place");
            subCommands.add("destroys");
            subCommands.add("deads");
            subCommands.add("mobdie");
            subCommands.add("onlinetime");
            subCommands.add("break_bedrock");
            subCommands.add("all");
            subCommands.add("my");
            subCommands.add("list");
            subCommands.add("help");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("list")) {
            subCommands.add("place");
            subCommands.add("destroys");
            subCommands.add("deads");
            subCommands.add("mobdie");
            subCommands.add("onlinetime");
        }

        return subCommands;
    }
}
