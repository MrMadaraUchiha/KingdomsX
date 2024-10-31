package org.kingdoms.outposts;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.kingdoms.config.KingdomsConfig;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.player.KingdomPlayer;
import org.kingdoms.managers.pvp.PvPManager;
import org.kingdoms.utils.MathUtils;
import org.kingdoms.utils.internal.numbers.Numbers;

public final class OutpostManager implements Listener {
    public OutpostManager() {
        PvPManager.registerHandler(new OutpostPvPHandler());
    }

    private static final class OutpostPvPHandler implements PvPManager.PvPHandler {
        @Override
        public Boolean canFight(@NonNull Player victim, @NonNull Player damager) {
            OutpostEvent victimEvent = OutpostEvent.getJoinedEvent(victim.getUniqueId());
            OutpostEvent damagerEvent = OutpostEvent.getJoinedEvent(damager.getUniqueId());

            if (victimEvent != damagerEvent) return null;
            return true;
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Kingdom kingdom = KingdomPlayer.getKingdomPlayer(player).getKingdom();
        if (kingdom == null) return;

        OutpostEvent opEvent = OutpostEvent.getJoinedEvent(kingdom);
        if (opEvent != null) opEvent.display(player);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (OutpostEvent.getEvents().isEmpty()) return;

        Player player = event.getEntity();
        KingdomPlayer kp = KingdomPlayer.getKingdomPlayer(player);
        Kingdom kingdom = kp.getKingdom();

        if (kingdom == null) return;
        if (!OutpostEvent.getKingdomsInEvents().containsKey(kingdom.getId())) return;

        String str = KingdomsConfig.OUTPOST_EVENTS_DEATH_RESOURCE_POINTS_PENALTY.getString();
        if (str == null || str.equals("0")) return;
        long rp = (long) MathUtils.eval(str, kingdom);
        if (rp <= 0) return;

        kingdom.getResourcePoints().add(-rp);
        OutpostsLang.OUTPOST_EVENTS_DEATH.sendMessage(player, "rp", Numbers.toFancyNumber(rp));
    }
}
