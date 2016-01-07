package net.scapeemulator.game.update;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.scapeemulator.game.model.Position;
import net.scapeemulator.game.model.World;
import net.scapeemulator.game.model.npc.NPC;
import net.scapeemulator.game.model.player.Player;
import net.scapeemulator.game.model.player.skills.prayer.Prayer;
import net.scapeemulator.game.msg.impl.NpcUpdateMessage;
import net.scapeemulator.game.msg.impl.PlayerUpdateMessage;
import net.scapeemulator.game.msg.impl.RegionChangeMessage;
import net.scapeemulator.game.msg.impl.RegionConstructMessage;
import net.scapeemulator.game.msg.impl.ResetMinimapFlagMessage;

public final class PlayerUpdater {

    private final World world;

    public PlayerUpdater(World world) {
        this.world = world;
    }

    public void tick() {
        for (Player player : world.getPlayers()) {
            if (player.getSession() != null) {
                preprocess(player);
            }
        }

        for (NPC npc : world.getNpcs())
            preprocess(npc);

        for (Player player : world.getPlayers()) {
            if (player.getSession() != null) {
                updatePlayers(player);
                updateNpcs(player);
            }
        }

        for (Player player : world.getPlayers())
            if (player.getSession() != null) {
                postprocess(player);
            }

        for (NPC npc : world.getNpcs())
            postprocess(npc);
    }

    private void preprocess(Player player) {
        if (player.getWalkingQueue().isMinimapFlagReset())
            player.send(new ResetMinimapFlagMessage());

        if (player.isTeleporting()) {
            player.setUpdateModelLists(true);
        }

        if (isRegionChangeRequired(player)) {
            Position position = player.getPosition();
            player.setLastKnownRegion(position);
            // TODO find a better way to do this?
            if (player.getConstructedRegion() != null) {
                player.send(new RegionConstructMessage(position, player.getConstructedRegion()));
                player.setClipped(false);
            } else {
                player.send(new RegionChangeMessage(position));
                player.setClipped(true);
            }
        }

        player.getTimers().tick();
        player.getPrayers().tick();
        player.getSkillSet().tick(player.getHealthRegen(), player.getPrayers().prayerActive(Prayer.RESTORE) ? 2 : 1);
        player.getWalkingQueue().tick();
        player.getCombatHandler().tick();
        player.getHits().tick();
    }

    private void preprocess(NPC npc) {

        /* Tick the NPC to update all the...things that need updating */
        npc.tick();
        if (npc.getDefinition().isAttackable()) {
            npc.getSkillSet().tick(npc.getHealthRegen(), 2);
            npc.getCombatHandler().tick();
            npc.getHits().tick();
        }

        npc.getWalkingQueue().tick();
    }

    private void updatePlayers(Player player) {
        Position lastKnownRegion = player.getLastKnownRegion();
        Position position = player.getPosition();
        int[] tickets = player.getAppearanceTickets();

        PlayerDescriptor selfDescriptor;
        if (player.isTeleporting())
            selfDescriptor = new TeleportPlayerDescriptor(player, tickets);
        else
            selfDescriptor = PlayerDescriptor.create(player, tickets);

        List<PlayerDescriptor> descriptors = new ArrayList<>();
        List<Player> localPlayers = player.getLocalPlayers();
        int localPlayerCount = localPlayers.size();

        for (Iterator<Player> it = localPlayers.iterator(); it.hasNext();) {
            Player p = it.next();
            if (!p.isActive() || p.isTeleporting() || !position.isWithinDistance(p.getPosition())
                    || position.getHeight() != p.getPosition().getHeight()) {
                it.remove();
                descriptors.add(new RemovePlayerDescriptor(p, tickets));
            } else {
                descriptors.add(PlayerDescriptor.create(p, tickets));
            }
        }

        for (Player p : world.getPlayers()) {
            if (localPlayers.size() >= 255)
                break;

            if (p != player && position.getHeight() == p.getPosition().getHeight() && position.isWithinDistance(p.getPosition())
                    && !localPlayers.contains(p)) {
                localPlayers.add(p);
                descriptors.add(new AddPlayerDescriptor(p, tickets));
            }
        }

        player.send(new PlayerUpdateMessage(lastKnownRegion, position, localPlayerCount, selfDescriptor, descriptors));
    }

    private void updateNpcs(Player player) {
        Position lastKnownRegion = player.getLastKnownRegion();
        Position position = player.getPosition();

        List<NpcDescriptor> descriptors = new ArrayList<>();
        List<NPC> localNpcs = player.getLocalNpcs();
        int localNpcCount = localNpcs.size();

        for (Iterator<NPC> it = localNpcs.iterator(); it.hasNext();) {
            NPC n = it.next();
            if (!n.isActive() || n.isTeleporting() || n.isHidden() || !position.isWithinDistance(n.getPosition())) {
                it.remove();
                descriptors.add(new RemoveNpcDescriptor(n));
            } else {
                descriptors.add(NpcDescriptor.create(n));
            }
        }

        for (NPC n : world.getNpcs()) {
            if (localNpcs.size() >= 255)
                break;

            if (position.isWithinDistance(n.getPosition()) && !localNpcs.contains(n) && !n.isHidden()) {
                localNpcs.add(n);
                descriptors.add(new AddNpcDescriptor(n));
            }
        }

        player.send(new NpcUpdateMessage(lastKnownRegion, position, localNpcCount, descriptors));
    }

    private void postprocess(Player player) {
        if (player.getUpdateModelLists()) {
            player.refreshGroundItems();
            player.refreshGroundObjects();
        }

        player.reset();
    }

    private void postprocess(NPC npc) {
        npc.reset();
    }

    private boolean isRegionChangeRequired(Player player) {
        if (player.getConstructedRegion() != null) {
            return true;
        }
        Position lastKnownRegion = player.getLastKnownRegion();
        Position position = player.getPosition();

        int deltaX = position.getLocalX(lastKnownRegion.getRegionX());
        int deltaY = position.getLocalY(lastKnownRegion.getRegionY());

        return deltaX < 16 || deltaX >= 88 || deltaY < 16 || deltaY >= 88;
    }

}