package me.ryzenfromfire.pressf;

import com.comphenix.protocol.*;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class Events implements Listener {

    private final PressF plugin;
    private Player lastMessenger, lastDeath;
    private long lastMessageTime = 0, lastDeathTime = 0;
    private boolean replaceF;

    public Events(PressF plugin) {
        this.plugin = plugin;
        replaceF = plugin.getConfigLoader().doReplaceF();
        if (plugin.protocolLibHook) {
            ProtocolManager protocolManager = plugin.getProtocolManager();
            if (protocolManager == null) {
                plugin.getLogger().severe("ERROR: ProtocolLib Hook failed (null).");
            } else {
                protocolManager.addPacketListener(new PacketAdapter((Plugin) this,
                        ListenerPriority.NORMAL,
                        PacketType.Play.Client.CHAT) {
                    @Override
                    public void onPacketReceiving(PacketEvent event) {
                        if (event.getPacketType() == PacketType.Play.Client.CHAT) {
                            PacketContainer packet = event.getPacket();
                            String message = packet.getStrings().read(0);

                            if (message.equalsIgnoreCase("F")) {
                                event.setCancelled(true);
                                Bukkit.getScheduler().runTask(this.plugin, () -> Bukkit.dispatchCommand(event.getPlayer(), "pressf"));
                            } else {
                                lastMessenger = event.getPlayer();
                                lastMessageTime = System.currentTimeMillis();
                            }
                        }
                    }
                });
            }
        } //end PL Hook
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
}