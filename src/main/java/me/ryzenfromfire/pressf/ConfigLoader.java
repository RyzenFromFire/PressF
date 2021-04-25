package me.ryzenfromfire.pressf;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;


public class ConfigLoader {

    private final PressF plugin;

    private Component prefix;
    private Component fKey;

    public ConfigLoader(PressF plugin) {
        this.plugin = plugin;
        this.plugin.saveDefaultConfig();
        loadConfig();
    }

    private void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        String config_prefix = config.getString("prefix");
        this.prefix = MiniMessage.get().parse(config_prefix);
        String config_fKey = config.getString("fkey");
        this.fKey = MiniMessage.get().parse(config_fKey);
    }

    public Component getPrefix() { return this.prefix; }

    public Component getFKey() { return this.fKey; }

    public void reloadConfig() {
        plugin.reloadConfig();
        loadConfig();
    }

}
