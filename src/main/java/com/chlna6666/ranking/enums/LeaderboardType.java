package com.chlna6666.ranking.enums;

public enum LeaderboardType {
    PLACE("place", "sidebar.place"),
    DESTROYS("destroys", "sidebar.break"),
    DEADS("deads", "sidebar.death"),
    MOB_DIE("mobdie", "sidebar.kill"),
    ONLINE_TIME("onlinetime", "sidebar.online_time"),
    BREAK_BEDROCK("break_bedrock", "sidebar.break_bedrock");

    private final String id;
    private final String langKey;

    LeaderboardType(String id, String langKey) {
        this.id = id;
        this.langKey = langKey;
    }

    public String getId() {
        return id;
    }

    public String getLangKey() {
        return langKey;
    }

    public static LeaderboardType fromString(String text) {
        if (text == null) return null;
        for (LeaderboardType type : values()) {
            if (type.id.equalsIgnoreCase(text)) {
                return type;
            }
        }
        return null;
    }
}