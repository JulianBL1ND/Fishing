package me.bl1nd.fisher;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

public class FishingXPListener implements Listener {

    private final PlayerXPManager xpManager;

    public FishingXPListener(PlayerXPManager xpManager) {
        this.xpManager = xpManager;
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            event.setExpToDrop(0); // Cancel the normal XP drop

            // Add custom XP
            xpManager.addXP(event.getPlayer(), 10); // Assuming you want to add 10 XP per fish caught
        }
    }
}
