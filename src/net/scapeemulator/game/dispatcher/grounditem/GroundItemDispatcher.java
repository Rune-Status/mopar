package net.scapeemulator.game.dispatcher.grounditem;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.scapeemulator.game.GameServer;
import net.scapeemulator.game.model.Option;
import net.scapeemulator.game.model.Position;
import net.scapeemulator.game.model.World;
import net.scapeemulator.game.model.definition.ItemDefinitions;
import net.scapeemulator.game.model.player.Player;
import net.scapeemulator.game.util.HandlerContext;

/**
 * @author Hadyn Richard
 * @author David Insley
 */
public final class GroundItemDispatcher {

    /**
     * The mapping for all of the handler lists.
     */
    private Map<Option, List<GroundItemHandler>> handlerLists = new HashMap<>();

    /**
     * Constructs a new {@link GroundItemDispatcher};
     */
    public GroundItemDispatcher() {
        for (Option option : Option.values()) {
            if (option.equals(Option.ALL)) {
                continue;
            }
            handlerLists.put(option, new LinkedList<GroundItemHandler>());
        }

        /* Bind each of the handlers */
        bind(new TakeGroundItemHandler());
    }

    /**
     * Binds a handler to this dispatcher.
     * 
     * @param handler The handler to bind.
     */
    public void bind(GroundItemHandler handler) {
        if (handler.getOption().equals(Option.ALL)) {
            for (Entry<Option, List<GroundItemHandler>> entry : handlerLists.entrySet()) {
                entry.getValue().add(handler);
            }
        } else {
            List<GroundItemHandler> list = handlerLists.get(handler.getOption());
            list.add(handler);
        }
    }

    /**
     * Gets the name of the option for an item.
     * 
     * @param id The item id.
     * @param option The option.
     * @return The option name.
     */
    private String getOptionName(int id, Option option) {
        String optionName = ItemDefinitions.forId(id).getGroundOptions()[option.toInteger()];
        return optionName == null ? "null" : optionName.toLowerCase();
    }

    /**
     * Handles the parameters of a ground item message.
     * 
     * @param player The player that the message came from.
     * @param id The item id.
     * @param position The position of the ground item.
     * @param option The option that was selected.
     */
    public void handle(Player player, int itemId, Position position, Option option) {
        if (player.actionsBlocked()) {
            return;
        }
        if (!World.getWorld().getGroundItems().contains(itemId, position, player)) {
            return;
        }
        String optionName = getOptionName(itemId, option);
        List<GroundItemHandler> handlers = handlerLists.get(option);
        if (handlers != null) {
            HandlerContext context = new HandlerContext();
            for (GroundItemHandler handler : handlers) {
                handler.handle(player, itemId, position, optionName, context);
                if (context.doStop()) {
                    break;
                }
            }
        }
    }

    public static GroundItemDispatcher getInstance() {
        return GameServer.getInstance().getMessageDispatcher().getGroundItemDispatcher();
    }
}
