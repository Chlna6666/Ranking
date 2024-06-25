package com.chlna6666.ranking.updatechecker;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
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
    private String latestVersion;
    private String viewUrl;

    public UpdateChecker(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public void checkForUpdates() {
        if (!config.getBoolean("update_checker.enabled")) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000); // 设置连接超时为5秒
                connection.setReadTimeout(5000);    // 设置读取超时为5秒

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    logWarning("连接更新服务器失败。响应代码: " + responseCode);
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

                if (!plugin.getDescription().getVersion().equals(latestVersion)) {
                    String message = ChatColor.GOLD + "[Ranking] " + ChatColor.GREEN + "发现新版本: " + latestVersion +
                            ChatColor.GOLD + " 下载链接: " + ChatColor.AQUA + viewUrl;
                    Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getConsoleSender().sendMessage(message));
                }
            } catch (SocketException e) {
                logWarning("连接重置。请检查您的网络连接。");
            } catch (Exception e) {
                plugin.getLogger().severe("[Ranking] 更新检查器: 无法检查更新: " + e.getMessage());
            }
        });
    }

    public void notifyAdminIfUpdateAvailable(CommandSender sender) {
        if (latestVersion != null && !plugin.getDescription().getVersion().equals(latestVersion)) {
            String message = ChatColor.GOLD + "[Ranking] " + ChatColor.GREEN + "发现新版本: " + latestVersion +
                    ChatColor.GOLD + " 下载链接: " + ChatColor.AQUA + viewUrl;
            sender.sendMessage(message);
        }
    }

    private void logWarning(String message) {
        Bukkit.getLogger().warning("更新检查器: " + message);
    }
}
