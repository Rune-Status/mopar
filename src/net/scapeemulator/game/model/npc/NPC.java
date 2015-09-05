package net.scapeemulator.game.model.npc;

import java.util.List;

import net.scapeemulator.cache.def.NPCDefinition;
import net.scapeemulator.game.model.Position;
import net.scapeemulator.game.model.World;
import net.scapeemulator.game.model.area.Area;
import net.scapeemulator.game.model.definition.NPCDefinitions;
import net.scapeemulator.game.model.mob.Mob;
import net.scapeemulator.game.model.npc.action.NPCDeathAction;
import net.scapeemulator.game.model.npc.drops.DropTable;
import net.scapeemulator.game.model.npc.drops.DropTables;
import net.scapeemulator.game.model.npc.drops.DropTables.TableDefinition;
import net.scapeemulator.game.model.player.Item;
import net.scapeemulator.game.model.player.Player;
import net.scapeemulator.game.model.player.skills.prayer.Bone;

public abstract class NPC extends Mob {

    private Position spawnPosition;
    private int type;
    private int currentHp;
    private int changingType = -1;
    private Area walkingBounds;
    private NPCDefinition definition;

    public NPC(int type) {
        this.type = type;
        init();
    }

    private void init() {
        definition = NPCDefinitions.forId(type);
        combatHandler = new NPCCombatHandler(this);
        size = definition.getSize();
        healToFull();
    }

    public abstract void tick();

    public void setPosition(Position position) {
        if (spawnPosition == null) {
            spawnPosition = position;
        }
        super.setPosition(position);
    }

    public void setWalkingBounds(Area walkingBounds) {
        this.walkingBounds = walkingBounds;
    }

    public Area getWalkingBounds() {
        return walkingBounds;
    }

    public int getType() {
        return type;
    }

    public NPCDefinition getDefinition() {
        return definition;
    }

    public Position getSpawnPosition() {
        return spawnPosition;
    }

    public int getHealthRegen() {
        return 2;
    }

    public void setChangingType(int type) {
        changingType = type;
        this.type = type;
        definition = NPCDefinitions.forId(type);
        size = definition.getSize();
        combatHandler = new NPCCombatHandler(this);
    }

    public int getChangingType() {
        return changingType;
    }

    public boolean isChangingType() {
        return changingType != -1;
    }

    @Override
    public void reset() {
        super.reset();
        changingType = -1;
    }

    protected void reduceHp(int amount) {
        currentHp -= amount;
    }

    public void heal(int amount) {
        currentHp += amount;
        if (currentHp > getMaximumHitpoints()) {
            currentHp = getMaximumHitpoints();
        }
    }

    public void healToFull() {
        currentHp = definition.getBaseHitpoints();
    }

    public void drop(Mob receiver) {
        TableDefinition def = DropTables.getTable(definition);
        if (def == null) {
            // TODO remove this
            World.getWorld().getGroundItems().add(Bone.BONES.getItemId(), 1, position, receiver instanceof Player ? (Player) receiver : null);
            return;
        }
        List<Item> items = def.getTable().getRandomDrops();
        for (Item item : items) {
            World.getWorld().getGroundItems().add(item.getId(), item.getAmount(), position, receiver instanceof Player ? (Player) receiver : null);
        }
    }

    protected void onDeath() {
        reset();
        startAction(new NPCDeathAction(this));
    }

    @Override
    public int getMaximumHitpoints() {
        return definition.getBaseHitpoints();
    }

    @Override
    public int getCurrentHitpoints() {
        return currentHp;
    }

    @Override
    public boolean isRunning() {
        return false;
    }
}
