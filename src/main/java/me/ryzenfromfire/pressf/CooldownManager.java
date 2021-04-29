package me.ryzenfromfire.pressf;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    private final PressF plugin;

    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public CooldownManager (PressF plugin) {
        this.plugin = plugin;
    }

    public void setCooldown(UUID player, Long time) {
        cooldowns.put(player, time);
    }

    public Long getCooldown(UUID player) {
        return cooldowns.get(player);
    }

    public boolean exists(UUID player) {
        return cooldowns.containsKey(player);
    }
}
