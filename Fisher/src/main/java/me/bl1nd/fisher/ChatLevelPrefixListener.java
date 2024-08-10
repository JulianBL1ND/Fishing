package me.bl1nd.fisher;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class ChatLevelPrefixListener implements Listener {

    private final PlayerXPManager playerXPManager;

    public ChatLevelPrefixListener(PlayerXPManager playerXPManager) {
        this.playerXPManager = playerXPManager;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        int level = playerXPManager.getPlayerLevel(player);
        String levelPrefix = ChatColor.GREEN + "[" + level + "✦]" + ChatColor.RESET; // Format the level prefix

        // Modify the format of the chat message
        event.setFormat(levelPrefix + " " + player.getName() + ": " + event.getMessage());
    }
}
