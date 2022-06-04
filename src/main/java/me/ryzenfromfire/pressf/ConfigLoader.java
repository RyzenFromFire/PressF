package me.ryzenfromfire.pressf;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.Objects;


public class ConfigLoader {

    private final PressF plugin;

    private Component prefix, fKey, leaderboardHeader;
    private String messageColor, accentColor, accentColor2, errorColor;
    private long cooldown, lbPageEntries;
    private boolean lbHeaderEnabled, lbNextPgMsgEnabled, replaceF;

    public ConfigLoader(PressF plugin) {
        this.plugin = plugin;
        this.plugin.saveDefaultConfig();
        loadConfig();
    }

    private void loadConfig() {
        FileConfiguration config = plugin.getConfig();

        this.prefix = MiniMessage.miniMessage().deserialize(Objects.requireNonNull(config.getString("prefix")));
        this.fKey = MiniMessage.miniMessage().deserialize(Objects.requireNonNull(config.getString("fkey")));
        this.leaderboardHeader = MiniMessage.miniMessage().deserialize(Objects.requireNonNull(config.getString("leaderboard-header")));
        this.lbHeaderEnabled = config.getBoolean("lb-header-enabled");
        this.lbNextPgMsgEnabled = config.getBoolean("lb-next-page-msg-enabled");
        this.lbPageEntries = config.getLong("lb-entries-per-page");
        this.messageColor = "<" + config.getString("message-color") + ">";
        this.accentColor = "<" + config.getString("accent-color") + ">";
        this.accentColor2 = "<" + config.getString("accent-color-2") + ">";
        this.errorColor = "<" + config.getString("error-color") + ">";
        this.cooldown = config.getLong("cooldown");
        this.replaceF = config.getBoolean("replace-f");
    }

    public Component getPrefix() { return this.prefix; }

    public Component getFKey() { return this.fKey; }

    public Component getLBHeader() { return this.leaderboardHeader; }

    public boolean getLBHeaderEnabled() { return this.lbHeaderEnabled; }

    public boolean getLBNextPgMsgEnabled() { return this.lbNextPgMsgEnabled; }

    public long getLBPageEntries() { return this.lbPageEntries; }

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

    public boolean doReplaceF() { return this.replaceF; }

    public void reloadConfig() {
        plugin.reloadConfig();
        loadConfig();
    }

}
