package com.chlna6666.ranking;

import com.chlna6666.ranking.utils.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.chlna6666.ranking.utils.Utils.isFolia;
import static org.bukkit.Bukkit.getServer;

public class DataManager {
    private final Ranking plugin;
    private final AtomicBoolean saveTaskRunning = new AtomicBoolean(false);
    private ObjectNode playersData;
    private ObjectNode placeData;
    private ObjectNode destroysData;
    private ObjectNode deadsData;
    private ObjectNode mobdieData;
    private ObjectNode onlinetimeData;
    private ObjectNode breakBedrockData;
    private File dataFile;
    private File placeFile;
    private File destroysFile;
    private File deadsFile;
    private File mobdieFile;
    private File onlinetimeFile;
    private File breakBedrockFile;
    private final ObjectMapper objectMapper;

    public DataManager(Ranking plugin) {
        this.plugin = plugin;
        this.objectMapper = new ObjectMapper();
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
        breakBedrockFile = new File(dataFolder, "break_bedrock.json");

        playersData = initializeAndLoadJSON(dataFile);
        placeData = initializeAndLoadJSON(placeFile);
        destroysData = initializeAndLoadJSON(destroysFile);
        deadsData = initializeAndLoadJSON(deadsFile);
        mobdieData = initializeAndLoadJSON(mobdieFile);
        onlinetimeData = initializeAndLoadJSON(onlinetimeFile);
        breakBedrockData = initializeAndLoadJSON(breakBedrockFile);
    }

    private ObjectNode initializeAndLoadJSON(File file) {
        if (!file.exists() || file.length() == 0) {
            ObjectNode emptyNode = objectMapper.createObjectNode();
            saveJSON(emptyNode, file);
            return emptyNode;
        } else {
            return loadJSON(file);
        }
    }

    private ObjectNode loadJSON(File file) {
        try {
            return (ObjectNode) objectMapper.readTree(file);
        } catch (IOException e) {
            Bukkit.getLogger().warning("Error loading JSON from file " + file.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return objectMapper.createObjectNode();
        }
    }


    public void saveJSONAsync(ObjectNode json, File file) {
        if (isFolia()) {
            // 使用 Folia 的调度方式
            getServer().getGlobalRegionScheduler().run(plugin, task -> saveJSON(json, file));
        } else {
            // 使用 Bukkit 的异步调度方式
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveJSON(json, file));
        }
    }

    public void saveJSON(ObjectNode json, File file) {
        try {
            objectMapper.writeValue(file, json);
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
        saveJSON(breakBedrockData, breakBedrockFile);
    }

    public void startSaveTask(AtomicBoolean taskRunning, Runnable task) {
        if (taskRunning.compareAndSet(false, true)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                task.run();
                taskRunning.set(false);
            }, plugin.getSaveDelayTicks());
        }
    }

    public ObjectNode getPlayersData() {
        return playersData;
    }

    public ObjectNode getPlaceData() {
        return placeData;
    }

    public ObjectNode getDestroysData() {
        return destroysData;
    }

    public ObjectNode getDeadsData() {
        return deadsData;
    }

    public ObjectNode getMobdieData() {
        return  mobdieData;
    }

    public ObjectNode getOnlinetimeData() {
        return onlinetimeData;
    }

    public ObjectNode getBreakBedrockData() {
        return  breakBedrockData;
    }


    // File getters remain unchanged
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

    public File getBreakBedrockFile() {
        return breakBedrockFile;
    }
}