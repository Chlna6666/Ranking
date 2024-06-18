package com.chlna6666.ranking;

import org.bukkit.Bukkit;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class DataManager {
    private final Ranking plugin;
    private final AtomicBoolean saveTaskRunning = new AtomicBoolean(false);
    private JSONObject playersData;
    private JSONObject placeData;
    private JSONObject destroysData;
    private JSONObject deadsData;
    private JSONObject mobdieData;
    private JSONObject onlinetimeData;
    private File dataFile;
    private File placeFile;
    private File destroysFile;
    private File deadsFile;
    private File mobdieFile;
    private File onlinetimeFile;

    public DataManager(Ranking plugin) {
        this.plugin = plugin;
        loadFiles();
    }

    private void loadFiles() {
        String storageLocation = plugin.getConfig().getString("data_storage.location");
        String serverDirectory = System.getProperty("user.dir");
        File dataFolder = new File(serverDirectory, storageLocation);

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        dataFile = new File(dataFolder, "data.json");
        placeFile = new File(dataFolder, "place.json");
        destroysFile = new File(dataFolder, "destroys.json");
        deadsFile = new File(dataFolder, "deads.json");
        mobdieFile = new File(dataFolder, "mobdie.json");
        onlinetimeFile = new File(dataFolder, "onlinetime.json");

        playersData = initializeAndLoadJSON(dataFile);
        placeData = initializeAndLoadJSON(placeFile);
        destroysData = initializeAndLoadJSON(destroysFile);
        deadsData = initializeAndLoadJSON(deadsFile);
        mobdieData = initializeAndLoadJSON(mobdieFile);
        onlinetimeData = initializeAndLoadJSON(onlinetimeFile);
    }

    private JSONObject initializeAndLoadJSON(File file) {
        if (!file.exists() || file.length() == 0) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("{}");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return new JSONObject();
        } else {
            return loadJSON(file);
        }
    }

    private JSONObject loadJSON(File file) {
        JSONParser parser = new JSONParser();
        try (FileReader reader = new FileReader(file)) {
            Object parsedObject = parser.parse(reader);
            if (parsedObject instanceof JSONObject) {
                return (JSONObject) parsedObject;
            } else {
                Bukkit.getLogger().warning("Error loading JSON from file " + file.getName() + ": The root element is not a JSON object.");
                return new JSONObject();
            }
        } catch (IOException | ParseException e) {
            Bukkit.getLogger().warning("Error loading JSON from file " + file.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return new JSONObject();
        }
    }

    public void saveJSONAsync(JSONObject json, File file) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveJSON(json, file));
    }

    public void saveJSON(JSONObject json, File file) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(json.toJSONString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveAllData() {
        saveJSON(playersData, dataFile);
        saveJSON(placeData, placeFile);
        saveJSON(destroysData, destroysFile);
        saveJSON(deadsData, deadsFile);
        saveJSON(mobdieData, mobdieFile);
        saveJSON(onlinetimeData, onlinetimeFile);
    }

    public void startSaveTask(AtomicBoolean taskRunning, Runnable task) {
        if (taskRunning.compareAndSet(false, true)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                task.run();
                taskRunning.set(false);
            }, plugin.getSaveDelayTicks());
        }
    }

    public JSONObject getPlayersData() {
        return playersData;
    }

    public JSONObject getPlaceData() {
        return placeData;
    }

    public JSONObject getDestroysData() {
        return destroysData;
    }

    public JSONObject getDeadsData() {
        return deadsData;
    }

    public JSONObject getMobdieData() {
        return mobdieData;
    }

    public JSONObject getOnlinetimeData() {
        return onlinetimeData;
    }

    public File getDataFile() {
        return dataFile;
    }

    public File getPlaceFile() {
        return placeFile;
    }

    public File getDestroysFile() {
        return destroysFile;
    }

    public File getDeadsFile() {
        return deadsFile;
    }

    public File getMobdieFile() {
        return mobdieFile;
    }

    public File getOnlinetimeFile() {
        return onlinetimeFile;
    }
}
