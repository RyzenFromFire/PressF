package me.ryzenfromfire.pressf;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class Data {

    private final PressF plugin;

    private File fCountFile;
    private YamlConfiguration fCountYml;


    public Data(PressF plugin) {
        this.plugin = plugin;
    }

    public void load(HashMap<UUID, Integer> map) {

        fCountFile = new File(plugin.getDataFolder() + File.separator + "fCount.yml");

        if (!fCountFile.exists()) {
            plugin.saveResource("fCount.yml", false);
        }

        fCountYml = YamlConfiguration.loadConfiguration(fCountFile);
        //for each line in the config file, load the key and value into the argument hashmap
        for (String idString : fCountYml.getKeys(false)) {
            Integer fCount = (Integer) fCountYml.get(idString);
            if (fCount == null) {
                fCount = -1; //for now just set it to -1 to represent error
            }
            UUID uuid = UUID.fromString(idString);

            map.put(uuid, fCount);

        }
    }

    public void save(HashMap<UUID, Integer> map) {
        map.forEach((k, v) -> {
            fCountYml.set(String.valueOf(k), v);
        });

        try {
            fCountYml.save(fCountFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
