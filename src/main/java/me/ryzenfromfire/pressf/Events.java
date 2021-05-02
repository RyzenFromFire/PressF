package me.ryzenfromfire.pressf;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class Events implements Listener {

    private Player lastMessenger, lastDeath;
    private long lastMessageTime = 0, lastDeathTime = 0;

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        lastMessenger = event.getPlayer();
        lastMessageTime = System.currentTimeMillis();
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
}