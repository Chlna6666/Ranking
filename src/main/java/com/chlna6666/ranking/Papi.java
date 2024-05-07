package com.chlna6666.ranking;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;

import java.util.*;

public class Papi extends PlaceholderExpansion {
    private Ranking pluginInstance;

    public Papi(Ranking pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    @Override
    public String getAuthor() {
        return String.join(", ", pluginInstance.getDescription().getAuthors());
    }

    @Override
    public String getIdentifier() {
        return "ranking";
    }

    @Override
    public String getVersion() {
        return pluginInstance.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        boolean result = pluginInstance != null;
        if (!result) {
            Bukkit.getLogger().warning("[Papi] pluginInstance is null! Placeholder will not be registered.");
        }
        return result;
    }


    @Override

    public String onRequest(OfflinePlayer player, String params) {
        if (params.equalsIgnoreCase("place") && pluginInstance != null) {
            JSONObject jsonData = pluginInstance.getplaceData();
            if (jsonData != null && player != null) {
                String playerUUID = player.getUniqueId().toString();
                Object playerPlaceCount = jsonData.get(playerUUID);
                if (playerPlaceCount != null) {
                    return String.valueOf(playerPlaceCount);
                }
            }
        }
     /* 1
     if (params.equalsIgnoreCase("allplace") && pluginInstance != null) {
            JSONObject jsonData = pluginInstance.getplaceData();
            if (jsonData != null) {
                StringBuilder result = new StringBuilder();
                for (Object key : jsonData.keySet()) {
                    String playerUUID = (String) key;
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(playerUUID));
                    if (offlinePlayer != null) {
                        String playerName = offlinePlayer.getName();
                        Object playerPlaceCount = jsonData.get(playerUUID);
                        if (playerPlaceCount != null) {
                            result.append(playerName).append(": ").append(playerPlaceCount).append(" ");
                        }
                    }
                }
                return result.toString().trim();
            }
        }
        */
        JSONObject placeData = pluginInstance.getplaceData();

        if (placeData != null) {
            List<Map.Entry<String, Object>> sortedEntries = new ArrayList<>(placeData.entrySet());

            Collections.sort(sortedEntries, (a, b) -> {
                int placeA = Integer.parseInt(a.getValue().toString());
                int placeB = Integer.parseInt(b.getValue().toString());
                return Integer.compare(placeA, placeB);
            });

            int totalPlayers = sortedEntries.size();
            OfflinePlayer currentPlayer = Bukkit.getOfflinePlayer(player.getUniqueId());

            for (int i = totalPlayers; i > 0; i--) {
                Map.Entry<String, Object> entry = sortedEntries.get(totalPlayers - i);
                String uuidKey = entry.getKey();
                Object placeCount = entry.getValue();

                if (params.equalsIgnoreCase("place_" + i)) {
                    OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuidKey));
                    if (targetPlayer != null) {
                        String playerName = targetPlayer.getName();
                        String placeDataStr = String.valueOf(placeCount);
                        if (targetPlayer.equals(currentPlayer)) {
                            placeDataStr += " (我)";
                        }
                        return playerName + ": " + placeDataStr;
                    }
                }
            }

            if (params.startsWith("place_")) {
                return "";
            }
        }



        if (params.equalsIgnoreCase("destroys") && pluginInstance != null) {
            JSONObject jsonData = pluginInstance.getdestroysData();
            if (jsonData != null && player != null) {
                String playerUUID = player.getUniqueId().toString();
                Object playerPlaceCount = jsonData.get(playerUUID);
                if (playerPlaceCount != null) {
                    return String.valueOf(playerPlaceCount);
                }
            }
        }

        JSONObject destroysData = pluginInstance.getdestroysData();

        if (destroysData != null) {
            List<Map.Entry<String, Object>> sortedEntries = new ArrayList<>(destroysData.entrySet());

            Collections.sort(sortedEntries, (a, b) -> {
                int destroysA = Integer.parseInt(a.getValue().toString());
                int destroysB = Integer.parseInt(b.getValue().toString());
                return Integer.compare(destroysA, destroysB);
            });

            int totalPlayers = sortedEntries.size();
            OfflinePlayer currentPlayer = Bukkit.getOfflinePlayer(player.getUniqueId());

            for (int i = totalPlayers; i > 0; i--) {
                Map.Entry<String, Object> entry = sortedEntries.get(totalPlayers - i);
                String uuidKey = entry.getKey();
                Object placeCount = entry.getValue();

                if (params.equalsIgnoreCase("destroys_" + i)) {
                    OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuidKey));
                    if (targetPlayer != null) {
                        String playerName = targetPlayer.getName();
                        String destroysDataStr = String.valueOf(placeCount);
                        if (targetPlayer.equals(currentPlayer)) {
                            destroysDataStr += " (我)";
                        }
                        return playerName + ": " + destroysDataStr;
                    }
                }
            }

            if (params.startsWith("destroys_")) {
                return "";
            }
        }


        if (params.equalsIgnoreCase("deads") && pluginInstance != null) {
            JSONObject jsonData = pluginInstance.getdeadsData();
            if (jsonData != null && player != null) {
                String playerUUID = player.getUniqueId().toString();
                Object playerPlaceCount = jsonData.get(playerUUID);
                if (playerPlaceCount != null) {
                    return String.valueOf(playerPlaceCount);
                }
            }
        }

        JSONObject deadsData = pluginInstance.getdeadsData();

        if (deadsData != null) {
            List<Map.Entry<String, Object>> sortedEntries = new ArrayList<>(deadsData.entrySet());

            Collections.sort(sortedEntries, (a, b) -> {
                int deadsA = Integer.parseInt(a.getValue().toString());
                int deadsB = Integer.parseInt(b.getValue().toString());
                return Integer.compare(deadsA, deadsB);
            });

            int totalPlayers = sortedEntries.size();
            OfflinePlayer currentPlayer = Bukkit.getOfflinePlayer(player.getUniqueId());

            for (int i = totalPlayers; i > 0; i--) {
                Map.Entry<String, Object> entry = sortedEntries.get(totalPlayers - i);
                String uuidKey = entry.getKey();
                Object placeCount = entry.getValue();

                if (params.equalsIgnoreCase("deads_" + i)) {
                    OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuidKey));
                    if (targetPlayer != null) {
                        String playerName = targetPlayer.getName();
                        String deadsDataStr = String.valueOf(placeCount);
                        if (targetPlayer.equals(currentPlayer)) {
                            deadsDataStr += " (我)";
                        }
                        return playerName + ": " + deadsDataStr;
                    }
                }
            }

            if (params.startsWith("deads_")) {
                return "";
            }
        }

        if (params.equalsIgnoreCase("mobdie") && pluginInstance != null) {
            JSONObject jsonData = pluginInstance.getmobdieData();
            if (jsonData != null && player != null) {
                String playerUUID = player.getUniqueId().toString();
                Object playerPlaceCount = jsonData.get(playerUUID);
                if (playerPlaceCount != null) {
                    return String.valueOf(playerPlaceCount);
                }
            }
        }

        JSONObject mobdieData = pluginInstance.getmobdieData();

        if (mobdieData != null) {
            List<Map.Entry<String, Object>> sortedEntries = new ArrayList<>(placeData.entrySet());

            Collections.sort(sortedEntries, (a, b) -> {
                int mobdieA = Integer.parseInt(a.getValue().toString());
                int mobdieB = Integer.parseInt(b.getValue().toString());
                return Integer.compare(mobdieA, mobdieB);
            });

            int totalPlayers = sortedEntries.size();
            OfflinePlayer currentPlayer = Bukkit.getOfflinePlayer(player.getUniqueId());

            for (int i = totalPlayers; i > 0; i--) {
                Map.Entry<String, Object> entry = sortedEntries.get(totalPlayers - i);
                String uuidKey = entry.getKey();
                Object placeCount = entry.getValue();

                if (params.equalsIgnoreCase("mobdie_" + i)) {
                    OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuidKey));
                    if (targetPlayer != null) {
                        String playerName = targetPlayer.getName();
                        String mobdieDataStr = String.valueOf(placeCount);
                        if (targetPlayer.equals(currentPlayer)) {
                            mobdieDataStr += " (我)";
                        }
                        return playerName + ": " + mobdieDataStr;
                    }
                }
            }

            if (params.startsWith("mobdie_")) {
                return "";
            }
        }

        if (params.equalsIgnoreCase("onlinetime") && pluginInstance != null) {
            JSONObject jsonData = pluginInstance.getonlinetimeData();
            if (jsonData != null && player != null) {
                String playerUUID = player.getUniqueId().toString();
                Object playerPlaceCount = jsonData.get(playerUUID);
                if (playerPlaceCount != null) {
                    return String.valueOf(playerPlaceCount);
                }
            }
        }

        JSONObject onlinetimeData = pluginInstance.getonlinetimeData();

        if (onlinetimeData != null) {
            List<Map.Entry<String, Object>> sortedEntries = new ArrayList<>(onlinetimeData.entrySet());

            Collections.sort(sortedEntries, (a, b) -> {
                int onlinetimeA = Integer.parseInt(a.getValue().toString());
                int onlinetimeB = Integer.parseInt(b.getValue().toString());
                return Integer.compare(onlinetimeA, onlinetimeB);
            });

            int totalPlayers = sortedEntries.size();
            OfflinePlayer currentPlayer = Bukkit.getOfflinePlayer(player.getUniqueId());

            for (int i = totalPlayers; i > 0; i--) {
                Map.Entry<String, Object> entry = sortedEntries.get(totalPlayers - i);
                String uuidKey = entry.getKey();
                Object placeCount = entry.getValue();

                if (params.equalsIgnoreCase("onlinetime_" + i)) {
                    OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuidKey));
                    if (targetPlayer != null) {
                        String playerName = targetPlayer.getName();
                        String onlinetimeDataStr = String.valueOf(placeCount);
                        if (targetPlayer.equals(currentPlayer)) {
                            onlinetimeDataStr += " (我)";
                        }
                        return playerName + ": " + onlinetimeDataStr;
                    }
                }
            }

            if (params.startsWith("onlinetime_")) {
                return "";
            }
        }


        return null;
    }
}
