package com.chlna6666.ranking.scoreboard;

import com.chlna6666.ranking.datamanager.DataManager;
import com.chlna6666.ranking.I18n.I18n;
import com.chlna6666.ranking.Ranking;
import com.chlna6666.ranking.utils.Utils;
import fr.mrmicky.fastboard.FastBoard;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ScoreboardManager {
    private final Ranking plugin;
    private final DataManager dataManager;
    private final I18n i18n;
    private final DynamicScoreboard dynamicHandler;

    private final Map<UUID, FastBoard> boards = new ConcurrentHashMap<>();

    public ScoreboardManager(Ranking plugin, DataManager dataManager, I18n i18n) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.i18n = i18n;
        this.dynamicHandler = new DynamicScoreboard(plugin, dataManager, i18n);
    }


    public void updateBoard(Player player, String title, List<String> lines) {
        if (!player.isOnline()) return;

        FastBoard board = boards.computeIfAbsent(player.getUniqueId(), uuid -> new FastBoard(player));
        board.updateTitle(title);
        board.updateLines(lines);
    }


    public void removeBoard(Player player) {
        FastBoard board = boards.remove(player.getUniqueId());
        if (board != null) {
            board.delete();
        }

        if (player.isOnline()) {
            try {
                if (!Utils.isFolia()) {
                    org.bukkit.scoreboard.ScoreboardManager bukkitMgr = Bukkit.getScoreboardManager();
                    if (bukkitMgr != null) {
                        player.setScoreboard(bukkitMgr.getNewScoreboard());
                        player.setScoreboard(bukkitMgr.getMainScoreboard());
                    }
                } else {
                    try {
                        player.getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {
            }
        }
    }

    public void broadcastUpdate(String type, String title, List<String> lines) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isScoreboardEnabledFor(player, type)) {
                updateBoard(player, title, lines);
            }
        }
    }

    public void dynamicScoreboard(Player player) {
        dynamicHandler.toggle(player);
    }

    public void toggleScoreboard(Player player, String type) {
        updateScoreboardStatus(player, type);

        String title = ScoreboardUtils.getTitle(i18n, type);

        if (isScoreboardEnabledFor(player, type)) {
            player.sendMessage(title + " " + i18n.translate("command.enabled"));
            plugin.updateScoreboards(title, ScoreboardUtils.getData(dataManager, type), type);
        } else {
            player.sendMessage(title + " " + i18n.translate("command.disabled"));
            removeBoard(player);
        }
    }

    public void refreshAllScoreboards() {
        for (String type : DataManager.SUPPORTED_TYPES) {
            String title = ScoreboardUtils.getTitle(i18n, type);
            JSONObject data = ScoreboardUtils.getData(dataManager, type);
            plugin.updateScoreboards(title, data, type);
        }
    }

    public void refreshScoreboard(String type) {
        String title = ScoreboardUtils.getTitle(i18n, type);
        JSONObject data = ScoreboardUtils.getData(dataManager, type);
        plugin.updateScoreboards(title, data, type);
    }

    private boolean isScoreboardEnabledFor(Player player, String targetType) {
        JSONObject playerData = getPlayerData(player);
        if (playerData == null) return false;

        Object value = playerData.get(targetType);
        return value instanceof Number && ((Number) value).intValue() == 1;
    }

    private void updateScoreboardStatus(Player player, String type) {
        JSONObject playersData = dataManager.getPlayersData();
        JSONObject playerData = (JSONObject) playersData.get(player.getUniqueId().toString());
        if (playerData == null) return;

        Object currentVal = playerData.getOrDefault(type, 0L);
        boolean wasEnabled = (currentVal instanceof Number && ((Number) currentVal).intValue() == 1);

        for (String key : DataManager.SUPPORTED_TYPES) {
            playerData.put(key, 0L);
        }
        playerData.put("dynamic", 0L);

        if (!wasEnabled) {
            playerData.put(type, 1L);
        }

        dataManager.saveData("data", playersData);
    }

    private JSONObject getPlayerData(Player player) {
        return (JSONObject) dataManager.getPlayersData().get(player.getUniqueId().toString());
    }
}