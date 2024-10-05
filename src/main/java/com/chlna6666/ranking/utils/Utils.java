package com.chlna6666.ranking.utils;

public class Utils {
    private static Boolean folia = null;

    public static boolean isFolia() {
        if (folia == null) {
            try {
                Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
                folia = true;
            } catch (ClassNotFoundException e) {
                folia = false;
            }
        }
        return folia;
    }
}
