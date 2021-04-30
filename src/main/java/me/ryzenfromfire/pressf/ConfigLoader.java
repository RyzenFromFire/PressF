package me.ryzenfromfire.pressf;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;


public class ConfigLoader {

    private final PressF plugin;

    private Component prefix, fKey, leaderboardHeader;
    private String messageColor, accentColor, accentColor2, errorColor;
    private long cooldown;

    public ConfigLoader(PressF plugin) {
        this.plugin = plugin;
        this.plugin.saveDefaultConfig();
        loadConfig();
    }

    private void loadConfig() {
        FileConfiguration config = plugin.getConfig();

        this.prefix = MiniMessage.get().parse(config.getString("prefix"));
        this.fKey = MiniMessage.get().parse(config.getString("fkey"));
        this.leaderboardHeader = MiniMessage.get().parse(config.getString("leaderboard-header"));
        this.messageColor = "<" + config.getString("message-color") + ">";
        this.accentColor = "<" + config.getString("accent-color") + ">";
        this.errorColor = "<" + config.getString("error-color") + ">";
        this.cooldown = config.getLong("cooldown");
    }

    public Component getPrefix() { return this.prefix; }

    public Component getFKey() { return this.fKey; }

    public Component getLBHeader() { return this.leaderboardHeader; }

    public enum colorType {
        message, accent, accent2, error
    }

    public String getColor(colorType type) {
        switch(type) {
            case message:
                return this.messageColor;
            case accent:
                return this.accentColor;
            case accent2:
                return this.accentColor2;
            case error:
                return this.errorColor;
            default:
                return "";
        }
    }

    public long getCooldown() { return this.cooldown; }

    public String getRawString(String key) {
        return plugin.getConfig().getString(key);
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        loadConfig();
    }

}
