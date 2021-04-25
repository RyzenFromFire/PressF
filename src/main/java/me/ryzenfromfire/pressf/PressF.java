package me.ryzenfromfire.pressf;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.Template;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import java.util.HashMap;
import java.util.UUID;

public final class PressF extends JavaPlugin {

    private ConfigLoader configLoader;

    HashMap<UUID, Integer> fCount = new HashMap<>();
    Component prefix;
    Component fKey;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("Pressing the start button (not F).");
        this.getServer().getPluginManager().registerEvents(new Events(), this);
        this.configLoader = new ConfigLoader(this);
        prefix = configLoader.getPrefix();
        fKey = configLoader.getFKey();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equals("pressf") && sender instanceof Player) {
            Events events = new Events();

            Player player = (Player) sender;

            Player lastMessenger = events.getLastMessenger();

            //target assignment (who is receiving the F)
            Player target;
            if (args.length != 0) {
                //first check if arg given
                target = Bukkit.getPlayer(args[0]);
            } else if (lastMessenger != null) {
                //if not set target to last person to send a message
                target = lastMessenger;
            } else {
                //if not set target to the command sender
                //should only happen if there has not yet been a message sent
                target = player;
            }

            getLogger().info("Pressed F. target = " + target);

            //if player is listed, set count to their current fCount, if not, initialize with 0
            int count = fCount.getOrDefault(target.getUniqueId(), 0);

            //increment target's fCount
            fCount.put(target.getUniqueId(), count + 1);

            Component pressedF = MiniMessage.get().parse("<prefix> Pressed <fKey> to pay respects to <player>.",
                    Template.of("prefix", prefix),
                    Template.of("fKey", fKey),
                    Template.of("player", target.displayName()));
            player.sendMessage(pressedF);
            return true;
        } else if (command.getName().equals("pressf") && !(sender instanceof Player)) {
            getLogger().info("You cannot press F from the console. F for you.");
            return true;
        }

        if (command.getName().equals("viewf") && sender instanceof Player) {
            Player player = (Player) sender;

            Component viewF = MiniMessage.get().parse("<prefix> You have received <count> <fKey>s.",
                    Template.of("prefix", prefix),
                    Template.of("fKey", fKey),
                    Template.of("count", String.valueOf(fCount.get(player.getUniqueId()))));
            player.sendMessage(viewF);

            return true;
        } else if (command.getName().equals("viewf") && !(sender instanceof Player)) {
            getLogger().info("You are the console, you don't exist. You can't have an F.");
            return true;
        }

        if (command.getName().equals("pfadmin")) {
            Component usage = MiniMessage.get().parse("Usage: /pfadmin <reload>");
            Component reloadingMsg = MiniMessage.get().parse("Reloaded config file.");

            if (args.length == 0) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    player.sendMessage(MiniMessage.get().parse("<prefix> <usage>",
                            Template.of("prefix", prefix),
                            Template.of("usage", usage)));
                } else {
                    getLogger().info(String.valueOf(usage));
                }
                return true;
            } else {
                if (args[0].equals("reload")) {
                    configLoader.reloadConfig();

                    //send message reloading is complete.
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        player.sendMessage(MiniMessage.get().parse("<prefix> <reload>",
                                Template.of("prefix", prefix),
                                Template.of("reload", reloadingMsg)));
                    } else {
                        getLogger().info(String.valueOf(reloadingMsg));
                    }

                    return true;
                }
            }
        }

        return false;
    }
}
