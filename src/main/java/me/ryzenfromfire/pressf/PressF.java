package me.ryzenfromfire.pressf;

import me.ryzenfromfire.pressf.hooks.ProtocolLibCompat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class PressF extends JavaPlugin {

    private ConfigLoader configLoader;
    private CooldownManager cooldownManager;
    private Events events;
    private Data data;

    private final Map<UUID, Integer> fCount = new HashMap<>();
    private Component prefix, fKey, lbHeader;
    private String messageColor, accentColor, accentColor2, errorColor;

    public Map<UUID, Integer> get_fCount() {
        return fCount;
    }
    public Boolean protocolLibHook = false;

    private MiniMessage mmsg;

    private boolean noData(String targetName) { // Checks if the given target has any data stored and if they have played before.
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (fCount.get(target.getUniqueId()) == null) {
            if (target.hasPlayedBefore()) { // player has played before but has never been interacted with
                fCount.putIfAbsent(target.getUniqueId(), 0);
                return false;
            } else { // player has not played before and has no data
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

    public ConfigLoader getConfigLoader() { return this.configLoader; }

    public Events getEvents() { return this.events; }

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("Pressing the start button (not F).");
        this.configLoader = new ConfigLoader(this);
        this.cooldownManager = new CooldownManager(this);
        this.events = new Events(this);
        this.getServer().getPluginManager().registerEvents(events, this);
        getComponents();
        this.data = new Data(this);
        data.load(fCount);

        mmsg = MiniMessage.builder()
                .tags(TagResolver.builder()
                        .resolver(StandardTags.defaults())
                        .resolver(Placeholder.component("prefix", this.prefix))
                        .resolver(Placeholder.component("f_key", this.fKey))
                        .resolver(Placeholder.component("header", this.lbHeader))
                        .resolver(Placeholder.parsed("mc", messageColor))
                        .resolver(Placeholder.parsed("ac", accentColor))
                        .resolver(Placeholder.parsed("ac2", accentColor2))
                        .resolver(Placeholder.parsed("ec", errorColor))
                        .build())
                .build();

        // ProtocolLib Hook (Optional)
        if (getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
            getLogger().info("ProtocolLib not found.");
        } else {
            protocolLibHook = true;
            ProtocolLibCompat p = new ProtocolLibCompat(this);
            p.enableProtocolLibHook();
        } // end PL Hook
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("Saving data...");
        data.save(fCount);
        getLogger().info("Data saved, shutting down.");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Global Command Components
        Component invalidTarget = mmsg.deserialize("<prefix> <ec>Error: Invalid target. Please provide the name of a valid player.");

        //
        //   PRESSF
        //
        if (command.getName().equals("pressf") && sender instanceof Player) { // PLAYER
            Player player = (Player) sender, lastDeath = events.getLastDeath(), lastMessenger = events.getLastMessenger();

            long lastMessageTime = events.getLastMessageTime(), lastDeathTime = events.getLastDeathTime();

            // check if player is on cooldown
            // first check if player has a cooldown, if not, init to 0
            if (!cooldownManager.exists(player.getUniqueId())) { cooldownManager.setCooldown(player.getUniqueId(), 0L); }

            // time in ms since command used
            long timeSince = System.currentTimeMillis() - cooldownManager.getCooldown(player.getUniqueId());

            // is player on cooldown?
            if (TimeUnit.MILLISECONDS.toSeconds(timeSince) < configLoader.getCooldown()) {
                // time since command used is less than cooldown, command on cooldown
                Component onCooldown = mmsg.deserialize("<prefix> <ec>You cannot press <f_key> <ec>for another <ac><time> <ec>seconds.",
                        Placeholder.parsed("time", String.valueOf(configLoader.getCooldown() - TimeUnit.MILLISECONDS.toSeconds(timeSince)))); // convert timeSince to time left to use
                player.sendMessage(onCooldown);
                return true;
            } // otherwise continue

            // target assignment (who is receiving the F)
            UUID targetId;
            String targetName;
            if (args.length != 0) {
                // if argument is given, find the player based on passed string username

                // check for data
                if (noData(args[0])) {
                    player.sendMessage(invalidTarget);
                    return true;
                }

                // set target
                targetId = Bukkit.getOfflinePlayer(args[0]).getUniqueId();
                targetName = Bukkit.getOfflinePlayer(args[0]).getName();

            } else if (lastDeath != null && lastDeathTime > lastMessageTime) { // verify validity and more recent than last message
                // check data
                if (noData(lastDeath.getName())) {
                    player.sendMessage(invalidTarget);
                    return true;
                }

                // set target
                targetId = lastDeath.getUniqueId();
                targetName = lastDeath.getName();

            } else if (lastMessenger != null && lastMessageTime > lastDeathTime) { // verify validity and more recent than last death
                // if not set target to last person to send a message

                // check data
                if (noData(lastMessenger.getName())) {
                    player.sendMessage(invalidTarget);
                    return true;
                }

                // set target
                targetId = lastMessenger.getUniqueId();
                targetName = lastMessenger.getName();
            } else {
                // if not set target to the command sender
                // should only happen if there has not yet been a message sent

                // check data
                fCount.putIfAbsent(player.getUniqueId(), 0);

                // set target
                targetId = player.getUniqueId();
                targetName = player.getName();
            }

            // increment target's fCount
            fCount.put(targetId, fCount.get(targetId) + 1);

            // set cooldown
            cooldownManager.setCooldown(player.getUniqueId(), System.currentTimeMillis());

            // if player clicked the F in chat
            // hover message will run "/pressf <target> false
            if (args.length > 1) {
                if (args[1].equals("false")) {
                    // send message just to player
                    // should only trigger from clicking a global message
                    if (targetName.equals(player.getName())) { targetName = "yourself"; }
                    Component pressedF = mmsg.deserialize("<prefix> <mc>You pressed <f_key> <mc>to pay respects to <ac><target><mc>.",
                            Placeholder.parsed("target", targetName));
                    player.sendMessage(pressedF);
                }
            } else {
                // send global server message of the pressed F
                String actualTargetName = targetName;
                if (targetName.equals(player.getName())) { targetName = "themself"; }
                Component pressedF = mmsg.deserialize("<hover:show_text:'<mc>Click to press <f_key> for <ac><a_target><mc>!'>" +
                                "<click:run_command:/pressf <a_target> false>" +
                                "<prefix> <ac><player> <mc>pressed <f_key> <mc>to pay respects to <ac><target><mc>.</click></hover>",
                        Placeholder.parsed("player", player.getName()),
                        Placeholder.parsed("target", targetName),
                        Placeholder.parsed("a_target", actualTargetName));
                this.getServer().sendMessage(pressedF);
            }
            return true;
        } else if (command.getName().equals("pressf") && !(sender instanceof Player)) { // CONSOLE
            getLogger().info("You cannot press F from the console. F.");
            return true;
        }

        //
        // VIEWF
        //
        if (command.getName().equals("viewf") && sender instanceof Player) { // PLAYER
            Player player = (Player) sender;

            // target assignment (who is receiving the F)
            UUID targetId;
            String targetName;
            Component subject; // have correct grammar for message
            if (args.length != 0) {
                // if argument is given, find the player based on passed string username

                // check for data
                if (noData(args[0])) {
                    player.sendMessage(invalidTarget);
                    return true;
                }

                // set target
                targetId = Bukkit.getOfflinePlayer(args[0]).getUniqueId();
                targetName = Bukkit.getOfflinePlayer(args[0]).getName();

                subject = mmsg.deserialize("<mc>" + targetName + " has");
            } else {
                // if not set target to the command sender
                // should only happen if there has not yet been a message sent
                targetId = player.getUniqueId();
                subject = mmsg.deserialize("<mc>You have");

                // if player fCount is null, put 0
                fCount.putIfAbsent(player.getUniqueId(), 0); // no need to check data here since player is always online
            }

            // send message to player
            Component viewF = mmsg.deserialize("<prefix> <subject> <mc>received <ac><count> <f_key><mc>s.",
                    Placeholder.component("subject", subject),
                    Placeholder.parsed("count", String.valueOf(fCount.get(targetId))));
            player.sendMessage(viewF);

            return true;
        } else if (command.getName().equals("viewf") && !(sender instanceof Player)) { // CONSOLE
            if (args.length == 0) { getLogger().info("You are the console, you don't exist. You can't have an F."); }
            else {
                // check for data
                if (noData(args[0])) {
                    getLogger().info("Error: Invalid target. Please provide the name of a valid player.");
                    return true;
                }

                // target assignment (who is receiving the F)
                UUID targetId = Bukkit.getOfflinePlayer(args[0]).getUniqueId();
                
                // send message to console
                getLogger().info(Bukkit.getOfflinePlayer(args[0]).getName() + " has received " + fCount.get(targetId) + " Fs.");
            }
            return true;
        }

        //
        // PFTOP | F LEADERBOARD
        //
        if (command.getName().equals("pressftop")) {
            Map<UUID, Integer> leaderboard =
                    fCount.entrySet().stream()
                            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

            final double entriesPerPage = configLoader.getLBPageEntries();
            int page = 0;
            final int maxPages = (int) Math.ceil(leaderboard.size() / entriesPerPage);

            if(args.length > 0) { // determine correct page to view based on argument, and make sure argument is actually a number
                int i = Integer.parseInt(args[0]);
                if (i > 0 && i <= maxPages) {
                    page = i - 1;
                }
            }

            StringBuilder output = new StringBuilder();
            List<UUID> indexList = new ArrayList<>(leaderboard.keySet()); // only for getting indices
            int startPos = page * (int) entriesPerPage; // number of the first entry on the selected leaderboard page
            int limit = startPos + (int) entriesPerPage;
            if(leaderboard.size() < limit) { limit = leaderboard.size(); }

            for(int i = startPos; i < limit; i++) {
                // Get Key and Value
                UUID k = indexList.get(i);
                Integer v = leaderboard.get(k);

                // Construct entries for the appropriate page
                output.append("<ac2>").append(i + 1).append(". <mc>")
                    .append(getServer().getOfflinePlayer(k).getName())
                    .append(": <ac>").append(v);
                if(i < limit - 1) {
                    output.append("\n");
                }
            }

            StringBuilder lbMessageBuilder = new StringBuilder();
            if (!(sender instanceof Player)) { lbMessageBuilder.append("\n"); }
            if(lbHeader != null && configLoader.getLBHeaderEnabled()) {
                lbMessageBuilder.append("<header>");
            }
            lbMessageBuilder.append("\n<list>");
            if(configLoader.getLBNextPgMsgEnabled() && (page + 2) <= maxPages) {
                lbMessageBuilder.append("\n<npm>");
            }

            Component pageComponent = mmsg.deserialize(String.valueOf(output));

            Component nextPageMsg = mmsg.deserialize("<mc>Type <ac>/pftop <next> <mc>to see the next page.",
                    Placeholder.parsed("next", String.valueOf(page + 2)));

            Component lbMessage = mmsg.deserialize(
                    String.valueOf(lbMessageBuilder), Placeholder.component("list", pageComponent), Placeholder.component("npm", nextPageMsg));

            sender.sendMessage(lbMessage);
            return true;
        }

        //
        // PFADMIN
        //
        if (command.getName().equals("pfadmin") && sender.hasPermission("pressf.admin")) {
            Component usage = mmsg.deserialize("<mc>Usage: /pfadmin <ac><reload | load | save>");
            Component reloadingMsg = mmsg.deserialize("<mc>Reloaded config file.");
            Component saved = mmsg.deserialize("<mc>Saved data to file.");
            Component loaded = mmsg.deserialize("<mc>Loaded data from file.");

            if (args.length == 0) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    player.sendMessage(mmsg.deserialize("<prefix> <usage>", Placeholder.component("usage", usage)));
                } else {
                    getLogger().info(PlainTextComponentSerializer.plainText().serialize(usage));
                }
                return true;
            } else {
                switch (args[0]) {
                    case "reload" -> {
                        configLoader.reloadConfig();
                        getComponents();

                        // send message reloading is complete.
                        if (sender instanceof Player) {
                            Player player = (Player) sender;
                            player.sendMessage(mmsg.deserialize("<prefix> <reload>", Placeholder.component("reload", reloadingMsg)));
                        } else {
                            getLogger().info(PlainTextComponentSerializer.plainText().serialize(reloadingMsg));
                        }
                        return true;
                    }
                    case "load" -> {
                        data.load(fCount);

                        // send message loading data is complete
                        if (sender instanceof Player) {
                            Player player = (Player) sender;
                            player.sendMessage(mmsg.deserialize("<prefix> <loaded>", Placeholder.component("loaded", loaded)));
                        } else {
                            getLogger().info(PlainTextComponentSerializer.plainText().serialize(loaded));
                        }
                        return true;
                    }
                    case "save" -> {
                        data.save(fCount);

                        // send message saving data is complete
                        if (sender instanceof Player) {
                            Player player = (Player) sender;
                            player.sendMessage(mmsg.deserialize("<prefix> <saved>", Placeholder.component("saved", saved)));
                        } else {
                            getLogger().info(PlainTextComponentSerializer.plainText().serialize(saved));
                        }
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
