package me.ryzenfromfire.pressf.hooks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import me.ryzenfromfire.pressf.Events;
import me.ryzenfromfire.pressf.PressF;
import org.bukkit.Bukkit;

public class ProtocolLibCompat {
    private final PressF plugin;
    private final Events events;

    public ProtocolLibCompat(PressF plugin) {
        this.plugin = plugin;
        this.events = plugin.getEvents();
    }

    public void enableProtocolLibHook() {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        if (protocolManager == null) {
            plugin.getLogger().severe("ERROR: ProtocolLib Hook failed (null).");
        } else {
            protocolManager.addPacketListener(new PacketAdapter(plugin,
                    ListenerPriority.NORMAL,
                    PacketType.Play.Client.CHAT) {
                @Override
                public void onPacketReceiving(PacketEvent event) {
                    if (event.getPacketType() == PacketType.Play.Client.CHAT) {
                        PacketContainer packet = event.getPacket();
                        String message = packet.getStrings().read(0);
                        if (events.doReplaceF() && message.equalsIgnoreCase("F")) {
                            event.setCancelled(true);
                            Bukkit.getScheduler().runTask(this.plugin, () -> Bukkit.dispatchCommand(event.getPlayer(), "pressf !silentFreplace false"));
                        } else {
                            events.setLastMessenger(event.getPlayer());
                            events.setLastMessageTime(System.currentTimeMillis());
                        }
                    }
                }
            });
        }
    }
}
