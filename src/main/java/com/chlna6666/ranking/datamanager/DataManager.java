package com.chlna6666.ranking.datamanager;

import com.chlna6666.ranking.Ranking;
import org.json.simple.JSONObject;

public abstract class DataManager {
    protected final Ranking plugin;

    // 数据存储对象
    protected JSONObject playersData;
    protected JSONObject placeData;
    protected JSONObject destroysData;
    protected JSONObject deadsData;
    protected JSONObject mobdieData;
    protected JSONObject onlinetimeData;
    protected JSONObject breakBedrockData;

    protected DataManager(Ranking plugin) {
        this.plugin = plugin;
    }

    protected boolean isJsonStorage() {
        String method = plugin.getConfig().getString("data_storage.method", "json");
        return "json".equalsIgnoreCase(method);
    }

    protected void initializeEmptyData() {
        playersData = new JSONObject();
        placeData = new JSONObject();
        destroysData = new JSONObject();
        deadsData = new JSONObject();
        mobdieData = new JSONObject();
        onlinetimeData = new JSONObject();
        breakBedrockData = new JSONObject();
    }

    // 抽象方法
    public abstract void saveData(String dataType, JSONObject data);
    public abstract void saveAllData();
    public abstract void shutdownDataManager();

    // 公共数据访问方法
    public JSONObject getPlayersData() { return playersData; }
    public JSONObject getPlaceData() { return placeData; }
    public JSONObject getDestroysData() { return destroysData; }
    public JSONObject getDeadsData() { return deadsData; }
    public JSONObject getMobdieData() { return mobdieData; }
    public JSONObject getOnlinetimeData() { return onlinetimeData; }
    public JSONObject getBreakBedrockData() { return breakBedrockData; }

    // 需要子类实现的受保护方法
    protected abstract void loadFiles();

}