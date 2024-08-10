package me.bl1nd.fisher;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerXPManager {

    private final Map<UUID, Integer> playerXPMap = new HashMap<>();
    private final Map<UUID, Integer> playerLevelMap = new HashMap<>();
    private static final int XP_PER_LEVEL = 1000;

    public void addXP(Player player, int xp) {
        UUID playerId = player.getUniqueId();
        int currentXP = playerXPMap.getOrDefault(playerId, 0);
        int newXP = currentXP + xp;
        int level = playerLevelMap.getOrDefault(playerId, 1);  // Start at level 1

        // Debugging output
        player.sendMessage("Current XP: " + currentXP);
        player.sendMessage("XP to add: " + xp);
        player.sendMessage("New XP: " + newXP);
        player.sendMessage("Current Level: " + level);

        // Check for level up
        if (newXP >= XP_PER_LEVEL) {
            level++;
            newXP -= XP_PER_LEVEL;  // Carry over remaining XP to the next level
            playerLevelMap.put(playerId, level);
            player.sendMessage("Congratulations! You've leveled up to level " + level + "!");
        }

        // Update the player's XP and level
        playerXPMap.put(playerId, newXP);
        updatePlayerXPBar(player);  // Update the XP bar with the new progress

        // Debugging output
        player.sendMessage("XP after update: " + newXP);
        player.sendMessage("Level after update: " + level);
    }

    public int getPlayerLevel(Player player) {
        return playerLevelMap.getOrDefault(player.getUniqueId(), 1);  // Start at level 1
    }

    public int getPlayerXP(Player player) {
        return playerXPMap.getOrDefault(player.getUniqueId(), 0);
    }

    public int getXPToNextLevel(Player player) {
        return XP_PER_LEVEL - getPlayerXP(player);
    }

    private void updatePlayerXPBar(Player player) {
        int currentXP = getPlayerXP(player);
        float xpProgress = (float) currentXP / XP_PER_LEVEL;  // Calculate progress towards the next level
        int level = getPlayerLevel(player);

        player.setLevel(level);  // Set the player's level above the XP bar
        player.setExp(xpProgress);  // Set the progress on the XP bar
    }

    public void resetXP(Player player) {
        UUID playerId = player.getUniqueId();
        playerXPMap.remove(playerId);
        playerLevelMap.remove(playerId);
        player.setLevel(1);  // Reset to level 1
        player.setExp(0);
    }
}
