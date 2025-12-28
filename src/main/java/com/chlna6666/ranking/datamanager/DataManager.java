package com.chlna6666.ranking.datamanager;

import com.chlna6666.ranking.Ranking;
import com.chlna6666.ranking.enums.LeaderboardType;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class DataManager {
    protected final Ranking plugin;

    // 依然保留这个列表供 TabCompleter 使用，但它是通过枚举生成的
    public static final List<String> SUPPORTED_TYPES = Stream.of(LeaderboardType.values())
            .map(LeaderboardType::getId)
            .collect(Collectors.toList());

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

    // --- 新增通用方法 ---
    public JSONObject getData(LeaderboardType type) {
        return switch (type) {
            case PLACE -> placeData;
            case DESTROYS -> destroysData;
            case DEADS -> deadsData;
            case MOB_DIE -> mobdieData;
            case ONLINE_TIME -> onlinetimeData;
            case BREAK_BEDROCK -> breakBedrockData;
        };
    }
    // -------------------

    public abstract void saveData(String dataType, JSONObject data);
    public abstract void saveAllData();
    public abstract void shutdownDataManager();

    public JSONObject getPlayersData() { return playersData; }
    public JSONObject getPlaceData() { return placeData; }
    public JSONObject getDestroysData() { return destroysData; }
    public JSONObject getDeadsData() { return deadsData; }
    public JSONObject getMobdieData() { return mobdieData; }
    public JSONObject getOnlinetimeData() { return onlinetimeData; }
    public JSONObject getBreakBedrockData() { return breakBedrockData; }

    protected abstract void loadFiles();

    public void resetAll() {
        for (LeaderboardType type : LeaderboardType.values()) {
            resetLeaderboard(type.getId());
        }
    }

    public void resetLeaderboard(String typeId) {
        LeaderboardType type = LeaderboardType.fromString(typeId);
        if (type == null) {
            plugin.getLogger().warning("未知的排行榜类型: " + typeId);
            return;
        }

        JSONObject empty = new JSONObject();
        // 更新内存引用
        switch (type) {
            case PLACE -> placeData.clear();
            case DESTROYS -> destroysData.clear();
            case DEADS -> deadsData.clear();
            case MOB_DIE -> mobdieData.clear();
            case ONLINE_TIME -> onlinetimeData.clear();
            case BREAK_BEDROCK -> breakBedrockData.clear();
        }
        // 触发保存
        saveData(typeId, empty);
    }
}