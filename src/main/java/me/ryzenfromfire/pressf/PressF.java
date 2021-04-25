package me.ryzenfromfire.pressf;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.Template;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import java.util.HashMap;
import java.util.UUID;

public final class PressF extends JavaPlugin {

    private ConfigLoader configLoader;
    private Data data;

    private HashMap<UUID, Integer> fCount = new HashMap<>();
    Component prefix;
    Component fKey;


    public HashMap<UUID, Integer> get_fCount() {
        return fCount;
    }

    private boolean noData(String targetName) { //Checks if the given target has any data stored and if they have played before.
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (fCount.get(target.getUniqueId()) == null) {
            if (target.hasPlayedBefore()) { //player has played before but has never been interacted with
                fCount.putIfAbsent(target.getUniqueId(), 0);
                return false;
            } else { //player has not played before and has no data
                return true;
            }
        }
        return false;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("Pressing the start button (not F).");
        this.getServer().getPluginManager().registerEvents(new Events(), this);
        this.configLoader = new ConfigLoader(this);
        prefix = configLoader.getPrefix();
        fKey = configLoader.getFKey();

        this.data = new Data(this);
        data.load(fCount);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("Saving data...");
        data.save(fCount);
        getLogger().info("Saved data, shutting down.");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        //Global Command Components
        Component invalidTarget = MiniMessage.get().parse("<prefix> Error: Invalid target. Please provide the name of a valid player.",
                Template.of("prefix", prefix));

        //
        //   PRESSF
        //
        if (command.getName().equals("pressf") && sender instanceof Player) { //PLAYER
            Events events = new Events();

            Player player = (Player) sender;

            Player lastMessenger = events.getLastMessenger();

            //target assignment (who is receiving the F)
            UUID targetId;
            if (args.length != 0) {
                //if argument is given, find the player based on passed string username

                //check for data
                if (noData(args[0])) {
                    player.sendMessage(invalidTarget);
                    return false;
                }

                //set targetId
                targetId = Bukkit.getOfflinePlayer(args[0]).getUniqueId();

            } else if (lastMessenger != null) {
                //if not set target to last person to send a message
                targetId = lastMessenger.getUniqueId();
            } else {
                //if not set target to the command sender
                //should only happen if there has not yet been a message sent
                targetId = player.getUniqueId();
            }

            //increment target's fCount
            fCount.put(targetId, fCount.get(targetId) + 1);

            //send message to player
            //TODO: Make a global message instead of only notifying the sender player.
            Component pressedF = MiniMessage.get().parse("<prefix> Pressed <fKey> to pay respects to <player>.",
                    Template.of("prefix", prefix),
                    Template.of("fKey", fKey),
                    Template.of("player", args[0]));
            player.sendMessage(pressedF);
            return true;
        } else if (command.getName().equals("pressf") && !(sender instanceof Player)) { //CONSOLE
            getLogger().info("You cannot press F from the console. F.");
            return true;
        }

        //
        // VIEWF
        //
        if (command.getName().equals("viewf") && sender instanceof Player) { //PLAYER
            Player player = (Player) sender;

            //target assignment (who is receiving the F)
            UUID targetId;
            Component subject; //have correct grammar for message
            if (args.length != 0) {
                //if argument is given, find the player based on passed string username

                //check for data
                if (noData(args[0])) {
                    player.sendMessage(invalidTarget);
                    return false;
                }

                //set targetId
                targetId = Bukkit.getOfflinePlayer(args[0]).getUniqueId();

                subject = MiniMessage.get().parse(args[0] + " has");
            } else {
                //if not set target to the command sender
                //should only happen if there has not yet been a message sent
                targetId = player.getUniqueId();
                subject = MiniMessage.get().parse("You have");

                //if player fCount is null, put 0
                fCount.putIfAbsent(player.getUniqueId(), 0); // no need to check data here since player is always online
            }

            //send message to player
            Component viewF = MiniMessage.get().parse("<prefix> <subject> received <count> <fKey>s.",
                    Template.of("prefix", prefix),
                    Template.of("subject", subject),
                    Template.of("fKey", fKey),
                    Template.of("count", String.valueOf(fCount.get(targetId))));
            player.sendMessage(viewF);

            return true;
        } else if (command.getName().equals("viewf") && !(sender instanceof Player)) { //CONSOLE
            if (args.length == 0) { getLogger().info("You are the console, you don't exist. You can't have an F."); }
            else {
                //target assignment (who is receiving the F)
                UUID targetId = Bukkit.getOfflinePlayer(args[0]).getUniqueId();
                
                //check for data
                if (noData(args[0])) {
                    getLogger().info("Error: Invalid target. Please provide the name of a valid player.");
                    return false;
                }
                
                //send message to console
                getLogger().info(args[0] + " has received " + fCount.get(targetId) + "Fs.");
            }
            return true;
        }

        //
        // PFADMIN
        //
        if (command.getName().equals("pfadmin")) {
            Component usage = MiniMessage.get().parse("Usage: /pfadmin <reload | load | save>");
            Component reloadingMsg = MiniMessage.get().parse("Reloaded config file.");
            Component saved = MiniMessage.get().parse("Saved data to file.");
            Component loaded = MiniMessage.get().parse("Loaded data from file.");

            if (args.length == 0) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    player.sendMessage(MiniMessage.get().parse("<prefix> <usage>",
                            Template.of("prefix", prefix),
                            Template.of("usage", usage)));
                } else {
                    getLogger().info(PlainComponentSerializer.plain().serialize(usage));
                }
                return true;
            } else {
                switch (args[0]) {
                    case "reload":
                        configLoader.reloadConfig();
                        prefix = configLoader.getPrefix();
                        fKey = configLoader.getFKey();

                        //send message reloading is complete.
                        if (sender instanceof Player) {
                            Player player = (Player) sender;
                            player.sendMessage(MiniMessage.get().parse("<prefix> <reload>",
                                    Template.of("prefix", prefix),
                                    Template.of("reload", reloadingMsg)));
                        } else {
                            getLogger().info(PlainComponentSerializer.plain().serialize(reloadingMsg));
                        }

                        return true;

                    case "load":
                        data.load(fCount);

                        //send message loading data is complete
                        if (sender instanceof Player) {
                            Player player = (Player) sender;
                            player.sendMessage(MiniMessage.get().parse("<prefix> <loaded>",
                                    Template.of("prefix", prefix),
                                    Template.of("loaded", loaded)));
                        } else {
                            getLogger().info(PlainComponentSerializer.plain().serialize(loaded));
                        }
                        return true;

                    case "save":
                        data.save(fCount);

                        //send message saving data is complete
                        if (sender instanceof Player) {
                            Player player = (Player) sender;
                            player.sendMessage(MiniMessage.get().parse("<prefix> <saved>",
                                    Template.of("prefix", prefix),
                                    Template.of("saved", saved)));
                        } else {
                            getLogger().info(PlainComponentSerializer.plain().serialize(saved));
                        }

                        return true;
                }
            }
        }

        return false;
    }
}
