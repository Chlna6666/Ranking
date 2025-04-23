package com.chlna6666.ranking.I18n;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.nio.file.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Stream;

public class I18n {
    private final JavaPlugin plugin;

    // 缓存已加载的语言映射
    private final Map<String, Map<String, String>> cache = new ConcurrentHashMap<>();
    private Map<String, String> currentMessages = Collections.emptyMap();
    private String currentLanguage = "en_US";

    public I18n(JavaPlugin plugin) {
        this.plugin = plugin;
        copyDefaultLanguageFiles();
        switchLanguage(plugin.getConfig().getString("language", "en_US"));
    }

    /**
     * 翻译指定 key，并替换占位符
     */
    public String translate(String key, Object... args) {
        String template = currentMessages.getOrDefault(key, key);
        return MessageFormat.format(template, args);
    }

    /**
     * 切换当前语言
     */
    public void switchLanguage(String lang) {
        if (lang == null || lang.isEmpty()) {
            lang = "en_US";
        }
        currentMessages = cache.computeIfAbsent(lang, this::loadLanguageFromFile);
        plugin.getLogger().info(translate("i18n.language_switched", lang));
    }

    /**
     * 从文件加载语言配置：先加载 en_US 为基础，然后尝试加载目标语言覆盖
     */
    private Map<String, String> loadLanguageFromFile(String lang) {
        // 1. 加载默认 en_US
        File defaultFile = new File(plugin.getDataFolder(), "language/en_US.yml");
        YamlConfiguration defaultYaml = YamlConfiguration.loadConfiguration(defaultFile);
        Map<String, String> result = new HashMap<>();
        for (String key : defaultYaml.getKeys(true)) {
            String val = defaultYaml.getString(key);
            if (val != null) {
                result.put(key, val);
            }
        }

        // 2. 尝试加载目标语言覆盖
        File targetFile = new File(plugin.getDataFolder(), "language/" + lang + ".yml");
        if (!targetFile.exists()) {
            String warnTemplate = result.get("i18n.language_not_found");
            plugin.getLogger().warning(MessageFormat.format(warnTemplate, lang));
        } else {
            YamlConfiguration targetYaml = YamlConfiguration.loadConfiguration(targetFile);
            for (String key : targetYaml.getKeys(true)) {
                String val = targetYaml.getString(key);
                if (val != null) {
                    result.put(key, val);
                }
            }
        }

        return Collections.unmodifiableMap(result);
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }

    public void reloadCurrentLanguage() {
        cache.remove(currentLanguage);
        switchLanguage(currentLanguage);
    }

    public void copyDefaultLanguageFiles() {
        copyResourceFolder();
    }

    private void copyResourceFolder() {
        try {
            File jarFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            if (jarFile.isFile()) {
                try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile)) {
                    Enumeration<java.util.jar.JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        java.util.jar.JarEntry entry = entries.nextElement();
                        String name = entry.getName();
                        if (name.startsWith("language/")) {
                            File f = new File(plugin.getDataFolder(), name);
                            if (entry.isDirectory()) {
                                if (!f.mkdirs() && !f.isDirectory()) {
                                    plugin.getLogger().log(Level.SEVERE, "无法创建目录: " + f.getAbsolutePath());
                                }
                            } else {
                                try (InputStream is = jar.getInputStream(entry)) {
                                    Files.copy(is, f.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                }
                            }
                        }
                    }
                }
            } else {
                java.net.URL url = plugin.getClass().getClassLoader().getResource("language");
                if (url != null) {
                    Path srcPath = Paths.get(url.toURI());
                    Path destPath = Paths.get(plugin.getDataFolder().toURI()).resolve("language");
                    try (Stream<Path> stream = Files.walk(srcPath)) {
                        stream.forEach(src -> {
                            try {
                                Files.copy(src, destPath.resolve(srcPath.relativize(src)), StandardCopyOption.REPLACE_EXISTING);
                            } catch (Exception e) {
                                plugin.getLogger().log(Level.SEVERE, "复制语言文件失败。", e);
                            }
                        });
                    }
                } else {
                    plugin.getLogger().log(Level.SEVERE, "无法找到资源目录 'language'");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "语言资源文件复制失败。", e);
        }
    }
}
