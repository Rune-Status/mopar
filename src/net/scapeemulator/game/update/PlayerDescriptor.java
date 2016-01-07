package net.scapeemulator.game.update;

import java.util.HashMap;
import java.util.Map;

import net.scapeemulator.game.model.mob.Direction;
import net.scapeemulator.game.model.player.Player;
import net.scapeemulator.game.msg.impl.PlayerUpdateMessage;
import net.scapeemulator.game.net.game.DataOrder;
import net.scapeemulator.game.net.game.DataType;
import net.scapeemulator.game.net.game.GameFrameBuilder;

public abstract class PlayerDescriptor {

    public static PlayerDescriptor create(Player player, int[] tickets) {
        Direction firstDirection = player.getFirstDirection();
        Direction secondDirection = player.getSecondDirection();

        if (firstDirection == Direction.NONE)
            return new IdlePlayerDescriptor(player, tickets);
        else if (secondDirection == Direction.NONE)
            return new WalkPlayerDescriptor(player, tickets);
        else
            return new RunPlayerDescriptor(player, tickets);
    }

    private final Map<Class<? extends PlayerBlock>, PlayerBlock> blocks = new HashMap<>();

    public PlayerDescriptor(Player player, int[] tickets) {
        this(player, tickets, false);
    }

    public PlayerDescriptor(Player player, int[] tickets, boolean force) {
        if (player.isActive()) {
            /*
             * This active check is required for the RemovePlayerDescriptor. The
             * player id would be -1 in this case, which causes the following
             * code to crash. Skipping this code doesn't matter as no update
             * blocks can be sent when removing a player.
             */
            int id = player.getId() - 1;
            int ticket = player.getAppearanceTicket();
            if (tickets[id] != ticket || force) {
                tickets[id] = ticket;
                addBlock(new AppearancePlayerBlock(player));
            }
        }

        if (player.isChatUpdated())
            addBlock(new ChatPlayerBlock(player));

        if (player.isHitOneUpdated())
            addBlock(new HitOnePlayerBlock(player));

        if (player.isHitTwoUpdated())
            addBlock(new HitTwoPlayerBlock(player));

        if (player.isChatForced())
            addBlock(new ForceChatPlayerBlock(player));

        if (player.isAnimationUpdated())
            addBlock(new AnimationPlayerBlock(player));

        if (player.isSpotAnimationUpdated())
            addBlock(new SpotAnimationPlayerBlock(player));

        if (player.isTurnToPositionUpdated())
            addBlock(new TurnToPositionPlayerBlock(player));

        if (player.isTurnToTargetUpdated())
            addBlock(new TurnToMobPlayerBlock(player));

        if (player.isForceMovementUpdated())
            addBlock(new ForceMovementPlayerBlock(player));

    }

    private void addBlock(PlayerBlock block) {
        blocks.put(block.getClass(), block);
    }

    public boolean isBlockUpdatedRequired() {
        return !blocks.isEmpty();
    }

    public void encode(PlayerUpdateMessage message, GameFrameBuilder builder, GameFrameBuilder blockBuilder) {
        encodeDescriptor(message, builder, blockBuilder);
        if (isBlockUpdatedRequired()) {
            int flags = 0;
            for (PlayerBlock block : blocks.values())
                flags |= block.getFlag();
            if (flags > 0xFF) {
                flags |= 0x10;
                blockBuilder.put(DataType.SHORT, DataOrder.LITTLE, flags);
            } else {
                blockBuilder.put(DataType.BYTE, flags);
            }
            encodeBlock(message, blockBuilder, ChatPlayerBlock.class);
            encodeBlock(message, blockBuilder, HitOnePlayerBlock.class);
            encodeBlock(message, blockBuilder, AnimationPlayerBlock.class);
            encodeBlock(message, blockBuilder, AppearancePlayerBlock.class);
            encodeBlock(message, blockBuilder, TurnToMobPlayerBlock.class);
            encodeBlock(message, blockBuilder, ForceMovementPlayerBlock.class);
            encodeBlock(message, blockBuilder, ForceChatPlayerBlock.class);
            encodeBlock(message, blockBuilder, HitTwoPlayerBlock.class);
            // encodeBlock(message, blockBuilder, ???.class); //0x800
            encodeBlock(message, blockBuilder, SpotAnimationPlayerBlock.class);
            encodeBlock(message, blockBuilder, TurnToPositionPlayerBlock.class);

        }
    }

    private void encodeBlock(PlayerUpdateMessage message, GameFrameBuilder builder, Class<? extends PlayerBlock> type) {
        PlayerBlock block = blocks.get(type);
        if (block != null) {
            block.encode(message, builder);
        }
    }

    public abstract void encodeDescriptor(PlayerUpdateMessage message, GameFrameBuilder builder, GameFrameBuilder blockBuilder);

}
