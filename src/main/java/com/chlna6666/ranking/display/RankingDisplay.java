package com.chlna6666.ranking.display;

import com.chlna6666.ranking.datamanager.DataManager;
import com.chlna6666.ranking.I18n.I18n;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;

import java.util.UUID;

public class RankingDisplay {
    private final DataManager dataManager;
    private final I18n i18n;
    private final BukkitAudiences adventure;

    public RankingDisplay(DataManager dataManager, I18n i18n, BukkitAudiences adventure) {
        this.dataManager = dataManager;
        this.i18n = i18n;
        this.adventure = adventure;
    }

    public void displayAllRankings(Player player) {
        player.sendMessage(Component.text(i18n.translate("command.all_rankings"), NamedTextColor.GOLD));
        displayRankingData(player, i18n.translate("sidebar.place"), dataManager.getPlaceData());
        displayRankingData(player, i18n.translate("sidebar.break"), dataManager.getDestroysData());
        displayRankingData(player, i18n.translate("sidebar.death"), dataManager.getDeadsData());
        displayRankingData(player, i18n.translate("sidebar.kill"), dataManager.getMobdieData());
        displayRankingData(player, i18n.translate("sidebar.online_time"), dataManager.getOnlinetimeData());
        displayRankingData(player, i18n.translate("sidebar.break_bedrock"), dataManager.getBreakBedrockData());
    }

    public void displayPlayerRankings(Player player) {
        UUID uuid = player.getUniqueId();
        player.sendMessage(Component.text(i18n.translate("command.your_rankings"), NamedTextColor.GOLD));
        displayPlayerData(player, i18n.translate("sidebar.place"), dataManager.getPlaceData(), uuid);
        displayPlayerData(player, i18n.translate("sidebar.break"), dataManager.getDestroysData(), uuid);
        displayPlayerData(player, i18n.translate("sidebar.death"), dataManager.getDeadsData(), uuid);
        displayPlayerData(player, i18n.translate("sidebar.kill"), dataManager.getMobdieData(), uuid);
        displayPlayerData(player, i18n.translate("sidebar.online_time"), dataManager.getOnlinetimeData(), uuid);
        displayPlayerData(player, i18n.translate("sidebar.break_bedrock"), dataManager.getBreakBedrockData(), uuid);
    }

    public void handleSingleRanking(Player player, String rankingName) {
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

    public void displayHelpMessage(Player player) {
        player.sendMessage("§3§l" + i18n.translate("command.help.header") + " §7by Chlna6666");
        player.sendMessage("§b/ranking place §f- §7" + i18n.translate("command.help.place"));
        player.sendMessage("§b/ranking destroys §f- §7" + i18n.translate("command.help.destroys"));
        player.sendMessage("§b/ranking deads §f- §7" + i18n.translate("command.help.deads"));
        player.sendMessage("§b/ranking mobdie §f- §7" + i18n.translate("command.help.mobdie"));
        player.sendMessage("§b/ranking onlinetime §f- §7" + i18n.translate("command.help.onlinetime"));
        player.sendMessage("§b/ranking break_bedrock §f- §7" + i18n.translate("command.help.break_bedrock"));
        player.sendMessage("§b/ranking dynamic §f- §7" + i18n.translate("command.help.dynamic"));
        player.sendMessage("§b/ranking all §f- §7" + i18n.translate("command.help.all"));
        player.sendMessage("§b/ranking my §f- §7" + i18n.translate("command.help.my"));
        player.sendMessage("§b/ranking list <ranking_name> §f- §7" + i18n.translate("command.help.list"));
        player.sendMessage("§b/ranking help §f- §7" + i18n.translate("command.help.help"));
    }
}
