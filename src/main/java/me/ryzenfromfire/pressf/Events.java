package me.ryzenfromfire.pressf;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class Events implements Listener {

    private Player lastMessenger;

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        lastMessenger = event.getPlayer();
    }

    public Player getLastMessenger() { return lastMessenger; }

}