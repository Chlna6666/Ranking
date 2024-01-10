package org.com.ranking;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.json.simple.JSONObject;

public class PublicUtil {

    public static void saveJSONAsync(JSONObject json, File file, Plugin plugin) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(json.toJSONString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}

