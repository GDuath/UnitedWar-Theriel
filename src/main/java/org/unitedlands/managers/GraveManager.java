package org.unitedlands.managers;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.ImmutablePair;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import de.jeff_media.angelchest.events.AngelChestSpawnPrepareEvent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.WarSide;
import org.unitedlands.listeners.PlayerDeathListener;
import org.unitedlands.models.War;
import org.unitedlands.util.WarLivesMetadata;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class GraveManager implements Listener {

    private final UnitedWar plugin;

    public GraveManager(UnitedWar plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAngelChestSpawnPrepare(AngelChestSpawnPrepareEvent event) {
        if (!plugin.getConfig().getBoolean("enable-war-grave-control", true)) return;

        Player victim = event.getPlayer();
        if (victim == null) return;

        Map<War, WarSide> victimWars = plugin.getWarManager().getActivePlayerWars(victim.getUniqueId());
        if (victimWars == null || victimWars.isEmpty()) return;

        var damageEvent = victim.getLastDamageCause();
        if (!(damageEvent instanceof EntityDamageByEntityEvent damageByEntity)) return;

        Player killer = PlayerDeathListener.findKillingPlayer(victim, damageByEntity);
        if (killer == null) {
            plugin.getLogger().info("[GraveManager] " + victim.getName() + " died during war, but no killer found. Creating grave.");
            return;
        }

        Map<War, WarSide> killerWars = plugin.getWarManager().getActivePlayerWars(killer.getUniqueId());

        boolean punishThirdParty = plugin.getConfig().getBoolean("punish-third-party-interference", true);
        boolean victimInWar = !victimWars.isEmpty();
        boolean killerInWar = killerWars != null && !killerWars.isEmpty();

        // If a third party kills a war participant, force grave creation.
        if (punishThirdParty && victimInWar && !killerInWar) {
            plugin.getLogger().info(String.format(
                    "[GraveManager] %s (neutral) killed %s (war participant). Creating grave.",
                    killer.getName(), victim.getName()
            ));
            return;
        }

        // If a war participant kills a third party, force items to floor.
        if (punishThirdParty && !victimInWar && killerInWar) {
            Pair<WarSide, String> land = getLandSideAndTownName(victim.getLocation(), null);
            if (land.getLeft() != null) {
                plugin.getLogger().info(String.format(
                        "[GraveManager] %s (neutral) was killed by %s (war participant) in warzone (%s). Dropping items.",
                        victim.getName(), killer.getName(), land.getRight()
                ));
                event.setCancelled(true);
                return;
            }
        }

        for (var entry : victimWars.entrySet()) {
            War war = entry.getKey();
            WarSide victimSide = entry.getValue();

            // Check if victim has war lives. Return default behaviour if no lives.
            Resident resident = TownyAPI.getInstance().getResident(victim);
            if (resident == null) continue;

            int lives = WarLivesMetadata.getWarLivesMetaData(resident, war.getId());
            if (lives <= 0) {
                plugin.getLogger().info(String.format(
                        "[GraveManager] %s has 0 war lives in war '%s'. Skipping grave behaviour.",
                        victim.getName(), war.getTitle()
                ));
                return;
            }

            // Check if killer has war  lives. Return default behaviour if no lives.
            Resident killerResident = TownyAPI.getInstance().getResident(killer);
            if (killerResident == null) continue;

            int killerLives = WarLivesMetadata.getWarLivesMetaData(killerResident, war.getId());
            if (killerLives <= 0) {
                plugin.getLogger().info(String.format(
                        "[GraveManager] %s (killer) has 0 war lives in war '%s'. Skipping grave behaviour.",
                        killer.getName(), war.getTitle()
                ));
                return;
            }

            boolean killerInSameWar = Objects.requireNonNull(killerWars).containsKey(war)
                    || war.getAttacking_mercenaries().contains(killer.getUniqueId())
                    || war.getDefending_mercenaries().contains(killer.getUniqueId());

            if (!killerInSameWar) continue;

            Pair<WarSide, String> land = getLandSideAndTownName(victim.getLocation(), war);
            WarSide landSide = land.getLeft();
            String landTown = land.getRight();

            boolean isWildLand = landSide == null;
            boolean isFriendlyLand = landSide == victimSide;
            boolean isHostileLand = !isWildLand && landSide != victimSide;

            if (isFriendlyLand) {
                if (plugin.getConfig().getBoolean("war-graves.in-friendly-land", true)) {
                    plugin.getLogger().info(String.format(
                            "[GraveManager] %s died in friendly land (%s). Creating grave.",
                            victim.getName(), landTown
                    ));
                } else {
                    plugin.getLogger().info(String.format(
                            "[GraveManager] %s died in friendly land (%s). Dropping items.",
                            victim.getName(), landTown
                    ));
                    event.setCancelled(true);
                }
                return;
            }

            if (isHostileLand) {
                if (!plugin.getConfig().getBoolean("war-graves.in-hostile-land", false)) {
                    plugin.getLogger().info(String.format(
                            "[GraveManager] %s died in hostile land (%s). Dropping items.",
                            victim.getName(), landTown
                    ));
                    event.setCancelled(true);
                    return;
                }
            }

            if (isWildLand) {
                if (!plugin.getConfig().getBoolean("war-graves.in-wild-land", false)) {
                    plugin.getLogger().info(String.format(
                            "[GraveManager] %s died in wild land. Dropping items.",
                            victim.getName()
                    ));
                    event.setCancelled(true);
                    return;
                }
            }

            plugin.getLogger().info(String.format(
                    "[GraveManager] %s (side: %s) was killed by %s (side: %s) in %s land (%s). Creating grave.",
                    victim.getName(), victimSide,
                    killer.getName(), war.getPlayerWarSide(killer.getUniqueId()),
                    isWildLand ? "wild" : landSide.toString().toLowerCase(),
                    landTown
            ));
            return;
        }

        plugin.getLogger().warning(String.format(
                "[GraveManager] Fallback: %s died during war, killed by %s. No valid scenario matched. Creating grave.",
                victim.getName(), killer.getName()
        ));
    }

    private Pair<WarSide, String> getLandSideAndTownName(Location location, War optionalWarContext) {
        TownBlock block = TownyAPI.getInstance().getTownBlock(location);
        if (block == null || !block.hasTown()) return new ImmutablePair<>(null, "wilderness");

        var town = block.getTownOrNull();
        if (town == null) return new ImmutablePair<>(null, "wilderness");

        UUID townId = town.getUUID();

        // If a specific war is passed, use its context to find land ownership side.
        if (optionalWarContext != null) {
            WarSide side = optionalWarContext.getTownWarSide(townId);
            return new ImmutablePair<>(side, town.getName());
        }

        // Otherwise, check all active wars and flag it as war land (no specific side).
        boolean isWarLand = plugin.getWarManager().getActiveWars().stream()
                .anyMatch(war -> war.getAttacking_towns().contains(townId) || war.getDefending_towns().contains(townId));

        return new ImmutablePair<>(isWarLand ? WarSide.NONE : null, town.getName());
    }

}
