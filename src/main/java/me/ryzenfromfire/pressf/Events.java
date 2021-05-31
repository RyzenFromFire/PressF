package me.ryzenfromfire.pressf;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class Events implements Listener {

    private final PressF plugin;
    private Player lastMessenger, lastDeath;
    private long lastMessageTime = 0, lastDeathTime = 0;
    private boolean replaceF;

    public Events(PressF plugin) {
        this.plugin = plugin;
        replaceF = plugin.getConfigLoader().doReplaceF();
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if (!(plugin.protocolLibHook)) {
            if (replaceF && PlainComponentSerializer.plain().serialize(event.message()).equalsIgnoreCase("F")) {
                //if enabled and chat message is only an "F", replace
                event.setCancelled(true);
                Bukkit.getScheduler().runTask( this.plugin, () -> Bukkit.dispatchCommand(event.getPlayer(), "pressf"));
            } else if (!event.isCancelled()) { //should prevent recording if the message was cancelled in another way
                //otherwise treat as normal chat message and record player and time
                lastMessenger = event.getPlayer();
                lastMessageTime = System.currentTimeMillis();
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        lastDeath = event.getEntity();
        lastDeathTime = System.currentTimeMillis();
    }

    public Player getLastMessenger() { return lastMessenger; }

    public long getLastMessageTime() { return lastMessageTime; }

    public Player getLastDeath() { return lastDeath; }

    public long getLastDeathTime() { return lastDeathTime; }

    public void setLastMessenger(Player player) { lastMessenger = player; }

    public void setLastMessageTime(long time) { lastMessageTime = time; }
}