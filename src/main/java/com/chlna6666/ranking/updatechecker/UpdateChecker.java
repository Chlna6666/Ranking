package com.chlna6666.ranking.updatechecker;

import com.chlna6666.ranking.Ranking;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;


public class UpdateChecker {
    private final JavaPlugin plugin;
    private final String apiUrl = "https://api.minebbs.com/api/openapi/v1/resources/7531";
    private final FileConfiguration config;
    private boolean warningSent = false;
    private String latestVersion;
    private String viewUrl;

    public UpdateChecker(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public void checkForUpdates(CommandSender sender) {
        if (!config.getBoolean("update_checker.enabled")) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    logWarning(((Ranking) plugin).getI18n().translate("update_checker.connection_failed").replace("{code}", String.valueOf(responseCode)));
                    return;
                }

                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONParser parser = new JSONParser();
                JSONObject json = (JSONObject) parser.parse(response.toString());
                JSONObject data = (JSONObject) json.get("data");
                latestVersion = (String) data.get("version");
                viewUrl = (String) data.get("view_url");

                Bukkit.getScheduler().runTask(plugin, () -> handleUpdateCheckResult(sender));
            } catch (SocketException e) {
                logWarning(((Ranking) plugin).getI18n().translate("update_checker.connection_reset"));
            } catch (Exception e) {
                plugin.getLogger().severe(((Ranking) plugin).getI18n().translate("update_checker.error_checking_updates") + ": " + e.getMessage());
            }
        });
    }

    private void handleUpdateCheckResult(CommandSender sender) {
        String currentVersion = plugin.getDescription().getVersion();
        Component message;

        if (isVersionHigher(latestVersion, currentVersion)) {
            message = Component.text("[Ranking] ", NamedTextColor.GOLD)
                    .append(Component.text(((Ranking) plugin).getI18n().translate("update_checker.new_version_found") + ": " + latestVersion, NamedTextColor.GREEN))
                    .append(Component.text("  " + ((Ranking) plugin).getI18n().translate("update_checker.download_link") + ": ", NamedTextColor.GOLD))
                    .append(Component.text(viewUrl, NamedTextColor.AQUA));
        } else if (latestVersion.equals(currentVersion)) {
            message = Component.text("[Ranking] ", NamedTextColor.GOLD)
                    .append(Component.text(((Ranking) plugin).getI18n().translate("update_checker.latest_version_installed"), NamedTextColor.GREEN));
        } else {
            message = Component.text("[Ranking] ", NamedTextColor.GOLD)
                    .append(Component.text(((Ranking) plugin).getI18n().translate("update_checker.beta_version_installed") + ": " + plugin.getDescription().getVersion(), NamedTextColor.RED))
                    .append(Component.text("  " + ((Ranking) plugin).getI18n().translate("update_checker.backup_warning"), NamedTextColor.GOLD));
        }

        // 发送消息给玩家或控制台
        if (sender instanceof Player) {
            sender.sendMessage(message);
        } else {
            Bukkit.getConsoleSender().sendMessage(message);
        }
    }

    private boolean isVersionHigher(String remoteVersion, String currentVersion) {
        String[] remoteParts = remoteVersion.replace("v", "").split("\\.");
        String[] currentParts = currentVersion.replace("v", "").split("\\.");

        int length = Math.max(remoteParts.length, currentParts.length);
        for (int i = 0; i < length; i++) {
            int remotePart = i < remoteParts.length ? Integer.parseInt(remoteParts[i]) : 0;
            int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;

            if (remotePart > currentPart) {
                return true;
            } else if (remotePart < currentPart) {
                return false;
            }
        }
        return false;
    }

    private void logWarning(String message) {
        if (!warningSent) {
            Bukkit.getLogger().warning(((Ranking) plugin).getI18n().translate("update_checker.update_checker") + message);
            warningSent = true;
            // 在一分钟后重置标志位
            Bukkit.getScheduler().runTaskLater(plugin, () -> warningSent = false, 1200L); // 1200 ticks = 1 minute
        }
    }
}
