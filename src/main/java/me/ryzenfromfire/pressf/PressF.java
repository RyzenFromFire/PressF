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

import java.util.*;
import java.util.concurrent.TimeUnit;

public final class PressF extends JavaPlugin {

    private ConfigLoader configLoader;
    private CooldownManager cooldownManager;
    private Data data;

    private final Map<UUID, Integer> fCount = new HashMap<>();
    private Component prefix, fKey, lbHeader;
    private String messageColor, accentColor, accentColor2, errorColor;

    public Map<UUID, Integer> get_fCount() {
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

    private void getComponents() {
        prefix = configLoader.getPrefix();
        fKey = configLoader.getFKey();
        lbHeader = configLoader.getLBHeader();
        messageColor = configLoader.getColor(ConfigLoader.colorType.message);
        accentColor = configLoader.getColor(ConfigLoader.colorType.accent);
        accentColor2 = configLoader.getColor(ConfigLoader.colorType.accent2);
        errorColor = configLoader.getColor(ConfigLoader.colorType.error);
    }

    public ConfigLoader getConfigLoader() { return configLoader; }

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("Pressing the start button (not F).");
        this.getServer().getPluginManager().registerEvents(new Events(), this);
        this.configLoader = new ConfigLoader(this);
        this.cooldownManager = new CooldownManager(this);
        getComponents();
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
        Component invalidTarget = MiniMessage.get().parse("<prefix> <ec>Error: Invalid target. Please provide the name of a valid player.",
                Template.of("prefix", prefix),
                Template.of("ec", errorColor));

        //
        //   PRESSF
        //
        if (command.getName().equals("pressf") && sender instanceof Player) { //PLAYER
            Events events = new Events();

            Player player = (Player) sender;

            Player lastMessenger = events.getLastMessenger();

            //check if player is on cooldown
            //first check if player has a cooldown, if not, init to 0
            if (!cooldownManager.exists(player.getUniqueId())) { cooldownManager.setCooldown(player.getUniqueId(), 0L); }

            //time in ms since command used
            long timeSince = System.currentTimeMillis() - cooldownManager.getCooldown(player.getUniqueId());

            //is player on cooldown?
            if (TimeUnit.MILLISECONDS.toSeconds(timeSince) < configLoader.getCooldown()) {
                //time since command used is less than cooldown, command on cooldown
                Component onCooldown = MiniMessage.get().parse("<prefix> <ec>You cannot press <fKey> <ec>for another <ac><time> <ec>seconds.",
                        Template.of("prefix", prefix),
                        Template.of("ec", errorColor),
                        Template.of("ac", accentColor),
                        Template.of("fKey", fKey),
                        Template.of("time", String.valueOf(configLoader.getCooldown() - TimeUnit.MILLISECONDS.toSeconds(timeSince)))); //convert timeSince to time left to use
                player.sendMessage(onCooldown);
                return true;
            } //otherwise continue

            //target assignment (who is receiving the F)
            UUID targetId;
            String targetName;
            if (args.length != 0) {
                //if argument is given, find the player based on passed string username

                //check for data
                if (noData(args[0])) {
                    player.sendMessage(invalidTarget);
                    return true;
                }

                //set target
                targetId = Bukkit.getOfflinePlayer(args[0]).getUniqueId();
                targetName = Bukkit.getOfflinePlayer(args[0]).getName();

            } else if (lastMessenger != null) {
                //if not set target to last person to send a message

                //check data
                if (noData(lastMessenger.getName())) {
                    player.sendMessage(invalidTarget);
                    return true;
                }

                //set target
                targetId = lastMessenger.getUniqueId();
                targetName = lastMessenger.getName();
            } else {
                //if not set target to the command sender
                //should only happen if there has not yet been a message sent

                //check data
                fCount.putIfAbsent(player.getUniqueId(), 0);

                //set target
                targetId = player.getUniqueId();
                targetName = player.getName();
            }

            //increment target's fCount
            fCount.put(targetId, fCount.get(targetId) + 1);

            //set cooldown
            cooldownManager.setCooldown(player.getUniqueId(), System.currentTimeMillis());

            //if player clicked the F in chat
            //hover message will run "/pressf <target> false
            if (args.length > 1) {
                if (args[1].equals("false")) {
                    //send message just to player
                    //should only trigger from clicking a global message
                    if (targetName.equals(player.getName())) { targetName = "yourself"; }
                    Component pressedF = MiniMessage.get().parse("<prefix> <mc>You pressed <fKey> <mc>to pay respects to <ac><target><mc>.",
                            Template.of("prefix", prefix),
                            Template.of("mc", messageColor),
                            Template.of("ac", accentColor),
                            Template.of("fKey", fKey),
                            Template.of("target", targetName));
                    player.sendMessage(pressedF);
                }
            } else {
                //send global server message of the pressed F
                String actualTargetName = targetName;
                if (targetName.equals(player.getName())) { targetName = "themself"; }
                String hoverText = "<mc>Click to press " + configLoader.getRawString("fkey") + " for <ac><aTarget><mc>!"; //hover text hates components
                Component pressedF = MiniMessage.get().parse("<hover:show_text:'<hoverText>'>" +
                                "<click:run_command:/pressf <aTarget> false>" +
                                "<prefix> <ac><player> <mc>pressed <fKey> <mc>to pay respects to <ac><target><mc>.</hover></click>",
                        Template.of("hoverText", hoverText),
                        Template.of("prefix", prefix),
                        Template.of("mc", messageColor),
                        Template.of("ac", accentColor),
                        Template.of("fKey", fKey),
                        Template.of("player", player.getName()),
                        Template.of("target", targetName),
                        Template.of("aTarget", actualTargetName));
                this.getServer().sendMessage(pressedF);
            }
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
            String targetName;
            Component subject; //have correct grammar for message
            if (args.length != 0) {
                //if argument is given, find the player based on passed string username

                //check for data
                if (noData(args[0])) {
                    player.sendMessage(invalidTarget);
                    return true;
                }

                //set target
                targetId = Bukkit.getOfflinePlayer(args[0]).getUniqueId();
                targetName = Bukkit.getOfflinePlayer(args[0]).getName();

                subject = MiniMessage.get().parse("<mc>" + targetName + " has", Template.of("mc", messageColor));
            } else {
                //if not set target to the command sender
                //should only happen if there has not yet been a message sent
                targetId = player.getUniqueId();
                subject = MiniMessage.get().parse("<mc>You have", Template.of("mc", messageColor));

                //if player fCount is null, put 0
                fCount.putIfAbsent(player.getUniqueId(), 0); // no need to check data here since player is always online
            }

            //send message to player
            Component viewF = MiniMessage.get().parse("<prefix> <subject> <mc>received <ac><count> <fKey><mc>s.",
                    Template.of("prefix", prefix),
                    Template.of("subject", subject),
                    Template.of("mc", messageColor),
                    Template.of("ac", accentColor),
                    Template.of("fKey", fKey),
                    Template.of("count", String.valueOf(fCount.get(targetId))));
            player.sendMessage(viewF);

            return true;
        } else if (command.getName().equals("viewf") && !(sender instanceof Player)) { //CONSOLE
            if (args.length == 0) { getLogger().info("You are the console, you don't exist. You can't have an F."); }
            else {
                //check for data
                if (noData(args[0])) {
                    getLogger().info("Error: Invalid target. Please provide the name of a valid player.");
                    return true;
                }

                //target assignment (who is receiving the F)
                UUID targetId = Bukkit.getOfflinePlayer(args[0]).getUniqueId();
                
                //send message to console
                getLogger().info(Bukkit.getOfflinePlayer(args[0]).getName() + " has received " + fCount.get(targetId) + " Fs.");
            }
            return true;
        }

        //
        // PFTOP | F LEADERBOARD
        //
        if (command.getName().equals("pressftop") && sender instanceof Player) {
            Player player = (Player) sender;
            Map<UUID, Integer> leaderboard = new TreeMap<>();
            fCount.forEach(leaderboard::put);
            List<String> playerList = new ArrayList<>();
            for (Map.Entry<UUID, Integer> entry : leaderboard.entrySet()) {
                playerList.add(getServer().getOfflinePlayer(entry.getKey()).getName() + ": <ac>" + String.valueOf(entry.getValue()));
            }
            StringBuilder top10 = new StringBuilder();
            int iLimit = Math.min(playerList.size(), 10);
            for (int i = 0; i < iLimit; i++) {
                top10.append("<ac2>").append(String.valueOf(i + 1)).append(". <mc>").append(playerList.get(i)).append("\n");
            }
            Component top10Component = MiniMessage.get().parse(top10.toString(), Template.of("mc", messageColor), Template.of("ac", accentColor), Template.of("ac2", accentColor2));
            Component lbMessage = MiniMessage.get().parse(
                    "\n<header> \n<list>", Template.of("header", lbHeader), Template.of("list", top10Component));
            player.sendMessage(lbMessage);
            return true;
        } else if (command.getName().equals("pressftop") && !(sender instanceof Player)) { //CONSOLE
            getLogger().info("You cannot view the F leaderboard from the console.");
            return true;
        }

        //
        // PFADMIN
        //
        if (command.getName().equals("pfadmin") && sender.hasPermission("pressf.admin")) {
            Component usage = MiniMessage.get().parse("<mc>Usage: /pfadmin <ac><reload | load | save>", Template.of("mc", messageColor), Template.of("ac", accentColor));
            Component reloadingMsg = MiniMessage.get().parse("<mc>Reloaded config file.", Template.of("mc", messageColor));
            Component saved = MiniMessage.get().parse("<mc>Saved data to file.", Template.of("mc", messageColor));
            Component loaded = MiniMessage.get().parse("<mc>Loaded data from file.", Template.of("mc", messageColor));

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
                        getComponents();

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
