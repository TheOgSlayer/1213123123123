package dev.nametags;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatsStore {
    private final File file;
    private final YamlConfiguration yaml;
    private final Map<UUID, PlayerData> cache = new HashMap<>();

    public StatsStore(NametagsPlugin plugin) {
        plugin.getDataFolder().mkdirs();
        file = new File(plugin.getDataFolder(), "data.yml");
        yaml = YamlConfiguration.loadConfiguration(file);
    }

    public PlayerData get(UUID id) {
        return cache.computeIfAbsent(id, k -> {
            PlayerData d = new PlayerData();
            String p = k + ".";
            d.kills = yaml.getInt(p + "kills", 0);
            d.deaths = yaml.getInt(p + "deaths", 0);
            d.enabled = yaml.getBoolean(p + "enabled", true);
            d.showMoney = yaml.getBoolean(p + "money", true);
            d.showHearts = yaml.getBoolean(p + "hearts", true);
            d.showKills = yaml.getBoolean(p + "showKills", true);
            d.showDeaths = yaml.getBoolean(p + "showDeaths", true);
            return d;
        });
    }

    public void put(UUID id) {
        PlayerData d = cache.get(id);
        if (d == null) return;
        String p = id + ".";
        yaml.set(p + "kills", d.kills);
        yaml.set(p + "deaths", d.deaths);
        yaml.set(p + "enabled", d.enabled);
        yaml.set(p + "money", d.showMoney);
        yaml.set(p + "hearts", d.showHearts);
        yaml.set(p + "showKills", d.showKills);
        yaml.set(p + "showDeaths", d.showDeaths);
    }

    public void saveAll() {
        for (UUID id : cache.keySet()) put(id);
        try { yaml.save(file); } catch (IOException e) { e.printStackTrace(); }
    }
}
