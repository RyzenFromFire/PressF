package me.ryzenfromfire.pressf;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.Supplier;

public final class PressF extends JavaPlugin {

    HashMap<UUID, Integer> fCount = new HashMap<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("Pressing the start button (not F).");
        this.getServer().getPluginManager().registerEvents(new Events(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equals("pressf")) {
            Player player = (Player) sender;

            Player lastMessenger = Events.getLastMessenger();

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
            assert target != null;
            int count = fCount.getOrDefault(target.getUniqueId(), 0);

            //increment target's fCount
            fCount.put(target.getUniqueId(), count + 1);

            player.sendMessage("[PressF] Pressed F to pay respects to " + target.getName() + ".");
            return true;
        }

        if (command.getName().equals("viewf")) {
            Player player = (Player) sender;
            player.sendMessage("[PressF] You have " + fCount.get(player.getUniqueId()) + " F's.");

            return true;
        }

        return false;
    }
}
