package com.chlna6666.ranking.datamanager;

import com.chlna6666.ranking.I18n.I18n;
import com.chlna6666.ranking.Ranking;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class JsonDataManager extends DataManager {
    private final I18n i18n ;

    private File dataFile;
    private File placeFile;
    private File destroysFile;
    private File deadsFile;
    private File mobdieFile;
    private File onlinetimeFile;
    private File breakBedrockFile;

    public JsonDataManager(Ranking plugin) {
        super(plugin);
        this.i18n = plugin.getI18n();
        plugin.getLogger().info(i18n.translate("data.use_json"));
        initialize();
    }

    private void initialize() {
        if (isJsonStorage()) {
            loadFiles();
        } else {
            initializeEmptyData();
        }
    }

    @Override
    protected void loadFiles() {
        String storageLocation = plugin.getConfig().getString("data_storage.location");
        String serverDirectory = System.getProperty("user.dir");
        File dataFolder = null;
        if (storageLocation != null) {
            dataFolder = new File(serverDirectory, storageLocation);
        }

        // 确保数据目录存在
        if (dataFolder != null && !dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().severe(i18n.translate("data.create_folder_failed") + dataFolder.getAbsolutePath());
            return;
        }

        // 先初始化所有文件路径
        dataFile = initFile(dataFolder, "data.json");
        placeFile = initFile(dataFolder, "place.json");
        destroysFile = initFile(dataFolder, "destroys.json");
        deadsFile = initFile(dataFolder, "deads.json");
        mobdieFile = initFile(dataFolder, "mobdie.json");
        onlinetimeFile = initFile(dataFolder, "onlinetime.json");
        breakBedrockFile = initFile(dataFolder, "break_bedrock.json");

        // 然后加载数据
        playersData = loadJSON(dataFile);
        placeData = loadJSON(placeFile);
        destroysData = loadJSON(destroysFile);
        deadsData = loadJSON(deadsFile);
        mobdieData = loadJSON(mobdieFile);
        onlinetimeData = loadJSON(onlinetimeFile);
        breakBedrockData = loadJSON(breakBedrockFile);
    }

    private File initFile(File parent, String filename) {
        File file = new File(parent, filename);
        try {
            if (!file.exists()) {
                if (file.createNewFile()) {
                    // 初始化空JSON文件
                    try (FileWriter writer = new FileWriter(file)) {
                        writer.write("{}");
                        plugin.getLogger().info(i18n.translate("data.json.init_file") + file.getAbsolutePath());
                    }
                }
            }
            return file;
        } catch (IOException e) {
            plugin.getLogger().severe(i18n.translate("data.json.create_file_failed") + file.getAbsolutePath());
            return null;
        }
    }

    private JSONObject loadJSON(File file) {
        if (file == null || !file.exists()) {
            plugin.getLogger().warning(i18n.translate("data.json.file_not_exist") + (file != null ? file.getName() : "null"));
            return new JSONObject();
        }

        JSONParser parser = new JSONParser();
        try (FileReader reader = new FileReader(file)) {
            Object parsed = parser.parse(reader);
            if (parsed instanceof JSONObject) {
                return (JSONObject) parsed;
            }
            plugin.getLogger().warning(i18n.translate("data.json.invalid_json") + file.getName());
        } catch (IOException | ParseException e) {
            plugin.getLogger().warning(i18n.translate("data.json.load_failed") + file.getName() + " - " + e.getMessage());
        }
        return new JSONObject();
    }

    @Override
    public void saveData(String dataType, JSONObject data) {
        File targetFile = getFileByDataType(dataType);
        if (targetFile != null) {
            saveJSON(data, targetFile);
        } else {
            plugin.getLogger().warning(i18n.translate("data.json.unknown_type") + dataType);
        }
    }

    @Override
    public void saveAllData() {
        saveJSON(playersData, dataFile);
        saveJSON(placeData, placeFile);
        saveJSON(destroysData, destroysFile);
        saveJSON(deadsData, deadsFile);
        saveJSON(mobdieData, mobdieFile);
        saveJSON(onlinetimeData, onlinetimeFile);
        saveJSON(breakBedrockData, breakBedrockFile);
    }

    private void saveJSON(JSONObject json, File file) {
        if (file == null) {
            plugin.getLogger().warning(i18n.translate("data.json.null_path"));
            return;
        }

        try {
            Path path = file.toPath();
            String data = json.toJSONString();
            Files.writeString(path, data, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            plugin.getLogger().severe(i18n.translate("data.json.save_failed") + file.getName() + " - " + e.getMessage());
        }
    }

    private File getFileByDataType(String dataType) {
        return switch (dataType.toLowerCase()) {
            case "data" -> dataFile;
            case "place" -> placeFile;
            case "destroys" -> destroysFile;
            case "deads" -> deadsFile;
            case "mobdie" -> mobdieFile;
            case "onlinetime" -> onlinetimeFile;
            case "break_bedrock" -> breakBedrockFile;
            default -> {
                plugin.getLogger().warning(i18n.translate("data.json.unknown_type") + dataType);
                yield null;
            }
        };
    }

    @Override
    public void shutdownDataManager() {
        plugin.getLogger().info(i18n.translate("data.json.shutdown_saving"));
        saveAllData();
    }

    @Override
    public void resetLeaderboard(String type) {
        JSONObject empty = new JSONObject();
        switch (type.toLowerCase()) {
            case "place" -> placeData = empty;
            case "destroys" -> destroysData = empty;
            case "deads" -> deadsData = empty;
            case "mobdie" -> mobdieData = empty;
            case "onlinetime" -> onlinetimeData = empty;
            case "break_bedrock" -> breakBedrockData = empty;
            default -> {
                plugin.getLogger().warning(i18n.translate("data.json.unknown_type") + type);
                return;
            }
        }
        saveData(type, empty);
    }

    @Override
    public void resetAll() {
        for (String type : SUPPORTED_TYPES) {
            resetLeaderboard(type);
        }
    }


}