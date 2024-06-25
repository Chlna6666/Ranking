package com.chlna6666.ranking.I18n;

import com.chlna6666.ranking.Ranking;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class I18n {
    private final JavaPlugin plugin;
    private FileConfiguration languageConfig;
    private final String defaultLanguage = "en_US";

    public I18n(JavaPlugin plugin) {
        this.plugin = plugin;
        copyDefaultLanguageFiles();
        loadLanguageFile();
    }

    public void reloadLanguage() {
        loadLanguageFile();
    }

    public String translate(String key) {
        if (languageConfig.contains(key)) {
            return languageConfig.getString(key);
        }
        return key;
    }

    private void loadLanguageFile() {
        String language = plugin.getConfig().getString("language", defaultLanguage);
        File langFile = new File(plugin.getDataFolder(), "language/" + language + ".yml");

        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file for " + language + " not found, using default language (" + defaultLanguage + ").");
            langFile = new File(plugin.getDataFolder(), "language/" + defaultLanguage + ".yml");
            language = defaultLanguage;
        }

        if (!langFile.exists()) {
            plugin.getLogger().warning("Default language file not found, copying default language files.");
            copyDefaultLanguageFiles();
            langFile = new File(plugin.getDataFolder(), "language/" + defaultLanguage + ".yml");
        }

        languageConfig = YamlConfiguration.loadConfiguration(langFile);
        String message = translate("i18n.loaded_language");
        plugin.getLogger().info(message + language);
    }

    public void copyDefaultLanguageFiles() {
        copyResourceFolder("language");
    }

    private void copyResourceFolder(String resourcePath) {
        try {
            File jarFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            if (jarFile.isFile()) {
                java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile);
                java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    java.util.jar.JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.startsWith(resourcePath + "/")) {
                        File f = new File(plugin.getDataFolder(), name);
                        if (entry.isDirectory()) {
                            f.mkdirs();
                        } else {
                            try (InputStream is = jar.getInputStream(entry)) {
                                Files.copy(is, f.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            }
                        }
                    }
                }
                jar.close();
            } else { // Run with IDE
                java.net.URL url = plugin.getClass().getClassLoader().getResource(resourcePath);
                if (url != null) {
                    java.nio.file.Path srcPath = java.nio.file.Paths.get(url.toURI());
                    java.nio.file.Path destPath = Paths.get(plugin.getDataFolder().toURI()).resolve(resourcePath);
                    Files.walk(srcPath).forEach(src -> {
                        try {
                            Files.copy(src, destPath.resolve(srcPath.relativize(src)), StandardCopyOption.REPLACE_EXISTING);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
